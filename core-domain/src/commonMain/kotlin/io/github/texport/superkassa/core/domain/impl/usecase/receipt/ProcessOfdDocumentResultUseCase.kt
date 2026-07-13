package io.github.texport.superkassa.core.domain.impl.usecase.receipt

import io.github.texport.superkassa.core.domain.api.model.common.CounterKeyFormats
import io.github.texport.superkassa.core.domain.api.model.common.format
import io.github.texport.superkassa.core.domain.api.model.common.CounterScopes
import io.github.texport.superkassa.core.domain.api.model.kkm.CashOperationType
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
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

        if (resultCode == null) {
            // Если resultCode == null, произошел таймаут или обрыв связи — фискализируем автономно
            val autonomousSign = clock.now().toString()
            storage.updateReceiptStatus(
                documentId = documentId,
                fiscalSign = null,
                autonomousSign = autonomousSign,
                ofdStatus = "PENDING",
                deliveredAt = null,
                isAutonomous = true
            )

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

        // Результат получен от ОФД напрямую (онлайн)
        val success = resultCode == 0
        storage.updateReceiptStatus(
            documentId = documentId,
            fiscalSign = ofdResult.fiscalSign,
            autonomousSign = ofdResult.autonomousSign,
            ofdStatus = if (success) "SENT" else "FAILED",
            deliveredAt = if (success) now else null,
            isAutonomous = false
        )

        if (success) {
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
     * Обновляет состояние блокировки ККМ на основе ответа ОФД.
     */
    private fun updateKkmBlockedStateFromOfd(kkm: KkmInfo, ofdResult: OfdCommandResult, now: Long) {
        val code = ofdResult.resultCode ?: return
        if (code == 15 && kkm.state != KkmState.BLOCKED.name) {
            storage.updateKkm(kkm.copy(updatedAt = now, state = KkmState.BLOCKED.name))
        } else if (code == 0 && kkm.state == KkmState.BLOCKED.name) {
            storage.updateKkm(kkm.copy(updatedAt = now, state = KkmState.ACTIVE.name))
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
     * @param amountBills Сумма операции в тиынах (минимальных денежных единицах).
     */
    fun updateCashSumForOperation(
        kkmId: String,
        shiftId: String,
        type: CashOperationType,
        amountBills: Long
    ) {
        if (amountBills == 0L) return
        val delta = when (type) {
            CashOperationType.CASH_IN -> amountBills
            CashOperationType.CASH_OUT -> -amountBills
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
}
