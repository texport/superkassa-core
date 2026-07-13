package io.github.texport.superkassa.core.presentation.impl.mapper

import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.receipt.ParentTicket
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType
import io.github.texport.superkassa.core.domain.impl.usecase.receipt.CreateReceiptCommand
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptResult
import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus
import io.github.texport.superkassa.core.presentation.api.model.receipt.*
import kotlinx.datetime.toInstant

/**
 * Маппер для преобразования HTTP DTO во входные параметры сценария доменного слоя.
 */
object ReceiptMapper {

    /**
     * Преобразовать DTO товарной позиции [ReceiptItemRequest] во входные данные для команды [CreateReceiptCommand.ItemInput].
     */
    fun toItemInput(dto: ReceiptItemRequest): CreateReceiptCommand.ItemInput {
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
     * Преобразовать DTO оплаты [ReceiptPaymentRequest] во входные данные для команды [CreateReceiptCommand.PaymentInput].
     */
    fun toPaymentInput(dto: ReceiptPaymentRequest): CreateReceiptCommand.PaymentInput {
        return CreateReceiptCommand.PaymentInput(
            type = dto.type,
            sum = dto.sum
        )
    }

    /**
     * Преобразовать DTO чека-основания [ParentTicketRequest] в доменную модель [ParentTicket].
     */
    fun toParentTicket(dto: ParentTicketRequest?): ParentTicket? {
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
        items: List<ReceiptItemRequest>,
        discountPercent: Double?,
        discountSum: Double?,
        markupPercent: Double?,
        markupSum: Double?,
        payments: List<ReceiptPaymentRequest>,
        taken: Double?,
        parentTicket: ParentTicketRequest? = null,
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

    fun toResponse(result: ReceiptResult): ReceiptResponse = ReceiptResponse(
        documentId = result.documentId,
        fiscalSign = result.fiscalSign,
        autonomousSign = result.autonomousSign,
        deliveryPayload = result.deliveryPayload,
        deliveryStatus = DeliveryStatus.valueOf(result.deliveryStatus.name),
        deliveryError = result.deliveryError
    )

    fun toDomain(dto: io.github.texport.superkassa.core.presentation.api.model.receipt.CreateReceiptCommand): io.github.texport.superkassa.core.domain.impl.usecase.receipt.CreateReceiptCommand =
        io.github.texport.superkassa.core.domain.impl.usecase.receipt.CreateReceiptCommand(
            kkmId = dto.kkmId,
            pin = dto.pin,
            operation = io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType.valueOf(dto.operation),
            idempotencyKey = dto.idempotencyKey,
            items = dto.items.map { toItemInput(it) },
            discountPercent = dto.discountPercent,
            discountSum = dto.discountSum,
            markupPercent = dto.markupPercent,
            markupSum = dto.markupSum,
            payments = dto.payments.map { toPaymentInput(it) },
            taken = dto.taken,
            parentTicket = toParentTicket(dto.parentTicket),
            defaultVatGroup = dto.defaultVatGroup,
            customerBin = dto.customerBin
        )

    fun toDomain(layout: ReceiptLayoutType): io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptLayoutType =
        io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptLayoutType.valueOf(layout.name)

    fun toDomain(type: PrintDocumentType): io.github.texport.superkassa.core.domain.api.model.report.PrintDocumentType =
        io.github.texport.superkassa.core.domain.api.model.report.PrintDocumentType.valueOf(type.name)
}
