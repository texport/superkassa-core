package io.github.texport.superkassa.core.presentation.impl

import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.presentation.api.model.ofd.*
import io.github.texport.superkassa.core.presentation.impl.mapper.OfdMapper

fun SuperkassaApiImpl.getOfdAuthInfoImpl(pin: String, request: OfdAuthInfoRequest): OfdAuthInfoResponse {
    val authInfo = getOfdAuthInfoUseCase.execute(request.kkmId, pin)
    return OfdAuthInfoResponse(token = authInfo.token, nextReqNum = authInfo.nextReqNum)
}

fun SuperkassaApiImpl.updateOfdTokenImpl(kkmId: String, pin: String, token: String): Boolean =
    updateOfdTokenUseCase.execute(kkmId, pin, token)

fun SuperkassaApiImpl.checkOfdConnectionImpl(kkmId: String): OfdCommandResponse =
    checkOfdConnectionUseCase.execute(kkmId).let { OfdMapper.toResponse(it) }

fun SuperkassaApiImpl.getOfdInfoImpl(kkmId: String): OfdCommandResponse =
    getOfdInfoUseCase.execute(kkmId).let { OfdMapper.toResponse(it) }

fun SuperkassaApiImpl.syncOfdServiceInfoImpl(kkmId: String, pin: String): OfdCommandResponse =
    syncOfdServiceInfoUseCase.execute(kkmId, pin).let { OfdMapper.toResponse(it) }

fun SuperkassaApiImpl.syncOfdCountersImpl(kkmId: String, pin: String): OfdCommandResponse =
    syncOfdCountersUseCase.execute(kkmId, pin).let { OfdMapper.toResponse(it) }

fun SuperkassaApiImpl.lookupNomenclatureImpl(pin: String, request: NomenclatureLookupRequest): NomenclatureLookupResponse {
    val kkm = authorization.requireKkm(request.kkmId)
    authorization.requireRole(kkm.id, pin, setOf(UserRole.CASHIER, UserRole.ADMIN))

    val result = lookupNomenclatureUseCase.execute(request.kkmId, request.barcode)
    return OfdMapper.toResponse(result)
}
