package kz.mybrain.superkassa.core.domain.usecase.receipt

import kz.mybrain.superkassa.core.domain.helper.tax.TaxCalculator
import kz.mybrain.superkassa.core.domain.model.common.VatGroup
import kz.mybrain.superkassa.core.domain.helper.ReceiptDeliveryHelper

import kz.mybrain.superkassa.core.domain.exception.ConflictException
import kz.mybrain.superkassa.core.domain.exception.ErrorMessages
import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandResult
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandType
import kz.mybrain.superkassa.core.domain.model.queue.OfflineQueueCommandRequest
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptRequest
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptResult
import kz.mybrain.superkassa.core.domain.port.OfflineQueuePort
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.helper.common.IdempotentOperationExecutor
import kz.mybrain.superkassa.core.domain.helper.KkmCommonHelper

/**
 * Сценарий обработки чека продажи или возврата на ККМ.
 *
 * Осуществляет расчет налогов (НДС), сохраняет информацию о чеке в локальное хранилище данных,
 * ставит команду в офлайн-очередь или сразу отправляет в ОФД, инициирует доставку чека покупателю
 * и возвращает фискальный результат выполнения.
 *
 * @property storage Порт для доступа к персистентному хранилищу данных.
 * @property queue Порт для работы с офлайн-очередью команд ККМ.
 * @property fiscalOperationExecutor Компонент обеспечения идемпотентности фискальных операций.
 * @property kkmCommonHelper Вспомогательный класс для выполнения общих операций ККМ.
 * @property receiptDeliveryHelper Помощник для форматирования и отправки фискальных чеков.
 * @property processOfdDocumentResult Лямбда-функция для обработки результатов фискализации документов в ОФД.
 * @property ofdResultQueuedOffline Лямбда-функция для создания фиктивного успешного ответа при офлайн-очереди.
 */
class ProcessReceiptUseCase(
    private val storage: StoragePort,
    private val queue: OfflineQueuePort,
    private val fiscalOperationExecutor: IdempotentOperationExecutor,
    private val kkmCommonHelper: KkmCommonHelper,
    private val receiptDeliveryHelper: ReceiptDeliveryHelper,
    private val processOfdDocumentResult:
    (KkmInfo, String, String, OfdCommandResult, OfdCommandType, Long, Pair<ReceiptRequest, String>?) -> Unit,
    private val ofdResultQueuedOffline: () -> OfdCommandResult
) {
    /**
     * Калькулятор для вычисления налогов на товары и услуги в чеке.
     */
    private val taxCalculator = TaxCalculator()

    /**
     * Выполняет обработку и регистрацию фискального чека.
     *
     * @param request Запрос чека с перечнем товаров, платежей и настроек доставки.
     * @param kkm Информация о текущем кассовом аппарате (налоговый режим, дефолтная группа НДС).
     * @return Объект результата обработки чека с фискальными признаками.
     * @throws ConflictException если смена на ККМ закрыта.
     */
    fun execute(request: ReceiptRequest, kkm: KkmInfo): ReceiptResult {
        // Подготовка запроса чека с заполнением налогового режима и НДС по умолчанию из настроек ККМ
        val requestWithTaxSettings = request.copy(
            taxRegime = kkm.taxRegime,
            defaultVatGroup = request.defaultVatGroup ?: kkm.defaultVatGroup
        )

        // Расчет сумм налогов (НДС) по каждой позиции чека
        val taxResult = taxCalculator.calculateTicketTaxes(
            items = requestWithTaxSettings.items,
            taxRegime = requestWithTaxSettings.taxRegime,
            defaultVatGroup = requestWithTaxSettings.defaultVatGroup ?: VatGroup.NO_VAT
        )
        val requestWithTaxes = requestWithTaxSettings.copy(
            ticketTaxes = taxResult.ticketTaxes
        )

        // Запуск фискальной операции с гарантией идемпотентности
        return fiscalOperationExecutor.executeIdempotentFiscalOperation(
            kkmId = requestWithTaxes.kkmId,
            pin = requestWithTaxes.pin,
            idempotencyKey = requestWithTaxes.idempotencyKey,
            operationType = "CREATE_RECEIPT",
            checkShift = {
                // Проверяем, что смена открыта, и получаем её ID
                val shift = storage.findOpenShift(requestWithTaxes.kkmId)
                    ?: throw ConflictException(ErrorMessages.shiftNotOpen(), "SHIFT_NOT_OPEN")
                shift.id
            },
            saveOperation = { documentId, now, shiftId ->
                // Сохраняем рассчитанный чек в базу данных
                storage.saveReceipt(requestWithTaxes, documentId, shiftId, now)
            },
            sendOfdCommand = { currentKkm, documentId ->
                val hasQueue = !queue.canSendDirectly(requestWithTaxes.kkmId)
                val command = OfflineQueueCommandRequest(
                    kkmId = requestWithTaxes.kkmId,
                    type = OfdCommandType.TICKET.value,
                    payloadRef = documentId
                )
                // Если ККМ работает офлайн, ставим чек в очередь, иначе шлем в ОФД сразу
                if (hasQueue) {
                    queue.enqueueOffline(command)
                    ofdResultQueuedOffline()
                } else {
                    kkmCommonHelper.sendOfdCommand(
                        kkm = currentKkm,
                        commandType = OfdCommandType.TICKET,
                        payloadRef = documentId
                    )
                }
            },
            processResult = { currentKkm, documentId, currentKkmId, ofdResult, commandType, now, receiptContext ->
                // Обрабатываем ответ ОФД (обновление счетчиков, перевод ККМ в автономный режим при таймаутах)
                processOfdDocumentResult(
                    currentKkm,
                    documentId,
                    currentKkmId,
                    ofdResult,
                    commandType,
                    now,
                    receiptContext
                )
                // Если чек успешно фискализован в ОФД, инициируем его отправку покупателю
                if (commandType == OfdCommandType.TICKET && receiptContext != null && ofdResult.resultCode == 0) {
                    val (receipt, _) = receiptContext
                    val doc = storage.findFiscalDocumentById(documentId)
                    if (doc != null) {
                        receiptDeliveryHelper.deliverReceipt(
                            kkmId = requestWithTaxes.kkmId,
                            documentId = documentId,
                            receipt = receipt,
                            docSnapshot = doc,
                            receiptUrl = ofdResult.receiptUrl,
                            responseBin = ofdResult.responseBin
                        )
                    }
                }
            },
            buildResult = { documentId, ofdResult, deliveryStatus ->
                // Сборка ответа для вызывающего слоя презентации
                val doc = storage.findFiscalDocumentById(documentId)
                ReceiptResult(
                    documentId = documentId,
                    fiscalSign = doc?.fiscalSign ?: ofdResult.fiscalSign,
                    autonomousSign = doc?.autonomousSign ?: ofdResult.autonomousSign,
                    deliveryPayload = ofdResult.responseBin,
                    deliveryStatus = deliveryStatus,
                    deliveryError = ofdResult.errorMessage
                )
            },
            receiptContextProvider = { shiftId -> Pair(requestWithTaxes, shiftId) }
        )
    }
}
