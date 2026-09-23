package io.github.texport.superkassa.core.presentation.impl

import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType
import io.github.texport.superkassa.core.presentation.api.model.receipt.*
import io.github.texport.superkassa.core.presentation.impl.mapper.ReceiptMapper

fun SuperkassaApiImpl.createReceiptImpl(command: CreateReceiptCommand): ReceiptResponse {
    logger.info("API -> createReceipt: kkmId='${command.kkmId}', operation='${command.operation}'")
    return try {
        val result = processReceiptUseCase.execute(ReceiptMapper.toDomain(command)).let { ReceiptMapper.toResponse(it) }
        logger.info("API -> createReceipt SUCCESS: documentId='${result.documentId}'")
        result
    } catch (e: Exception) {
        logger.error("API -> createReceipt ERROR for kkmId='${command.kkmId}'", e)
        throw e
    }
}

fun SuperkassaApiImpl.createSellReceiptImpl(kkmId: String, pin: String, request: ReceiptSellRequest): ReceiptResponse {
    logger.info("API -> createSellReceipt: kkmId='$kkmId'")
    return try {
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
            vatGroup = request.vatGroup,
            customerBin = request.customerBin,
            domain = request.domain
        )
        val result = processReceiptUseCase.execute(command).let { ReceiptMapper.toResponse(it) }
        logger.info("API -> createSellReceipt SUCCESS: documentId='${result.documentId}'")
        result
    } catch (e: Exception) {
        logger.error("API -> createSellReceipt ERROR for kkmId='$kkmId'", e)
        throw e
    }
}

fun SuperkassaApiImpl.createSellReturnReceiptImpl(kkmId: String, pin: String, request: ReceiptSellReturnRequest): ReceiptResponse {
    logger.info("API -> createSellReturnReceipt: kkmId='$kkmId'")
    return try {
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
            vatGroup = request.vatGroup,
            customerBin = request.customerBin,
            domain = request.domain
        )
        val result = processReceiptUseCase.execute(command).let { ReceiptMapper.toResponse(it) }
        logger.info("API -> createSellReturnReceipt SUCCESS: documentId='${result.documentId}'")
        result
    } catch (e: Exception) {
        logger.error("API -> createSellReturnReceipt ERROR for kkmId='$kkmId'", e)
        throw e
    }
}

fun SuperkassaApiImpl.createBuyReceiptImpl(kkmId: String, pin: String, request: ReceiptBuyRequest): ReceiptResponse {
    logger.info("API -> createBuyReceipt: kkmId='$kkmId'")
    return try {
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
            vatGroup = request.vatGroup,
            customerBin = request.customerBin,
            domain = request.domain
        )
        val result = processReceiptUseCase.execute(command).let { ReceiptMapper.toResponse(it) }
        logger.info("API -> createBuyReceipt SUCCESS: documentId='${result.documentId}'")
        result
    } catch (e: Exception) {
        logger.error("API -> createBuyReceipt ERROR for kkmId='$kkmId'", e)
        throw e
    }
}

fun SuperkassaApiImpl.createBuyReturnReceiptImpl(kkmId: String, pin: String, request: ReceiptBuyReturnRequest): ReceiptResponse {
    logger.info("API -> createBuyReturnReceipt: kkmId='$kkmId'")
    return try {
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
            vatGroup = request.vatGroup,
            customerBin = request.customerBin,
            domain = request.domain
        )
        val result = processReceiptUseCase.execute(command).let { ReceiptMapper.toResponse(it) }
        logger.info("API -> createBuyReturnReceipt SUCCESS: documentId='${result.documentId}'")
        result
    } catch (e: Exception) {
        logger.error("API -> createBuyReturnReceipt ERROR for kkmId='$kkmId'", e)
        throw e
    }
}
