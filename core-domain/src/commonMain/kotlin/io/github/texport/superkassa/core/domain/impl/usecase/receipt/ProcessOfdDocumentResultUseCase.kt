package io.github.texport.superkassa.core.domain.impl.usecase.receipt

import io.github.texport.superkassa.core.domain.api.model.common.CounterKeyFormats
import io.github.texport.superkassa.core.domain.api.model.common.format
import io.github.texport.superkassa.core.domain.api.model.common.CounterScopes
import io.github.texport.superkassa.core.domain.api.model.kkm.CashOperationType
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.api.model.queue.OfflineQueueCommandRequest
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.internal.OfflineQueuePort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.usecase.counter.UpdateCountersUseCase
import io.github.texport.superkassa.core.domain.impl.helper.OfdResponseParser

/**
 * Сценарий обработки результатов ответа ОФД по фискальным документам.
 *
 * Анализирует статус фискализации документов в ОФД. В случае успеха обновляет
 * статус чеков на "SENT", обновляет счетчики продаж/возвратов и инициирует доставку чеков покупателям.
 * При возникновении ошибок или таймаутов переводит ККМ в автономный режим, регистрирует
 * автономные признаки и ставит документы в очередь на повторную отправку.
 *
 * @property storage Порт для доступа к персистентному хранилищу данных.
 * @property queue Порт для работы с офлайн-очередью команд ККМ.
 * @property clock Порт для работы с системным временем.
 * @property updateCountersUseCase Сценарий обновления счетчиков продаж/возвратов ККМ.
 * @property deliverReceipt Сценарий доставки чека покупателю.
 */
class ProcessOfdDocumentResultUseCase(
    private val storage: StoragePort,
    private val queue: OfflineQueuePort,
    private val clock: ClockPort,
    private val updateCountersUseCase: UpdateCountersUseCase,
    private val deliverReceipt: DeliverReceiptUseCase
) {
    /**
     * Выполняет обработку результатов фискализации документа.
     *
     * @param kkm Информация о текущей ККМ.
     * @param documentId Идентификатор обработанного фискального документа.
     * @param kkmId Идентификатор кассового аппарата (ККМ).
     * @param ofdResult Результат выполнения команды фискализации в ОФД.
     * @param commandType Тип отправленной фискальной команды.
     * @param now Текущее системное время в миллисекундах.
     * @param receiptContext Контекст чека (запрос чека и ID смены) для обновления счетчиков.
     */
    fun execute(
        kkm: KkmInfo,
        documentId: String,
        kkmId: String,
        ofdResult: OfdCommandResult,
        commandType: OfdCommandType,
        now: Long,
        receiptContext: Pair<ReceiptRequest, String>?
    ) {
        val resultCode = ofdResult.resultCode
        // Обновление статуса блокировки ККМ на основе кода ошибки ОФД (код 15 означает блокировку)
        updateKkmBlockedStateFromOfd(kkm, ofdResult, now)

        if (noAnswerFromOfd(ofdResult)) {
            // Если произошел таймаут или обрыв связи, или ОФД вернул 254/255 — фискализируем автономно
            val autonomousSign = clock.now().toString()
            storage.updateReceiptStatus(
                documentId = documentId,
                fiscalSign = null,
                autonomousSign = autonomousSign,
                ofdStatus = "PENDING",
                ofdErrorCode = null,
                deliveredAt = null,
                isAutonomous = true
            )

            // Номер автономного документа касса присваивает сама: в разрыве
            // его неоткуда получить, а без номера ОФД не отличит документ,
            // оформленный офлайн, от обычного сетевого.
            val nextOfflineNumber = 1 + (
                storage.loadCounters(kkmId, CounterScopes.GLOBAL, null)[
                    CounterKeyFormats.OFFLINE_TICKET_NUMBER
                ] ?: 0L
                )
            storage.upsertCounter(
                kkmId,
                CounterScopes.GLOBAL,
                null,
                CounterKeyFormats.OFFLINE_TICKET_NUMBER,
                nextOfflineNumber
            )
            storage.updateDocumentNumber(documentId, nextOfflineNumber)

            // Постановка фискального документа в очередь для отложенной отправки при восстановлении связи
            queue.enqueueOffline(
                OfflineQueueCommandRequest(
                    kkmId = kkmId,
                    type = commandType.value,
                    payloadRef = documentId
                )
            )

            // Переводим кассу в автономный (офлайн) режим
            markAutonomousStarted(kkm, now)

            // Обновляем счетчики продаж с пометкой автономного (офлайн) режима
            if (commandType == OfdCommandType.TICKET && receiptContext != null) {
                updateCountersUseCase.execute(
                    kkmId,
                    receiptContext.second,
                    receiptContext.first,
                    isOffline = true
                )
            }
            return
        }

        if (resultCode == null) {
            // Ответа нет, но и связи не теряли: узел не смог отправить команду
            // (нечем построить запрос, негодная конфигурация ОФД). Повтор
            // с теми же данными провалится так же, поэтому автономный признак
            // здесь не выдаётся и в очередь документ не ставится: он навсегда
            // остался бы «ожидает отправки» и молча копил бы попытки.
            storage.updateReceiptStatus(
                documentId = documentId,
                fiscalSign = null,
                autonomousSign = null,
                ofdStatus = REJECTED,
                ofdErrorCode = null,
                deliveredAt = null,
                isAutonomous = false
            )
            return
        }

        // Результат получен от ОФД напрямую (онлайн)
        // Связь была, ответ получен: документ либо принят, либо отвергнут.
        // Третьего состояния нет. Раньше отказ с кодом вне списка 13/14/17
        // оставлял документ в PENDING: в очередь он не попадал, повтор его
        // не подхватывал, и чек висел «ожидает отправки» бессрочно.
        val success = resultCode == 0
        val status = if (success) DELIVERED else REJECTED

        storage.updateReceiptStatus(
            documentId = documentId,
            fiscalSign = ofdResult.fiscalSign,
            autonomousSign = ofdResult.autonomousSign,
            ofdStatus = status,
            // Код отказа сохраняется при любом ненулевом результате: раньше
            // причина отказа терялась везде, кроме трёх известных кодов.
            ofdErrorCode = if (success) null else resultCode,
            deliveredAt = if (success) now else null,
            isAutonomous = false
        )

        if (success) {
            val extractedDocNo = OfdResponseParser.extractDocNumber(ofdResult.responseJson)
            if (extractedDocNo != null) {
                storage.updateDocumentNumber(documentId, extractedDocNo)
            }

            val ticketAds = OfdResponseParser.extractTicketAds(ofdResult.responseJson)
            if (ticketAds.isNotEmpty()) {
                val freshKkm = storage.findKkmForUpdate(kkmId)
                if (freshKkm != null) {
                    storage.updateKkm(
                        freshKkm.copy(
                            updatedAt = now,
                            branding = freshKkm.branding.copy(ofdTicketAds = ticketAds)
                        )
                    )
                }
            }
        }

        if (!success) return

        // Если отправка успешна, проверяем возможность выхода из автономного режима
        clearAutonomousIfReady(kkm, now)

        // Для чеков продаж/возвратов обновляем счетчики и доставляем чек
        if (commandType != OfdCommandType.TICKET || receiptContext == null) return

        updateCountersUseCase.execute(
            kkmId,
            receiptContext.second,
            receiptContext.first,
            isOffline = false
        )

        val (receipt, _) = receiptContext
        val doc = storage.findFiscalDocumentById(documentId) ?: return

        deliverReceipt.execute(
            kkmId = kkmId,
            documentId = documentId,
            receipt = receipt,
            docSnapshot = doc,
            receiptUrl = ofdResult.receiptUrl,
            responseBin = ofdResult.responseBin
        )
    }

    /**
     * Остался ли документ без ответа ОФД по причине связи.
     *
     * Только такой исход даёт право фискализировать автономно и досылать
     * из очереди. Отказ узла отправить команду связью не является.
     */
    private fun noAnswerFromOfd(result: OfdCommandResult): Boolean =
        result.status == OfdCommandStatus.TIMEOUT ||
            result.resultCode == SERVICE_TEMPORARILY_UNAVAILABLE ||
            result.resultCode == UNKNOWN_ERROR

    /**
     * Обновляет состояние блокировки ККМ на основе ответа ОФД.
     */
    private fun updateKkmBlockedStateFromOfd(kkm: KkmInfo, ofdResult: OfdCommandResult, now: Long) {
        val code = ofdResult.resultCode ?: return
        // 18 и 19 добавлены протоколом 2.0.4: касса снята с учёта и касса
        // отключена от ОФД. Обе означают, что фискализировать больше нечего,
        // и без них снятая с учёта касса продолжала бы выпускать чеки.
        val shouldBlock = code in 1..7 || code == 11 || code == 12 || code == 15 ||
            code == DEREGISTERED_CODE || code == DISCONNECTED_CODE
        if (shouldBlock && kkm.state != KkmState.BLOCKED.name) {
            storage.updateKkm(
                kkm.copy(
                    updatedAt = now,
                    state = KkmState.BLOCKED.name,
                    blockReasonCode = code + 1000
                )
            )
        } else if (code == 0 && kkm.state == KkmState.BLOCKED.name && (kkm.blockReasonCode ?: 0) >= 1000) {
            storage.updateKkm(
                kkm.copy(
                    updatedAt = now,
                    state = KkmState.ACTIVE.name,
                    blockReasonCode = null
                )
            )
        }
    }

    /**
     * Помечает ККМ как работающую в автономном режиме.
     */
    private fun markAutonomousStarted(kkm: KkmInfo, now: Long) {
        if (kkm.autonomousSince != null) return
        storage.updateKkm(kkm.copy(updatedAt = now, autonomousSince = now))
    }

    /**
     * Сбрасывает автономный режим ККМ, если все документы отправлены и связь стабильна.
     */
    private fun clearAutonomousIfReady(kkm: KkmInfo, now: Long) {
        if (kkm.autonomousSince == null && kkm.state != KkmState.BLOCKED.name) return
        if (!queue.canSendDirectly(kkm.id)) return
        val nextState = if (kkm.state == KkmState.BLOCKED.name) KkmState.ACTIVE.name else kkm.state
        storage.updateKkm(kkm.copy(updatedAt = now, autonomousSince = null, state = nextState))
    }

    /**
     * Обновляет общую сумму наличных в кассе (в пределах смены и глобально) при внесении или изъятии.
     *
     * @param kkmId Идентификатор кассового аппарата (ККМ).
     * @param shiftId Идентификатор текущей открытой смены.
     * @param type Тип кассовой операции (внесение или изъятие).
     * @param amountTiyn Сумма операции в тиынах (минимальных денежных единицах).
     */
    fun updateCashSumForOperation(
        kkmId: String,
        shiftId: String,
        type: CashOperationType,
        amountTiyn: Long
    ) {
        if (amountTiyn == 0L) return
        val delta = when (type) {
            CashOperationType.CASH_IN -> amountTiyn
            CashOperationType.CASH_OUT -> -amountTiyn
        }
        fun update(scope: String, scopeShiftId: String?) {
            val current = storage.loadCounters(kkmId, scope, scopeShiftId)[CounterKeyFormats.CASH_SUM] ?: 0L
            val next = current + delta
            storage.upsertCounter(kkmId, scope, scopeShiftId, CounterKeyFormats.CASH_SUM, next)
        }
        update(CounterScopes.SHIFT, shiftId)
        update(CounterScopes.GLOBAL, null)
    }

    /**
     * Обновляет счетчики количества и сумм операций внесения/изъятия на основе фискального документа.
     *
     * @param documentId Идентификатор фискального документа операции с наличными.
     * @param isOffline Признак выполнения операции в автономном (офлайн) режиме.
     */
    fun updateMoneyPlacementCountersFromDocument(documentId: String, isOffline: Boolean) {
        val snapshot = storage.findFiscalDocumentById(documentId) ?: return
        val shiftId = snapshot.shiftId
        if (shiftId.isBlank()) return
        val kkmId = snapshot.cashboxId
        val amount = snapshot.totalAmount ?: 0L
        if (amount == 0L) return

        val opKey = when (snapshot.docType) {
            CashOperationType.CASH_IN.name -> "MONEY_PLACEMENT_DEPOSIT"
            CashOperationType.CASH_OUT.name -> "MONEY_PLACEMENT_WITHDRAWAL"
            else -> return
        }

        fun increment(scope: String, scopeShiftId: String?, key: String, delta: Long) {
            val current = storage.loadCounters(kkmId, scope, scopeShiftId)[key] ?: 0L
            storage.upsertCounter(kkmId, scope, scopeShiftId, key, current + delta)
        }

        fun updateScope(scope: String, scopeShiftId: String?) {
            increment(scope, scopeShiftId, CounterKeyFormats.MONEY_PLACEMENT_TOTAL_COUNT.format(opKey), 1L)
            increment(scope, scopeShiftId, CounterKeyFormats.MONEY_PLACEMENT_COUNT.format(opKey), 1L)
            increment(scope, scopeShiftId, CounterKeyFormats.MONEY_PLACEMENT_SUM.format(opKey), amount)
            if (isOffline) {
                increment(scope, scopeShiftId, CounterKeyFormats.MONEY_PLACEMENT_OFFLINE_COUNT.format(opKey), 1L)
            }
        }

        updateScope(CounterScopes.SHIFT, shiftId)
        updateScope(CounterScopes.GLOBAL, null)
    }

    private companion object {
        /** Документ принят ОФД. */
        const val DELIVERED = "SENT"

        /** Документ отвергнут: фискальным он не стал. */
        const val REJECTED = "FAILED"

        /** Сервис временно недоступен: отправку следует повторить. */
        const val SERVICE_TEMPORARILY_UNAVAILABLE = 254

        /** Неизвестная ошибка: отправку следует повторить. */
        const val UNKNOWN_ERROR = 255

        /** Касса снята с учёта в налоговом органе (протокол 2.0.4). */
        const val DEREGISTERED_CODE = 18

        /** Касса отключена от ОФД (протокол 2.0.4). */
        const val DISCONNECTED_CODE = 19
    }
}
