package io.github.texport.superkassa.core.domain.impl.usecase.receipt

import io.github.texport.superkassa.core.domain.impl.helper.tax.TaxCalculator
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup
import io.github.texport.superkassa.core.domain.impl.helper.ReceiptDeliveryHelper

import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.common.UnitOfMeasurement
import io.github.texport.superkassa.core.domain.api.model.receipt.PaymentType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptItem
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptPayment
import kotlin.math.roundToLong
import io.github.texport.superkassa.core.domain.api.exception.ConflictException
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.api.model.queue.OfflineQueueCommandRequest
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptResult
import io.github.texport.superkassa.core.domain.api.port.internal.OfflineQueuePort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.helper.common.IdempotentOperationExecutor
import io.github.texport.superkassa.core.domain.impl.helper.KkmCommonHelper
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.kkm.RequireOperationalUseCase

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
 * @property authorizeUser Сценарий авторизации и загрузки ККМ.
 * @property requireOperational Сценарий проверки работоспособности ККМ.
 * @property processOfdDocumentResult Лямбда-функция для обработки результатов фискализации документов в ОФД.
 * @property ofdResultQueuedOffline Лямбда-функция для создания фиктивного успешного ответа при офлайн-очереди.
 */
class ProcessReceiptUseCase(
    private val storage: StoragePort,
    private val queue: OfflineQueuePort,
    private val fiscalOperationExecutor: IdempotentOperationExecutor,
    private val kkmCommonHelper: KkmCommonHelper,
    private val receiptDeliveryHelper: ReceiptDeliveryHelper,
    private val authorizeUser: AuthorizeUserUseCase,
    private val requireOperational: RequireOperationalUseCase,
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
    fun execute(command: CreateReceiptCommand): ReceiptResult {
        val kkm = authorizeUser.requireKkm(command.kkmId)
        requireOperational.execute(kkm)

        // 1. Валидация входных данных команды
        if ((command.operation == ReceiptOperationType.SELL_RETURN || command.operation == ReceiptOperationType.BUY_RETURN) && command.parentTicket == null) {
            throw ValidationException(
                CoreStrings.parentTicketRequiredForReturns(),
                "PARENT_TICKET_REQUIRED"
            )
        }

        // 2. Расчет сумм и маппинг позиций
        val receiptItems = command.items.map { dto ->
            val baseSumTenge = dto.price * dto.quantity
            val itemDiscountTenge = when {
                dto.discountPercent != null -> baseSumTenge * dto.discountPercent / 100.0
                dto.discountSum != null -> dto.discountSum
                else -> 0.0
            }
            val itemMarkupTenge = when {
                dto.markupPercent != null -> baseSumTenge * dto.markupPercent / 100.0
                dto.markupSum != null -> dto.markupSum
                else -> 0.0
            }
            val itemSumTenge = (baseSumTenge - itemDiscountTenge + itemMarkupTenge).coerceAtLeast(0.0)
            val itemDiscount = if (itemDiscountTenge > 0) Money.fromTenge(itemDiscountTenge) else null
            val itemMarkup = if (itemMarkupTenge > 0) Money.fromTenge(itemMarkupTenge) else null
            val measureUnitCode = dto.measureUnitCode?.takeIf { it.isNotBlank() }?.let { raw ->
                try {
                    UnitOfMeasurement.fromCode(raw).code
                } catch (_: IllegalArgumentException) {
                    throw ValidationException(CoreStrings.measureUnitCodeInvalid(raw), "MEASURE_UNIT_CODE_INVALID")
                }
            }
            ReceiptItem(
                name = dto.name,
                sectionCode = "001",
                quantity = (dto.quantity * 1000).roundToLong(),
                price = Money.fromTenge(dto.price),
                sum = Money.fromTenge(itemSumTenge),
                barcode = dto.barcode?.takeIf { it.isNotBlank() },
                vatGroup = dto.vatGroup?.let { value ->
                    try {
                        VatGroup.valueOf(value)
                    } catch (_: IllegalArgumentException) {
                        throw IllegalArgumentException(
                            "Invalid vatGroup: $value. Valid: " +
                                VatGroup.entries.joinToString { it.name }
                        )
                    }
                },
                discount = itemDiscount,
                markup = itemMarkup,
                measureUnitCode = measureUnitCode,
                listExciseStamp = dto.listExciseStamp?.takeIf { it.isNotEmpty() },
                ntin = dto.ntin?.takeIf { it.isNotBlank() },
                isStorno = dto.isStorno
            )
        }

        // 3. Проверка конфликтов скидок на уровне позиций и чека
        val hasItemDiscounts = receiptItems.any { it.discount != null }
        val hasReceiptDiscount = command.discountPercent != null || command.discountSum != null
        if (hasItemDiscounts && hasReceiptDiscount) {
            throw ValidationException(
                CoreStrings.receiptDiscountScopesConflict(),
                "RECEIPT_DISCOUNT_SCOPES_CONFLICT"
            )
        }

        // 4. Расчет сумм скидок/наценок на чек и итоговой суммы чека
        val itemsTotalTenge = receiptItems.sumOf { item ->
            val sign = if (item.isStorno) -1.0 else 1.0
            (item.sum.bills + item.sum.coins / 100.0) * sign
        }
        val receiptDiscountTenge = when {
            command.discountPercent != null -> itemsTotalTenge * command.discountPercent / 100.0
            command.discountSum != null -> command.discountSum
            else -> 0.0
        }
        val receiptMarkupTenge = when {
            command.markupPercent != null -> itemsTotalTenge * command.markupPercent / 100.0
            command.markupSum != null -> command.markupSum
            else -> 0.0
        }
        val totalTenge = (itemsTotalTenge - receiptDiscountTenge + receiptMarkupTenge).coerceAtLeast(0.0)
        val totalMoney = Money.fromTenge(totalTenge)

        // 5. Оплаты и сдача
        val receiptPayments = command.payments.map { dto ->
            val paymentType = try {
                PaymentType.valueOf(dto.type)
            } catch (_: IllegalArgumentException) {
                throw IllegalArgumentException(
                    "Invalid payment type: ${dto.type}. Valid: ${PaymentType.entries.joinToString { it.name }}"
                )
            }
            ReceiptPayment(type = paymentType, sum = Money.fromTenge(dto.sum))
        }

        val cashSumTenge = receiptPayments
            .filter { it.type == PaymentType.CASH }
            .sumOf { it.sum.bills + it.sum.coins / 100.0 }
        val (takenMoney, changeMoney) = when {
            command.taken == null -> {
                Pair(Money.fromTenge(cashSumTenge), null)
            }
            else -> {
                require(command.taken >= cashSumTenge) {
                    "taken must be >= sum of CASH payments (cashSum=$cashSumTenge, taken=${command.taken})"
                }
                val changeTenge = (command.taken - totalTenge).coerceAtLeast(0.0)
                Pair(Money.fromTenge(command.taken), Money.fromTenge(changeTenge))
            }
        }

        val receiptDiscount = if (receiptDiscountTenge > 0) Money.fromTenge(receiptDiscountTenge) else null
        val receiptMarkup = if (receiptMarkupTenge > 0) Money.fromTenge(receiptMarkupTenge) else null
        val defaultVatGroup = command.defaultVatGroup?.takeIf { it.isNotBlank() }?.let { value ->
            try {
                VatGroup.valueOf(value)
            } catch (_: IllegalArgumentException) {
                throw IllegalArgumentException(
                    "Invalid defaultVatGroup: $value. Valid: " + VatGroup.entries.joinToString { it.name }
                )
            }
        }

        // 6. Подготовка доменного ReceiptRequest
        val request = ReceiptRequest(
            kkmId = command.kkmId,
            pin = command.pin,
            operation = command.operation,
            items = receiptItems,
            payments = receiptPayments,
            total = totalMoney,
            taken = takenMoney,
            change = changeMoney,
            idempotencyKey = command.idempotencyKey,
            parentTicket = command.parentTicket,
            defaultVatGroup = defaultVatGroup ?: kkm.defaultVatGroup,
            taxRegime = kkm.taxRegime,
            discount = receiptDiscount,
            markup = receiptMarkup,
            customerBin = command.customerBin
        )

        // Расчет сумм налогов (НДС) по каждой позиции чека
        val taxResult = taxCalculator.calculateTicketTaxes(
            items = request.items,
            taxRegime = request.taxRegime,
            defaultVatGroup = request.defaultVatGroup ?: VatGroup.NO_VAT
        )
        val requestWithTaxes = request.copy(
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
                    ?: throw ConflictException(CoreStrings.shiftNotOpen(), "SHIFT_NOT_OPEN")
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
