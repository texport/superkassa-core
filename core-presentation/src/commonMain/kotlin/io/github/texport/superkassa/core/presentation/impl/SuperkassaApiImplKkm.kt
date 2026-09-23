package io.github.texport.superkassa.core.presentation.impl

import io.github.texport.superkassa.core.domain.api.exception.ConflictException
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
fun SuperkassaApiImpl.initKkmImpl(request: KkmInitDirectRequest): KkmResponse =
    registerKkmUseCase.initKkm(
        ofdId = request.ofdId,
        ofdEnvironment = request.ofdEnvironment,
        ofdSystemId = request.ofdSystemId,
        ofdToken = request.ofdToken,
        kkmKgdId = request.kkmKgdId,
        factoryNumber = request.factoryNumber,
        manufactureYear = request.manufactureYear,
        serviceInfo = request.serviceInfo?.let { KkmMapper.toDomain(it) },
        oked = request.oked,
        adminPin = request.adminPin.orEmpty()
    ).let { KkmMapper.toResponse(it) }

@Throws(Exception::class)
fun SuperkassaApiImpl.initKkmSimpleImpl(request: KkmInitSimpleRequest): KkmResponse {
    logger.info(
        "API -> initKkmSimple: systemId='{}', ofdId='{}', env='{}'",
        request.ofdSystemId,
        request.ofdId,
        request.ofdEnvironment
    )
    return try {
        val result = registerKkmUseCase.initKkmSimple(
            ofdId = request.ofdId,
            ofdEnvironment = request.ofdEnvironment,
            ofdSystemId = request.ofdSystemId,
            ofdToken = request.ofdToken,
            defaultVatGroup = DomainVatGroup.valueOf(request.defaultVatGroup.name),
            oked = request.oked,
            adminPin = request.adminPin.orEmpty()
        )
        logger.info("API -> initKkmSimple SUCCESS: kkmId='{}'", result.id)
        KkmMapper.toResponse(result)
    } catch (e: Exception) {
        logger.error("API -> initKkmSimple ERROR for systemId='${request.ofdSystemId}'", e)
        throw e
    }
}

fun SuperkassaApiImpl.generateFactoryInfoImpl(): FactoryNumberResponse {
    val factoryNumber = idGenerator.generateFactoryNumber(coreSettings.kkmFactoryNumberPrefix)
    return FactoryNumberResponse(
        factoryNumber = factoryNumber,
        manufactureYear = clock.currentYear()
    )
}

fun SuperkassaApiImpl.getKkmImpl(id: String): KkmResponse {
    val stored = storage.findKkm(id) ?: throw NotFoundException(
        trilingualMessage = CoreStrings.kkmNotFound(),
        code = "KKM_NOT_FOUND"
    )

    // Автономный признак сверяется с очередью при чтении кассы: очередь
    // расходится в фоне, и до этой сверки касса числилась автономной,
    // пока кассир не пробьёт следующий чек. Превышенный лимит автономной
    // работы здесь только записывается: чтение кассы отказом не отвечает,
    // об этом скажет само действие.
    val kkm = try {
        enforceAutonomousLimitsUseCase.execute(stored)
    } catch (limitExceeded: ConflictException) {
        storage.findKkm(id) ?: stored
    }

    val openShift = storage.findOpenShift(id)
    val queueStatus = try {
        queue.getQueueStatus(io.github.texport.superkassa.core.presentation.api.model.queue.QueueStatusRequest(id))
    } catch (e: Exception) {
        null
    }

    val lastError = try {
        val tasks = storage.listQueueTasksByCashbox(id, "OFFLINE", 20)
        tasks.firstOrNull { it.status == "FAILED" }?.lastError
    } catch (e: Exception) {
        null
    }

    return KkmMapper.toResponse(kkm).copy(
        isShiftOpen = openShift != null,
        shiftOpenedAt = openShift?.openedAt,
        offlineQueueCount = queueStatus?.pendingCount ?: 0,
        stuckQueueCount = queueStatus?.rejectedCount ?: 0,
        lastSyncError = lastError
    )
}

fun SuperkassaApiImpl.listKkmsImpl(params: KkmListParams): KkmListResponse {
    val items = storage.listKkms(
        limit = params.limit,
        offset = params.offset,
        state = params.state,
        search = params.search,
        sortBy = params.sortBy,
        sortOrder = params.sortOrder
    ).map { kkm ->
        val openShift = storage.findOpenShift(kkm.id)
        val queueStatus = try {
            queue.getQueueStatus(
                io.github.texport.superkassa.core.presentation.api.model.queue.QueueStatusRequest(kkm.id)
            )
        } catch (e: Exception) {
            null
        }
        val lastError = try {
            val tasks = storage.listQueueTasksByCashbox(kkm.id, "OFFLINE", 20)
            tasks.firstOrNull { it.status == "FAILED" }?.lastError
        } catch (e: Exception) {
            null
        }
        KkmMapper.toResponse(kkm).copy(
            isShiftOpen = openShift != null,
            shiftOpenedAt = openShift?.openedAt,
            offlineQueueCount = queueStatus?.pendingCount ?: 0,
            stuckQueueCount = queueStatus?.rejectedCount ?: 0,
            lastSyncError = lastError
        )
    }
    val total = storage.countKkms(state = params.state, search = params.search)
    return KkmListResponse(items = items, total = total)
}

fun SuperkassaApiImpl.deleteKkmImpl(id: String, pin: String): Boolean {
    val kkm = authorization.requireKkm(id)
    authorization.requireRole(kkm.id, pin, setOf(UserRole.ADMIN))
    return decommissionUseCase.execute(kkm)
}

fun SuperkassaApiImpl.validateCanDeleteKkmImpl(id: String, pin: String): Boolean {
    val kkm = authorization.requireKkm(id)
    authorization.requireRole(kkm.id, pin, setOf(UserRole.ADMIN))
    decommissionUseCase.validateCanDelete(kkm)
    return true
}

fun SuperkassaApiImpl.listCountersImpl(kkmId: String, pin: String): List<CounterSnapshotResponse> {
    authorization.requireKkm(kkmId)
    authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN, UserRole.CASHIER))
    return storage.listCounters(kkmId).map { KkmMapper.toResponse(it) }
}

fun SuperkassaApiImpl.updateKkmSettingsImpl(kkmId: String, pin: String, autoCloseShift: Boolean, autoCashout: Boolean): KkmResponse {
    kkmCommonHelper.ensureSystemTimeValid()
    authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN))
    val kkm = authorization.requireKkm(kkmId)
    return updateSettingsUseCase.updateGeneralSettings(
        kkm,
        autoCloseShift,
        autoCashout
    ).let { KkmMapper.toResponse(it) }
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

/**
 * Пишет название кассы на узел.
 *
 * Пин спрашивается, как у соседних настроек, но роли не различаются:
 * название не влияет ни на чек, ни на смену, а даёт его тот, кто в этот
 * момент за кассой. Требование прав администратора оставило бы кассы
 * безымянными на всех рабочих местах, где владелец не входит сам.
 */
fun SuperkassaApiImpl.updateKkmNameImpl(kkmId: String, pin: String, name: String?): KkmResponse {
    authorization.requireRole(kkmId, pin, setOf(UserRole.ADMIN, UserRole.CASHIER))
    val kkm = authorization.requireKkm(kkmId)
    return updateSettingsUseCase.updateName(kkm, name).let { KkmMapper.toResponse(it) }
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
    createCashOperationUseCase.execute(
        kkmId,
        KkmMapper.toDomain(request, pin),
        CashOperationType.CASH_IN
    ).let { KkmMapper.toResponse(it) }

fun SuperkassaApiImpl.cashOutImpl(kkmId: String, pin: String, request: CashOperationRequest): CashOperationResponse =
    createCashOperationUseCase.execute(
        kkmId,
        KkmMapper.toDomain(request, pin),
        CashOperationType.CASH_OUT
    ).let { KkmMapper.toResponse(it) }
