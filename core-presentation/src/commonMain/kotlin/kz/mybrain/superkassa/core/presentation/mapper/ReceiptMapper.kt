package kz.mybrain.superkassa.core.presentation.mapper

import kz.mybrain.superkassa.core.domain.exception.ErrorMessages
import kz.mybrain.superkassa.core.domain.exception.TrilingualMessage
import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.model.common.Money
import kz.mybrain.superkassa.core.domain.model.common.UnitOfMeasurement
import kz.mybrain.superkassa.core.domain.model.common.VatGroup
import kz.mybrain.superkassa.core.domain.model.receipt.ParentTicket
import kz.mybrain.superkassa.core.domain.model.receipt.PaymentType
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptItem
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptOperationType
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptPayment
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptRequest
import kz.mybrain.superkassa.core.presentation.model.ParentTicketDto
import kz.mybrain.superkassa.core.presentation.model.ReceiptItemDto
import kz.mybrain.superkassa.core.presentation.model.ReceiptPaymentDto
import kotlinx.datetime.Instant

/**
 * Маппер для преобразования HTTP DTO в domain модели для чеков.
 * Код секции (sectionCode) заполняется на стороне сервера по умолчанию "001".
 */
object ReceiptMapper {

    private const val DEFAULT_SECTION_CODE = "001"

    /**
     * Преобразовать DTO товарной позиции [ReceiptItemDto] в доменную модель [ReceiptItem].
     * Сумма позиции вычисляется: price × quantity − скидка + наценка (по позиции).
     *
     * @param dto DTO позиции чека.
     * @return Доменная модель позиции чека.
     */
    fun toReceiptItem(dto: ReceiptItemDto): ReceiptItem {
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
                throw ValidationException(ErrorMessages.measureUnitCodeInvalid(raw), "MEASURE_UNIT_CODE_INVALID")
            }
        }
        return ReceiptItem(
            name = dto.name,
            sectionCode = DEFAULT_SECTION_CODE,
            quantity = dto.quantity,
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
            isStorno = dto.isStorno ?: false
        )
    }

    /**
     * Преобразовать DTO оплаты [ReceiptPaymentDto] в доменную модель [ReceiptPayment].
     *
     * @param dto DTO оплаты.
     * @return Доменная модель оплаты.
     */
    fun toReceiptPayment(dto: ReceiptPaymentDto): ReceiptPayment {
        val paymentType = try {
            PaymentType.valueOf(dto.type)
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Invalid payment type: ${dto.type}. Valid: ${PaymentType.entries.joinToString { it.name }}"
            )
        }
        return ReceiptPayment(type = paymentType, sum = Money.fromTenge(dto.sum))
    }

    /**
     * Преобразовать DTO чека-основания [ParentTicketDto] в доменную модель [ParentTicket].
     */
    private fun toParentTicket(dto: ParentTicketDto?): ParentTicket? {
        if (dto == null) return null
        val instant = Instant.parse(dto.parentTicketDateTime)
        val millis = instant.toEpochMilliseconds()
        return ParentTicket(
            parentTicketNumber = dto.parentTicketNumber,
            parentTicketDateTimeMillis = millis,
            kgdKkmId = dto.kgdKkmId,
            parentTicketTotal = Money.fromTenge(dto.parentTicketTotal),
            parentTicketIsOffline = dto.parentTicketIsOffline
        )
    }

    /**
     * Собрать полный [ReceiptRequest] для ядра на основе переданных параметров DTO.
     * Общая сумма чека вычисляется на сервере: сумма по всем позициям (уже с учётом скидок/наценок по позиции)
     * минус скидка на чек плюс наценка на чек.
     */
    fun toReceiptRequest(
        kkmId: String,
        pin: String,
        operation: ReceiptOperationType,
        idempotencyKey: String,
        items: List<ReceiptItemDto>,
        discountPercent: Double?,
        discountSum: Double?,
        markupPercent: Double?,
        markupSum: Double?,
        payments: List<ReceiptPaymentDto>,
        taken: Double?,
        @Suppress("UNUSED_PARAMETER") change: Double?, // не передаём в ОФД; сдача считается по taken и total
        parentTicket: ParentTicketDto? = null,
        defaultVatGroup: String? = null,
        customerBin: String? = null
    ): ReceiptRequest {
        if ((operation == ReceiptOperationType.SELL_RETURN || operation == ReceiptOperationType.BUY_RETURN) && parentTicket == null) {
            throw ValidationException(
                TrilingualMessage(
                    ru = "Чек-основание (parentTicket) обязателен при возврате",
                    kk = "Қайтару кезінде негізгі чек (parentTicket) міндетті болып табылады",
                    en = "Parent ticket (parentTicket) is required for returns"
                ),
                "PARENT_TICKET_REQUIRED"
            )
        }
        val receiptItems = items.map { toReceiptItem(it) }
        val hasItemDiscounts = receiptItems.any { it.discount != null }
        val hasReceiptDiscount = discountPercent != null || discountSum != null
        if (hasItemDiscounts && hasReceiptDiscount) {
            throw ValidationException(
                TrilingualMessage(
                    ru = "Нельзя одновременно применять скидки на уровне позиций и на уровне всего чека",
                    kk = "Бір чек ішінде позиция деңгейіндегі және бүкіл чек деңгейіндегі жеңілдіктерді бір уақытта қолдануға болмайды",
                    en = "Cannot apply both item-level and receipt-level discounts in a single receipt"
                ),
                "RECEIPT_DISCOUNT_SCOPES_CONFLICT"
            )
        }
        val itemsTotalTenge = receiptItems.sumOf { it.sum.bills + it.sum.coins / 100.0 }
        val receiptDiscountTenge = when {
            discountPercent != null -> itemsTotalTenge * discountPercent / 100.0
            discountSum != null -> discountSum
            else -> 0.0
        }
        val receiptMarkupTenge = when {
            markupPercent != null -> itemsTotalTenge * markupPercent / 100.0
            markupSum != null -> markupSum
            else -> 0.0
        }
        val totalTenge = (itemsTotalTenge - receiptDiscountTenge + receiptMarkupTenge).coerceAtLeast(0.0)
        val totalMoney = Money.fromTenge(totalTenge)
        val receiptPayments = payments.map { toReceiptPayment(it) }
        val cashSumTenge = receiptPayments
            .filter { it.type == PaymentType.CASH }
            .sumOf { it.sum.bills + it.sum.coins / 100.0 }
        val (takenMoney, changeMoney) = when {
            taken == null -> {
                Pair(Money.fromTenge(cashSumTenge), null)
            }
            else -> {
                require(taken >= cashSumTenge) {
                    "taken must be >= sum of CASH payments (cashSum=$cashSumTenge, taken=$taken)"
                }
                val changeTenge = (taken - totalTenge).coerceAtLeast(0.0)
                Pair(Money.fromTenge(taken), Money.fromTenge(changeTenge))
            }
        }
        val receiptDiscount = if (receiptDiscountTenge > 0) Money.fromTenge(receiptDiscountTenge) else null
        val receiptMarkup = if (receiptMarkupTenge > 0) Money.fromTenge(receiptMarkupTenge) else null
        val vatGroup = defaultVatGroup?.takeIf { it.isNotBlank() }?.let { value ->
            try {
                VatGroup.valueOf(value)
            } catch (_: IllegalArgumentException) {
                throw IllegalArgumentException(
                    "Invalid defaultVatGroup: $value. Valid: " + VatGroup.entries.joinToString { it.name }
                )
            }
        }
        return ReceiptRequest(
            kkmId = kkmId,
            pin = pin,
            operation = operation,
            items = receiptItems,
            payments = receiptPayments,
            total = totalMoney,
            taken = takenMoney,
            change = changeMoney,
            idempotencyKey = idempotencyKey,
            parentTicket = toParentTicket(parentTicket),
            defaultVatGroup = vatGroup,
            discount = receiptDiscount,
            markup = receiptMarkup,
            customerBin = customerBin
        )
    }
}
