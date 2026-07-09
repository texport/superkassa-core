package io.github.texport.superkassa.core.presentation.impl.mapper

import io.github.texport.superkassa.core.domain.model.common.Money
import io.github.texport.superkassa.core.domain.model.receipt.ParentTicket
import io.github.texport.superkassa.core.domain.model.receipt.ReceiptOperationType
import io.github.texport.superkassa.core.domain.usecase.receipt.CreateReceiptCommand
import io.github.texport.superkassa.core.presentation.api.model.ParentTicketDto
import io.github.texport.superkassa.core.presentation.api.model.ReceiptItemDto
import io.github.texport.superkassa.core.presentation.api.model.ReceiptPaymentDto
import kotlinx.datetime.toInstant

/**
 * Маппер для преобразования HTTP DTO во входные параметры сценария доменного слоя.
 */
object ReceiptMapper {

    /**
     * Преобразовать DTO товарной позиции [ReceiptItemDto] во входные данные для команды [CreateReceiptCommand.ItemInput].
     */
    fun toItemInput(dto: ReceiptItemDto): CreateReceiptCommand.ItemInput {
        return CreateReceiptCommand.ItemInput(
            name = dto.name,
            price = dto.price,
            quantity = dto.quantity,
            barcode = dto.barcode,
            vatGroup = dto.vatGroup,
            discountPercent = dto.discountPercent,
            discountSum = dto.discountSum,
            markupPercent = dto.markupPercent,
            markupSum = dto.markupSum,
            measureUnitCode = dto.measureUnitCode,
            listExciseStamp = dto.listExciseStamp,
            ntin = dto.ntin,
            isStorno = dto.isStorno ?: false
        )
    }

    /**
     * Преобразовать DTO оплаты [ReceiptPaymentDto] во входные данные для команды [CreateReceiptCommand.PaymentInput].
     */
    fun toPaymentInput(dto: ReceiptPaymentDto): CreateReceiptCommand.PaymentInput {
        return CreateReceiptCommand.PaymentInput(
            type = dto.type,
            sum = dto.sum
        )
    }

    /**
     * Преобразовать DTO чека-основания [ParentTicketDto] в доменную модель [ParentTicket].
     */
    fun toParentTicket(dto: ParentTicketDto?): ParentTicket? {
        if (dto == null) return null
        val dateTimeStr = dto.parentTicketDateTime
        val cleanDateTimeStr = if (dateTimeStr.endsWith("Z")) {
            dateTimeStr.substring(0, dateTimeStr.length - 1)
        } else {
            dateTimeStr
        }
        val localDateTime = kotlinx.datetime.LocalDateTime.parse(cleanDateTimeStr)
        val millis = localDateTime.toInstant(
            kotlinx.datetime.TimeZone.UTC
        ).toEpochMilliseconds()
        return ParentTicket(
            parentTicketNumber = dto.parentTicketNumber,
            parentTicketDateTimeMillis = millis,
            kgdKkmId = dto.kgdKkmId,
            parentTicketTotal = Money.fromTenge(dto.parentTicketTotal),
            parentTicketIsOffline = dto.parentTicketIsOffline
        )
    }

    /**
     * Собрать команду [CreateReceiptCommand] на основе переданных параметров DTO.
     */
    fun toCreateReceiptCommand(
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
        parentTicket: ParentTicketDto? = null,
        defaultVatGroup: String? = null,
        customerBin: String? = null
    ): CreateReceiptCommand {
        return CreateReceiptCommand(
            kkmId = kkmId,
            pin = pin,
            operation = operation,
            idempotencyKey = idempotencyKey,
            items = items.map { toItemInput(it) },
            discountPercent = discountPercent,
            discountSum = discountSum,
            markupPercent = markupPercent,
            markupSum = markupSum,
            payments = payments.map { toPaymentInput(it) },
            taken = taken,
            parentTicket = toParentTicket(parentTicket),
            defaultVatGroup = defaultVatGroup,
            customerBin = customerBin
        )
    }
}
