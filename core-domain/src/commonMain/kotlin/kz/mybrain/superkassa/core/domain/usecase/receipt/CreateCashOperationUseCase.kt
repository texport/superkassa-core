package kz.mybrain.superkassa.core.domain.usecase.receipt

import kz.mybrain.superkassa.core.domain.exception.ConflictException
import kz.mybrain.superkassa.core.domain.exception.ErrorMessages
import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.model.common.Money
import kz.mybrain.superkassa.core.domain.model.kkm.CashOperationRequest
import kz.mybrain.superkassa.core.domain.model.kkm.CashOperationResult
import kz.mybrain.superkassa.core.domain.model.kkm.CashOperationType
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandResult
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandStatus
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandType
import kz.mybrain.superkassa.core.domain.model.queue.OfflineQueueCommandRequest
import kz.mybrain.superkassa.core.domain.port.OfflineQueuePort
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.helper.common.IdempotentOperationExecutor
import kz.mybrain.superkassa.core.domain.helper.KkmCommonHelper

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
    private val processOfdDocumentResult: ProcessOfdDocumentResultUseCase
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
        if (request.amount < 0.0) {
            throw ValidationException(ErrorMessages.badRequest(), "CASH_SUM_NEGATIVE")
        }
        val amountMoney = Money.fromTenge(request.amount)

        // Выполнение идемпотентной фискальной операции
        return executor.executeIdempotentFiscalOperation(
            kkmId = kkmId,
            pin = request.pin,
            idempotencyKey = request.idempotencyKey,
            operationType = type.name,
            checkShift = {
                // Проверка наличия открытой смены на ККМ
                val shift = storage.findOpenShift(kkmId)
                    ?: throw ConflictException(ErrorMessages.shiftNotOpen(), "SHIFT_NOT_OPEN")
                shift.id
            },
            saveOperation = { docId, now, shiftId ->
                // Сохранение операции с наличными в базе данных ККМ
                storage.saveCashOperation(
                    kkmId = kkmId,
                    type = type.name,
                    amount = amountMoney,
                    documentId = docId,
                    shiftId = shiftId,
                    createdAt = now
                )
                // Обновление общего баланса наличных в ККМ по результатам проведения операции
                processOfdDocumentResult.updateCashSumForOperation(
                    kkmId = kkmId,
                    shiftId = shiftId,
                    type = type,
                    amountBills = amountMoney.bills
                )
            },
            sendOfdCommand = { kkmInfo, docId ->
                val command = OfflineQueueCommandRequest(
                    kkmId = kkmId,
                    type = OfdCommandType.MONEY_PLACEMENT.value,
                    payloadRef = docId
                )
                // Если ККМ работает в автономном/офлайн-режиме, помещаем команду в очередь
                val hasQueue = !queue.canSendDirectly(kkmId)
                if (hasQueue) {
                    queue.enqueueOffline(command)
                    OfdCommandResult(status = OfdCommandStatus.OK)
                } else {
                    // Иначе отправляем команду напрямую в ОФД через ККМ
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
                // Обновление счетчиков операций внесения/изъятия по результатам фискализации документа
                val isOffline = ofdResult.status == OfdCommandStatus.TIMEOUT
                processOfdDocumentResult.updateMoneyPlacementCountersFromDocument(documentId, isOffline)
            },
            buildResult = { documentId, ofdResult, deliveryStatus ->
                // Сборка финального объекта результата кассовой операции
                CashOperationResult(
                    documentId = documentId,
                    deliveryStatus = deliveryStatus,
                    deliveryError = ofdResult.errorMessage
                )
            }
        )
    }
}
