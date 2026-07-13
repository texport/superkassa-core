package io.github.texport.superkassa.core.presentation.impl

import io.github.texport.superkassa.core.domain.api.exception.ConflictException
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.shift.*
import io.github.texport.superkassa.core.presentation.impl.mapper.KkmMapper
import io.github.texport.superkassa.core.presentation.impl.mapper.ShiftMapper
import io.github.texport.superkassa.core.string.api.CoreStrings

fun SuperkassaApiImpl.openShiftImpl(kkmId: String, pin: String): ShiftResponse =
    openShiftUseCase.execute(kkmId, pin).let { ShiftMapper.toResponse(it) }

fun SuperkassaApiImpl.closeShiftImpl(kkmId: String, pin: String): ReportResponse =
    closeShiftUseCase.execute(kkmId, pin).let { ShiftMapper.toResponse(it) }

fun SuperkassaApiImpl.getOpenShiftImpl(kkmId: String, pin: String): ShiftResponse {
    kkmCommonHelper.ensureSystemTimeValid()
    val kkm = authorization.requireKkm(kkmId)
    requireOperational(kkm)
    authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN, UserRole.CASHIER))
    return (storage.findOpenShift(kkmId)
        ?: throw ConflictException(CoreStrings.shiftNotOpen(), "SHIFT_NOT_OPEN")).let { ShiftMapper.toResponse(it) }
}

fun SuperkassaApiImpl.listShiftsImpl(
    kkmId: String,
    limit: Int,
    offset: Int,
    pin: String
): List<ShiftResponse> {
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
    val kkm = authorization.requireKkm(kkmId)
    requireOperational(kkm)
    authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN, UserRole.CASHIER))
    return storage.listFiscalDocumentsByShift(kkmId, shiftId, limit.coerceIn(1, 500), offset).map { KkmMapper.toResponse(it) }
}

fun SuperkassaApiImpl.listFiscalDocumentsByPeriodImpl(
    kkmId: String,
    fromInclusive: Long,
    toExclusive: Long,
    limit: Int,
    offset: Int,
    pin: String
): List<FiscalDocumentResponse> {
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

fun SuperkassaApiImpl.createReportImpl(kkmId: String, pin: String): ReportResponse =
    processReportUseCase.execute(kkmId, pin).let { ShiftMapper.toResponse(it) }
