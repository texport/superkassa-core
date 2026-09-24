package io.github.texport.superkassa.core.domain.impl.usecase.receipt

import io.github.texport.superkassa.core.domain.api.model.common.CounterKeyFormats
import io.github.texport.superkassa.core.domain.api.exception.ConflictException
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.kkm.CashOperationRequest
import io.github.texport.superkassa.core.domain.api.model.kkm.CashOperationResult
import io.github.texport.superkassa.core.domain.api.model.kkm.CashOperationType
import io.github.texport.superkassa.core.domain.api.model.kkm.becameFiscal
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.api.port.internal.OfflineQueuePort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.helper.common.IdempotentOperationExecutor
import io.github.texport.superkassa.core.domain.impl.helper.KkmCommonHelper
import io.github.texport.superkassa.core.domain.impl.helper.ofd.BfdDeliveryFailure
import io.github.texport.superkassa.core.domain.impl.usecase.shift.RecalculateShiftCountersUseCase

/**
 * Сценарий выполнения операции внесения/изъятия наличных денег.
 *
 * Данный класс инкапсулирует бизнес-логику проведения операций с наличными средствами на ККМ,
 * обеспечивая их идемпотентность и синхронизацию с ОФД.
 *
 * @property storage Порт для доступа к персистентному хранилищу данных.
 * @property queue Порт для работы с офлайн-очередью команд ККМ.
 * @property executor Компонент для обеспечения идемпотентности выполнения фискальных операций.
 * @property kkmCommonHelper Общий помощник для выполнения типовых операций с ККМ.
 * @property processOfdDocumentResult Сценарий обработки результатов ответов от ОФД.
 */
class CreateCashOperationUseCase(
    private val storage: StoragePort,
    private val queue: OfflineQueuePort,
    private val executor: IdempotentOperationExecutor,
    private val kkmCommonHelper: KkmCommonHelper,
    private val processOfdDocumentResult: ProcessOfdDocumentResultUseCase,
    private val recalculateShiftCounters: RecalculateShiftCountersUseCase
) {
    /**
     * Выполняет операцию внесения или изъятия наличных средств.
     *
     * @param kkmId Идентификатор кассового аппарата (ККМ).
     * @param request Запрос на выполнение кассовой операции с указанием суммы, PIN-кода и ключа идемпотентности.
     * @param type Тип кассовой операции (внесение или изъятие).
     * @return Результат выполнения кассовой операции с идентификатором документа и статусом отправки.
     * @throws ValidationException если сумма операции отрицательная.
     */
    fun execute(kkmId: String, request: CashOperationRequest, type: CashOperationType): CashOperationResult {
        // Проверка корректности суммы операции (не должна быть отрицательной)
        if (request.amount.signum < 0) {
            throw ValidationException(CoreStrings.cashSumNegative(), "CASH_SUM_NEGATIVE")
        }
        val amountMoney = Money.fromTenge(request.amount)
        // Ноль и доли тиына — не операция: узел отвергал их проверкой запроса,
        // касса в приложении пробивала бы внесение на 0 ₸.
        if (amountMoney.tiyn() <= 0L) {
            throw ValidationException(CoreStrings.cashSumTooSmall(), "CASH_SUM_TOO_SMALL")
        }

        // Оцениваем статус автономной очереди ДО сохранения нового документа
        val hasQueue = !queue.canSendDirectly(kkmId)

        // Смену открывает проверка, а деньги в ящике двигает обработка ответа
        // ОФД: смена одна и та же, и перечитывать её из документа незачем.
        var openShift = ""

        // Выполнение идемпотентной фискальной операции
        return executor.executeIdempotentFiscalOperation(
            kkmId = kkmId,
            pin = request.pin,
            idempotencyKey = request.idempotencyKey,
            operationType = type.name,
            checkShift = {
                // Проверка наличия открытой смены на ККМ
                val shift = storage.findOpenShift(kkmId)
                    ?: throw ConflictException(CoreStrings.shiftNotOpen(), "SHIFT_NOT_OPEN")

                if (type == CashOperationType.CASH_OUT) {
                    // Сохранённый счётчик обновляется только при построении отчёта,
                    // поэтому между отчётами он отстаёт от действительности.
                    // Остаток берётся тем же расчётом, что и X- и Z-отчёт, — иначе
                    // касса откажет в изъятии денег, которые у неё есть.
                    val currentCash = recalculateShiftCounters.execute(kkmId, shift)[CounterKeyFormats.CASH_SUM] ?: 0L
                    // Сравнение в тиынах: остаток ящика хранится в них.
                    if (amountMoney.tiyn() > currentCash) {
                        throw ValidationException(CoreStrings.insufficientCash(), "INSUFFICIENT_CASH")
                    }
                }

                shift.id
            },
            saveOperation = { docId, now, shiftId ->
                openShift = shiftId
                // Сохранение операции с наличными в базе данных ККМ
                storage.saveCashOperation(
                    kkmId = kkmId,
                    type = type.name,
                    amount = amountMoney,
                    documentId = docId,
                    shiftId = shiftId,
                    createdAt = now
                )
            },
            sendOfdCommand = { kkmInfo, docId ->
                // При непустой очереди операция встаёт в её конец: ставит её
                // обработка ответа, как любой документ без ответа БФД.
                // Постановка ещё и здесь была второй за одну операцию.
                if (hasQueue) {
                    OfdCommandResult(status = OfdCommandStatus.TIMEOUT)
                } else {
                    kkmCommonHelper.sendOfdCommand(kkmInfo, OfdCommandType.MONEY_PLACEMENT, docId)
                }
            },
            processResult = { kkm, documentId, currentKkmId, ofdResult, commandType, now, _ ->
                // Обработка ответа от ОФД о статусе документа
                processOfdDocumentResult.execute(
                    kkm = kkm,
                    documentId = documentId,
                    kkmId = currentKkmId,
                    ofdResult = ofdResult,
                    commandType = commandType,
                    now = now,
                    receiptContext = null
                )
                // Деньги в ящике двигает только проведённый документ. Отвергнутое
                // ОФД изъятие денег из кассы не забирает: иначе в ящике недостача
                // по операции, которой не было, — и так до ближайшего X-отчёта.
                val document = storage.findFiscalDocumentById(documentId)
                if (document != null && document.becameFiscal()) {
                    processOfdDocumentResult.updateCashSumForOperation(
                        kkmId = currentKkmId,
                        shiftId = openShift,
                        type = type,
                        amountTiyn = amountMoney.tiyn()
                    )
                    val isOffline = ofdResult.status == OfdCommandStatus.TIMEOUT
                    processOfdDocumentResult.updateMoneyPlacementCountersFromDocument(documentId, isOffline)
                }
            },
            buildResult = { documentId, ofdResult, deliveryStatus ->
                // Сборка финального объекта результата кассовой операции
                CashOperationResult(
                    documentId = documentId,
                    deliveryStatus = deliveryStatus,
                    deliveryError = BfdDeliveryFailure.reason(ofdResult),
                    bfdResultCode = BfdDeliveryFailure.code(ofdResult)
                )
            }
        )
    }
}
