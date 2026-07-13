package io.github.texport.superkassa.core.presentation.impl

import io.github.texport.superkassa.core.domain.api.exception.NotFoundException
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.model.common.TaxRegime as DomainTaxRegime
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup as DomainVatGroup
import io.github.texport.superkassa.core.domain.api.model.kkm.CashOperationType
import io.github.texport.superkassa.core.presentation.api.model.common.FactoryNumberResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.*
import io.github.texport.superkassa.core.presentation.impl.mapper.KkmMapper
import io.github.texport.superkassa.core.string.api.CoreStrings

@Throws(Exception::class)
fun SuperkassaApiImpl.initKkmImpl(pin: String, request: KkmInitDirectRequest): KkmResponse =
    registerKkmUseCase.initKkm(
        pin = pin,
        ofdId = request.ofdId,
        ofdEnvironment = request.ofdEnvironment,
        ofdSystemId = request.ofdSystemId,
        ofdToken = request.ofdToken,
        kkmKgdId = request.kkmKgdId,
        factoryNumber = request.factoryNumber,
        manufactureYear = request.manufactureYear,
        serviceInfo = request.serviceInfo?.let { KkmMapper.toDomain(it) },
        okved = request.okved
    ).let { KkmMapper.toResponse(it) }

@Throws(Exception::class)
fun SuperkassaApiImpl.initKkmSimpleImpl(pin: String, request: KkmInitSimpleRequest): KkmResponse =
    registerKkmUseCase.initKkmSimple(
        pin = pin,
        ofdId = request.ofdId,
        ofdEnvironment = request.ofdEnvironment,
        ofdSystemId = request.ofdSystemId,
        ofdToken = request.ofdToken,
        defaultVatGroup = DomainVatGroup.valueOf(request.defaultVatGroup.name),
        okved = request.okved
    ).let { KkmMapper.toResponse(it) }

fun SuperkassaApiImpl.generateFactoryInfoImpl(): FactoryNumberResponse {
    val factoryNumber = idGenerator.generateFactoryNumber(coreSettings.kkmFactoryNumberPrefix)
    return FactoryNumberResponse(
        factoryNumber = factoryNumber,
        manufactureYear = clock.currentYear()
    )
}

fun SuperkassaApiImpl.getKkmImpl(id: String): KkmResponse =
    storage.findKkm(id)?.let { KkmMapper.toResponse(it) }
        ?: throw NotFoundException(
            trilingualMessage = CoreStrings.kkmNotFound(),
            code = "KKM_NOT_FOUND"
        )

fun SuperkassaApiImpl.listKkmsImpl(params: KkmListParams): KkmListResponse {
    val items = storage.listKkms(
        limit = params.limit,
        offset = params.offset,
        state = params.state,
        search = params.search,
        sortBy = params.sortBy,
        sortOrder = params.sortOrder
    ).map { KkmMapper.toResponse(it) }
    val total = storage.countKkms(state = params.state, search = params.search)
    return KkmListResponse(items = items, total = total)
}

fun SuperkassaApiImpl.deleteKkmImpl(id: String, pin: String): Boolean {
    val kkm = authorization.requireKkm(id)
    authorization.requireRole(kkm.id, pin, setOf(UserRole.ADMIN))
    return decommissionUseCase.execute(kkm)
}

fun SuperkassaApiImpl.listCountersImpl(kkmId: String, pin: String): List<CounterSnapshotResponse> {
    authorization.requireKkm(kkmId)
    authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN))
    return storage.listCounters(kkmId).map { KkmMapper.toResponse(it) }
}

fun SuperkassaApiImpl.updateKkmSettingsImpl(kkmId: String, pin: String, autoCloseShift: Boolean): KkmResponse {
    kkmCommonHelper.ensureSystemTimeValid()
    authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN))
    val kkm = authorization.requireKkm(kkmId)
    return updateSettingsUseCase.updateGeneralSettings(kkm, autoCloseShift).let { KkmMapper.toResponse(it) }
}

fun SuperkassaApiImpl.updateTaxSettingsImpl(kkmId: String, pin: String, taxRegime: TaxRegime, defaultVatGroup: VatGroup): KkmResponse {
    kkmCommonHelper.ensureSystemTimeValid()
    authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN))
    val kkm = authorization.requireKkm(kkmId)
    return updateSettingsUseCase.updateTaxSettings(
        kkm,
        DomainTaxRegime.valueOf(taxRegime.name),
        DomainVatGroup.valueOf(defaultVatGroup.name)
    ).let { KkmMapper.toResponse(it) }
}

fun SuperkassaApiImpl.updateBrandingSettingsImpl(kkmId: String, pin: String, branding: ReceiptBrandingRequest): KkmResponse {
    kkmCommonHelper.ensureSystemTimeValid()
    authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN))
    val kkm = authorization.requireKkm(kkmId)
    return updateSettingsUseCase.updateBranding(kkm, KkmMapper.toDomain(branding)).let { KkmMapper.toResponse(it) }
}

fun SuperkassaApiImpl.enterProgrammingImpl(kkmId: String, pin: String): KkmResponse {
    val kkm = authorization.requireKkm(kkmId)
    authorization.requireRole(kkm.id, pin, setOf(UserRole.ADMIN))
    return enterProgrammingUseCase.execute(kkm).let { KkmMapper.toResponse(it) }
}

fun SuperkassaApiImpl.exitProgrammingImpl(kkmId: String, pin: String): KkmResponse {
    val kkm = authorization.requireKkm(kkmId)
    authorization.requireRole(kkm.id, pin, setOf(UserRole.ADMIN))
    return exitProgrammingUseCase.execute(kkm).let { KkmMapper.toResponse(it) }
}

fun SuperkassaApiImpl.cashInImpl(kkmId: String, pin: String, request: CashOperationRequest): CashOperationResponse =
    createCashOperationUseCase.execute(kkmId, KkmMapper.toDomain(request, pin), CashOperationType.CASH_IN).let { KkmMapper.toResponse(it) }

fun SuperkassaApiImpl.cashOutImpl(kkmId: String, pin: String, request: CashOperationRequest): CashOperationResponse =
    createCashOperationUseCase.execute(kkmId, KkmMapper.toDomain(request, pin), CashOperationType.CASH_OUT).let { KkmMapper.toResponse(it) }
