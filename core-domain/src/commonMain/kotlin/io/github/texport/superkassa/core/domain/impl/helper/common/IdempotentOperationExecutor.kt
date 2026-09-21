package io.github.texport.superkassa.core.domain.impl.helper.common

import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.common.CounterScopes
import io.github.texport.superkassa.core.domain.api.model.common.CounterKeyFormats
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.kkm.RequireOperationalUseCase
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryStatus
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
import io.github.texport.superkassa.core.string.api.CoreStrings

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

    /**
     * Смена не длиннее суток.
     *
     * Требование к ККМ (пункты 14, 52 и 93): сверх суток касса обязана
     * перестать оформлять кассовые операции, пока смену не закроют.
     * Проверка стоит здесь, потому что здесь проходят все они — чек,
     * возврат, внесение и изъятие. X-отчёт и закрытие смены идут другим
     * путём и остаются доступны: иначе кассиру было бы нечем ни посмотреть
     * смену, ни выйти из запрета.
     *
     * Отсчёт идёт с первого платёжного документа смены, а не с её открытия:
     * так велит требование, и открыть смену могут задолго до первой
     * продажи. Смена без платёжных документов отсчёт ещё не начала.
     */
    private fun requireShiftWithinDay(kkmId: String) {
        val shift = storage.findOpenShift(kkmId) ?: return
        val since = storage.firstPaymentTimeInShift(shift.id) ?: return
        if (clock.now() - since <= DAY_MILLIS) {
            return
        }
        logger.warn("Shift {} is longer than a day: operations are refused until it is closed", shift.shiftNo)
        throw ValidationException(CoreStrings.shiftLongerThanDay(), "SHIFT_LONGER_THAN_DAY")
    }

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
            requireShiftWithinDay(kkmId)
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
                // Для идемпотентности возвращаем результат с существующим documentId
                val emptyResult = OfdCommandResult(
                    status = OfdCommandStatus.OK,
                    responseBin = null,
                    responseJson = null,
                    responseToken = null,
                    responseReqNum = null,
                    resultCode = 0,
                    resultText = null,
                    errorMessage = null,
                    fiscalSign = null,
                    autonomousSign = null
                )
                return@inTransaction buildResult(existing, emptyResult, DeliveryStatus.ONLINE_OK)
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

/** Сутки — предел продолжительности смены по требованиям к ККМ. */
private const val DAY_MILLIS: Long = 24L * 60 * 60 * 1000
