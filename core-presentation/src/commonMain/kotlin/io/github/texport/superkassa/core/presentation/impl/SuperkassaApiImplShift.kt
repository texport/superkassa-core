package io.github.texport.superkassa.core.presentation.impl

import io.github.texport.superkassa.core.domain.api.exception.NotFoundException
import io.github.texport.superkassa.core.presentation.impl.mapper.toView
import io.github.texport.superkassa.core.presentation.impl.mapper.ReceiptMapper
import io.github.texport.superkassa.core.presentation.api.model.kkm.DocumentDetailsResponse
import io.github.texport.superkassa.core.domain.api.exception.ConflictException
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.shift.*
import io.github.texport.superkassa.core.presentation.impl.mapper.KkmMapper
import io.github.texport.superkassa.core.presentation.impl.mapper.ShiftMapper
import io.github.texport.superkassa.core.string.api.CoreStrings

fun SuperkassaApiImpl.openShiftImpl(kkmId: String, pin: String): ShiftResponse {
    logger.info("API -> openShift: kkmId='$kkmId'")
    return try {
        val result = openShiftUseCase.execute(kkmId, pin).let { ShiftMapper.toResponse(it) }
        logger.info("API -> openShift SUCCESS: shiftNo=${result.shiftNo}")
        result
    } catch (e: Exception) {
        logger.error("API -> openShift ERROR for kkmId='$kkmId'", e)
        throw e
    }
}

fun SuperkassaApiImpl.closeShiftImpl(kkmId: String, pin: String): ReportResponse {
    logger.info("API -> closeShift: kkmId='$kkmId'")
    return try {
        val result = closeShiftUseCase.execute(kkmId, pin).let { ShiftMapper.toResponse(it) }
        logger.info("API -> closeShift SUCCESS: documentId=${result.documentId}")
        result
    } catch (e: Exception) {
        logger.error("API -> closeShift ERROR for kkmId='$kkmId'", e)
        throw e
    }
}

fun SuperkassaApiImpl.getOpenShiftImpl(kkmId: String, pin: String): ShiftResponse {
    logger.debug("API -> getOpenShift: kkmId='$kkmId'")
    return try {
        kkmCommonHelper.ensureSystemTimeValid()
        val kkm = authorization.requireKkm(kkmId)
        requireOperational(kkm)
        authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN, UserRole.CASHIER))
        (
            storage.findOpenShift(kkmId)
                ?: throw ConflictException(CoreStrings.shiftNotOpen(), "SHIFT_NOT_OPEN")
            )
            .let { ShiftMapper.toResponse(it) }
    } catch (e: Exception) {
        logger.error("API -> getOpenShift ERROR for kkmId='$kkmId'", e)
        throw e
    }
}

fun SuperkassaApiImpl.getLocalOpenShiftImpl(kkmId: String, pin: String): ShiftResponse? {
    logger.debug("API -> getLocalOpenShift: kkmId='$kkmId'")
    authorization.requireKkm(kkmId)
    authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN, UserRole.CASHIER))
    return storage.findOpenShift(kkmId)?.let { ShiftMapper.toResponse(it) }
}

fun SuperkassaApiImpl.listShiftsImpl(
    kkmId: String,
    limit: Int,
    offset: Int,
    pin: String
): List<ShiftResponse> {
    logger.debug("API -> listShifts: kkmId='$kkmId', limit=$limit, offset=$offset")
    authorization.requireKkm(kkmId)
    authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN, UserRole.CASHIER))
    return storage.listShifts(kkmId, limit.coerceIn(1, 500), offset).map { ShiftMapper.toResponse(it) }
}

fun SuperkassaApiImpl.listShiftDocumentsImpl(
    kkmId: String,
    shiftId: String,
    limit: Int,
    offset: Int,
    pin: String
): List<FiscalDocumentResponse> {
    logger.debug("API -> listShiftDocuments: kkmId='$kkmId', shiftId='$shiftId'")
    val kkm = authorization.requireKkm(kkmId)
    requireOperational(kkm)
    authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN, UserRole.CASHIER))
    return storage.listFiscalDocumentsByShift(kkmId, shiftId, limit.coerceIn(1, 500), offset).map {
        KkmMapper.toResponse(it)
    }
}

fun SuperkassaApiImpl.listFiscalDocumentsByPeriodImpl(
    kkmId: String,
    fromInclusive: Long,
    toExclusive: Long,
    limit: Int,
    offset: Int,
    pin: String
): List<FiscalDocumentResponse> {
    logger.debug("API -> listFiscalDocumentsByPeriod: kkmId='$kkmId', from=$fromInclusive, to=$toExclusive")
    authorization.requireKkm(kkmId)
    authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN, UserRole.CASHIER))
    return storage.listFiscalDocumentsByPeriod(
        kkmId,
        fromInclusive,
        toExclusive,
        limit.coerceIn(1, 500),
        offset
    ).map { KkmMapper.toResponse(it) }
}

fun SuperkassaApiImpl.createReportImpl(kkmId: String, pin: String): ReportResponse {
    logger.info("API -> createReport (X-report): kkmId='$kkmId'")
    return try {
        val result = processReportUseCase.execute(kkmId, pin).let { ShiftMapper.toResponse(it) }
        logger.info("API -> createReport SUCCESS for kkmId='$kkmId'")
        result
    } catch (e: Exception) {
        logger.error("API -> createReport ERROR for kkmId='$kkmId'", e)
        throw e
    }
}

/**
 * Документ вместе с составом чека и тем, кто его оформил.
 *
 * Состав нужен возврату: вернуть можно то, что продано, и в том
 * количестве, в каком продано. Кассир нужен разбору отказов ОФД —
 * по списку документов видно только код.
 *
 * У отчётов и операций с наличными состава нет: список позиций пуст,
 * и это не ошибка.
 */
fun SuperkassaApiImpl.getDocumentDetailsImpl(
    kkmId: String,
    documentId: String,
    pin: String
): DocumentDetailsResponse {
    val kkm = authorization.requireKkm(kkmId)
    requireOperational(kkm)
    authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN, UserRole.CASHIER))
    val stored = storage.findFiscalDocumentWithReceiptPayload(documentId)
    val document = stored?.first
        ?: storage.findFiscalDocumentById(documentId)
        ?: throw NotFoundException(CoreStrings.documentNotFound(), "DOCUMENT_NOT_FOUND")
    val receipt = stored?.second
    return DocumentDetailsResponse(
        document = KkmMapper.toResponse(document),
        items = receipt?.items.orEmpty().map { ReceiptMapper.toView(it) },
        operatorName = receipt?.operatorName
    )
}
