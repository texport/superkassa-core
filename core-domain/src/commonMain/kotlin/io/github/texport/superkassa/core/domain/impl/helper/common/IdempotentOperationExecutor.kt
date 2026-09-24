package io.github.texport.superkassa.core.domain.impl.helper.common

import io.github.texport.superkassa.core.domain.api.model.common.CounterScopes
import io.github.texport.superkassa.core.domain.api.model.common.CounterKeyFormats
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.kkm.RequireOperationalUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.shift.ShiftDayLimit
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryStatus
import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.internal.IdGeneratorPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.api.port.integration.inTransaction
import io.github.texport.superkassa.core.domain.impl.logging.getLogger

/**
 * Исполнитель идемпотентных фискальных операций.
 *
 * Предоставляет общую логику для безопасного выполнения операций (например, выбивание чека,
 * внесение/изъятие наличных) с защитой от повторных запросов с тем же ключом идемпотентности.
 *
 * @property storage Порт для доступа к хранилищу данных и транзакционному контексту.
 * @property idGenerator Генератор уникальных идентификаторов документов.
 * @property clock Порт для работы с системным временем.
 * @property authorizeUserUseCase Сценарий для проверки прав доступа пользователей.
 * @property requireOperationalUseCase Сценарий для проверки состояния ККМ перед операциями.
 */
class IdempotentOperationExecutor(
    private val storage: StoragePort,
    private val idGenerator: IdGeneratorPort,
    private val clock: ClockPort,
    private val authorizeUserUseCase: AuthorizeUserUseCase,
    private val requireOperationalUseCase: RequireOperationalUseCase
) {
    private val logger = getLogger(IdempotentOperationExecutor::class)

    /** Предел смены в сутки: те же правило и часы, что у экрана и автозакрытия. */
    private val dayLimit = ShiftDayLimit(storage, clock)

    /**
     * Выполняет фискальную операцию с контролем идемпотентности в единой транзакции базы данных.
     *
     * Процесс выполнения:
     * 1. Проверяет работоспособность кассы (должна быть не в режиме программирования).
     * 2. Авторизует роль пользователя (администратор или кассир).
     * 3. Проверяет наличие сохраненного ответа по [idempotencyKey]. Если он найден, повторно
     *    возвращает ранее сгенерированный результат, не отправляя повторных запросов в ОФД.
     * 4. Если ключ новый, регистрирует его, проверяет статус смены, генерирует идентификатор документа,
     *    сохраняет операцию, отправляет команду в ОФД, обрабатывает результат и обновляет статус доставки.
     *
     * @param T Результирующий тип фискальной операции.
     * @param kkmId Уникальный идентификатор ККМ.
     * @param pin ПИН-код пользователя для авторизации.
     * @param idempotencyKey Уникальный ключ идемпотентности запроса.
     * @param operationType Тип операции (например, "CREATE_RECEIPT", "CASH_IN", "CASH_OUT").
     * @param checkShift Функция проверки статуса смены, возвращающая идентификатор активной смены.
     * @param saveOperation Функция сохранения новой кассовой операции в БД.
     * @param sendOfdCommand Функция непосредственной отправки команды в ОФД.
     * @param processResult Функция обработки результатов команды.
     * @param buildResult Функция преобразования результатов ОФД в целевой тип [T].
     * @param receiptContextProvider Необязательный провайдер контекста чека для дополнительных проверок.
     * @return Результат фискальной операции типа [T].
     */
    fun <T> executeIdempotentFiscalOperation(
        kkmId: String,
        pin: String,
        idempotencyKey: String,
        operationType: String,
        checkShift: () -> String, // Возвращает shiftId
        saveOperation: (String, Long, String) -> Unit, // documentId, now, shiftId
        sendOfdCommand: (KkmInfo, String) -> OfdCommandResult, // kkm, documentId -> result
        processResult: (
            KkmInfo,
            String,
            String,
            OfdCommandResult,
            OfdCommandType,
            Long,
            Pair<ReceiptRequest, String>?
        ) -> Unit,
        buildResult: (String, OfdCommandResult, DeliveryStatus) -> T,
        receiptContextProvider: ((String) -> Pair<ReceiptRequest, String>?)? = null
    ): T {
        return storage.inTransaction {
            val kkm = authorizeUserUseCase.requireKkm(kkmId, forUpdate = true)
            requireOperationalUseCase.execute(kkm)
            dayLimit.requireWithinDay(kkmId)
            authorizeUserUseCase.requireRole(kkm.id, pin, setOf(UserRole.ADMIN, UserRole.CASHIER))

            // Проверка идемпотентности
            val existing = storage.findIdempotencyResponse(kkmId, idempotencyKey)
            if (existing != null) {
                logger.info(
                    "Idempotency hit. kkmId={}, key={}, operation={}",
                    kkmId,
                    idempotencyKey,
                    operationType
                )
                // Повтор отвечает тем, что стало с документом, а не «доставлен» всегда.
                val document = storage.findFiscalDocumentById(existing)
                return@inTransaction buildResult(existing, repeatedAnswer(document), deliveryStatusOf(document))
            }

            storage.insertIdempotency(kkmId, idempotencyKey, operationType)

            // Проверка смены
            val shiftId = checkShift()

            // Сохранение операции
            val documentId = idGenerator.nextId()
            val now = clock.now()
            saveOperation(documentId, now, shiftId)

            // Отправка команды в ОФД
            // Номер печатного документа касса ведёт сама и не прерывает
            // в разрыве связи: по нему восстанавливают нумерацию после сбоя.
            val printedNumber = 1 + (
                storage.loadCounters(kkmId, CounterScopes.GLOBAL, null)[
                    CounterKeyFormats.PRINTED_DOCUMENT_NUMBER
                ] ?: 0L
                )
            storage.upsertCounter(
                kkmId,
                CounterScopes.GLOBAL,
                null,
                CounterKeyFormats.PRINTED_DOCUMENT_NUMBER,
                printedNumber
            )
            storage.updatePrintedDocumentNumber(documentId, printedNumber)

            val ofdResult = sendOfdCommand(kkm, documentId)

            // Определение типа команды из результата
            val commandType = when (operationType) {
                "CREATE_RECEIPT" -> OfdCommandType.TICKET
                "CASH_IN", "CASH_OUT" -> OfdCommandType.MONEY_PLACEMENT
                else -> OfdCommandType.TICKET
            }

            val receiptContext = receiptContextProvider?.invoke(shiftId)

            // Обработка результата
            processResult(kkm, documentId, kkmId, ofdResult, commandType, now, receiptContext)

            // Определяем статус доставки для клиента
            val deliveryStatus = when (ofdResult.status) {
                OfdCommandStatus.OK -> DeliveryStatus.ONLINE_OK
                OfdCommandStatus.TIMEOUT -> DeliveryStatus.OFFLINE_QUEUED
                OfdCommandStatus.FAILED -> DeliveryStatus.ONLINE_ERROR
            }

            // Обновление идемпотентности
            storage.updateIdempotencyResponse(kkmId, idempotencyKey, documentId)

            // Построение результата
            buildResult(documentId, ofdResult, deliveryStatus)
        }
    }
}

/**
 * Ответ БФД, каким он был для сохранённого документа, — для ответа на повтор.
 *
 * Отклонённый документ отвечает своим кодом отказа, и повтор называет
 * причину теми же словами, что и первый ответ. Прежде повтор отдавал текст
 * БФД как есть — на одном языке и во всех полях сразу.
 */
internal fun repeatedAnswer(document: FiscalDocumentSnapshot?): OfdCommandResult = when (document?.ofdStatus) {
    "FAILED" -> OfdCommandResult(OfdCommandStatus.FAILED, resultCode = document.ofdErrorCode)
    else -> OfdCommandResult(OfdCommandStatus.OK)
}

/** Статус доставки сохранённого документа для ответа на повтор; непринятый БФД — «в очереди». */
internal fun deliveryStatusOf(document: FiscalDocumentSnapshot?): DeliveryStatus = when (document?.ofdStatus) {
    "SENT" -> DeliveryStatus.ONLINE_OK
    "FAILED" -> DeliveryStatus.ONLINE_ERROR
    else -> DeliveryStatus.OFFLINE_QUEUED
}
