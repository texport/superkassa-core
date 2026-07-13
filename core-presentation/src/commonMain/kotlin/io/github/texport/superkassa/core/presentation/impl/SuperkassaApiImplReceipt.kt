package io.github.texport.superkassa.core.presentation.impl

import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType
import io.github.texport.superkassa.core.presentation.api.model.receipt.*
import io.github.texport.superkassa.core.presentation.impl.mapper.ReceiptMapper

fun SuperkassaApiImpl.createReceiptImpl(command: CreateReceiptCommand): ReceiptResponse =
    processReceiptUseCase.execute(ReceiptMapper.toDomain(command)).let { ReceiptMapper.toResponse(it) }

fun SuperkassaApiImpl.createSellReceiptImpl(kkmId: String, pin: String, request: ReceiptSellRequest): ReceiptResponse {
    val command = ReceiptMapper.toCreateReceiptCommand(
        kkmId = kkmId,
        pin = pin,
        operation = ReceiptOperationType.SELL,
        idempotencyKey = request.idempotencyKey,
        items = request.items,
        discountPercent = request.discountPercent,
        discountSum = request.discountSum,
        markupPercent = request.markupPercent,
        markupSum = request.markupSum,
        payments = request.payments,
        taken = request.taken,
        defaultVatGroup = request.defaultVatGroup,
        customerBin = request.customerBin
    )
    return processReceiptUseCase.execute(command).let { ReceiptMapper.toResponse(it) }
}

fun SuperkassaApiImpl.createSellReturnReceiptImpl(kkmId: String, pin: String, request: ReceiptSellReturnRequest): ReceiptResponse {
    val command = ReceiptMapper.toCreateReceiptCommand(
        kkmId = kkmId,
        pin = pin,
        operation = ReceiptOperationType.SELL_RETURN,
        idempotencyKey = request.idempotencyKey,
        items = request.items,
        discountPercent = request.discountPercent,
        discountSum = request.discountSum,
        markupPercent = request.markupPercent,
        markupSum = request.markupSum,
        payments = request.payments,
        taken = request.taken,
        parentTicket = request.parentTicket,
        defaultVatGroup = request.defaultVatGroup,
        customerBin = request.customerBin
    )
    return processReceiptUseCase.execute(command).let { ReceiptMapper.toResponse(it) }
}

fun SuperkassaApiImpl.createBuyReceiptImpl(kkmId: String, pin: String, request: ReceiptBuyRequest): ReceiptResponse {
    val command = ReceiptMapper.toCreateReceiptCommand(
        kkmId = kkmId,
        pin = pin,
        operation = ReceiptOperationType.BUY,
        idempotencyKey = request.idempotencyKey,
        items = request.items,
        discountPercent = request.discountPercent,
        discountSum = request.discountSum,
        markupPercent = request.markupPercent,
        markupSum = request.markupSum,
        payments = request.payments,
        taken = request.taken,
        defaultVatGroup = request.defaultVatGroup,
        customerBin = request.customerBin
    )
    return processReceiptUseCase.execute(command).let { ReceiptMapper.toResponse(it) }
}

fun SuperkassaApiImpl.createBuyReturnReceiptImpl(kkmId: String, pin: String, request: ReceiptBuyReturnRequest): ReceiptResponse {
    val command = ReceiptMapper.toCreateReceiptCommand(
        kkmId = kkmId,
        pin = pin,
        operation = ReceiptOperationType.BUY_RETURN,
        idempotencyKey = request.idempotencyKey,
        items = request.items,
        discountPercent = request.discountPercent,
        discountSum = request.discountSum,
        markupPercent = request.markupPercent,
        markupSum = request.markupSum,
        payments = request.payments,
        taken = request.taken,
        parentTicket = request.parentTicket,
        defaultVatGroup = request.defaultVatGroup,
        customerBin = request.customerBin
    )
    return processReceiptUseCase.execute(command).let { ReceiptMapper.toResponse(it) }
}
