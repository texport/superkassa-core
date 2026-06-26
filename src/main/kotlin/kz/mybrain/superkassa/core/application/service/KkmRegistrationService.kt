package kz.mybrain.superkassa.core.application.service

import kz.mybrain.superkassa.core.application.error.ConflictException
import kz.mybrain.superkassa.core.application.error.ErrorMessages
import kz.mybrain.superkassa.core.application.error.ForbiddenException
import kz.mybrain.superkassa.core.application.error.ValidationException
import kz.mybrain.superkassa.core.application.model.FactoryNumberResponse
import kz.mybrain.superkassa.core.application.model.KkmInitDirectRequest
import kz.mybrain.superkassa.core.application.model.KkmInitSimpleRequest
import kz.mybrain.superkassa.core.domain.model.KkmInfo
import kz.mybrain.superkassa.core.domain.model.KkmMode
import kz.mybrain.superkassa.core.domain.model.KkmState
import kz.mybrain.superkassa.core.domain.model.OfdCommandStatus
import kz.mybrain.superkassa.core.domain.model.OfdCommandType
import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.CounterUpdaterPort
import kz.mybrain.superkassa.core.domain.port.IdGenerator
import kz.mybrain.superkassa.core.domain.port.OfdConfigPort
import kz.mybrain.superkassa.core.domain.port.OfdManagerPort
import kz.mybrain.superkassa.core.domain.port.PinHasherPort
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.port.TimeValidatorPort
import kz.mybrain.superkassa.core.domain.port.TokenCodecPort

/**
 * Сервис регистрации ККМ (создание черновиков, инициализация).
 * Делегирует логику вспомогательным классам для соблюдения лимитов на размер классов.
 */
class KkmRegistrationService(
    private val storage: StoragePort,
    private val ofd: OfdManagerPort,
    private val ofdConfig: OfdConfigPort,
    private val tokenCodec: TokenCodecPort,
    private val idGenerator: IdGenerator,
    private val clock: ClockPort,
    private val pinHasher: PinHasherPort,
    authorization: AuthorizationService,
    private val ofdCommandRequestBuilder: OfdCommandRequestBuilder,
    private val reqNumService: ReqNumService,
    counters: CounterUpdaterPort,
    private val timeValidator: TimeValidatorPort
) {
    private val registeredMode = KkmMode.REGISTRATION.name
    private val registeredState = KkmState.ACTIVE.name
    private val defaultAdminPin = "0000"

    private val kkmCommonHelper = KkmCommonHelper(
        storage = storage,
        clock = clock,
        timeValidator = timeValidator,
        tokenCodec = tokenCodec,
        reqNumService = reqNumService,
        ofdCommandRequestBuilder = ofdCommandRequestBuilder,
        ofd = ofd
    )

    private val initializer = KkmRegistrationInitializer(
        storage = storage,
        clock = clock,
        idGenerator = idGenerator,
        tokenCodec = tokenCodec,
        pinHasher = pinHasher,
        kkmCommonHelper = kkmCommonHelper
    )

    /**
     * Генерирует заводской номер и год выпуска без создания записи ККМ.
     */
    fun generateFactoryInfo(): FactoryNumberResponse {
        kkmCommonHelper.ensureSystemTimeValid()
        val factoryNumber = initializer.generateFactoryNumber()
        val year = initializer.currentYear()
        return FactoryNumberResponse(
            factoryNumber = factoryNumber,
            manufactureYear = year
        )
    }

    /**
     * Инициализация ККМ без черновика через COMMAND_SYSTEM и COMMAND_INFO.
     */
    fun initKkm(pin: String, request: KkmInitDirectRequest): KkmInfo {
        kkmCommonHelper.ensureSystemTimeValid()
        requireBootstrapAdminPin(pin)
        if (request.ofdSystemId.isBlank()) {
            throw ValidationException(ErrorMessages.kkmSystemIdRequired(), "KKM_SYSTEM_ID_REQUIRED")
        }
        val now = clock.now()
        val ofdTag = validateOfd(request.ofdId, request.ofdEnvironment)
        val registrationNumber = request.kkmKgdId
        val existingByReg = storage.findKkmByRegistrationNumber(registrationNumber)
        if (existingByReg != null) {
            throw ConflictException(ErrorMessages.kkmExists(), "KKM_EXISTS")
        }
        val existingBySystem = storage.findKkmBySystemId(request.ofdSystemId)
        if (existingBySystem != null) {
            throw ConflictException(
                ErrorMessages.kkmSystemIdExists(request.ofdSystemId),
                "KKM_SYSTEM_ID_EXISTS"
            )
        }

        val kkmId = idGenerator.nextId()
        val rawServiceInfo = request.serviceInfo ?: kkmCommonHelper.defaultServiceInfo()
        val finalServiceInfo = if (request.okved != null) {
            rawServiceInfo.copy(orgOkved = request.okved)
        } else {
            rawServiceInfo
        }

        if (finalServiceInfo.orgOkved.isBlank() || finalServiceInfo.orgOkved == "00000") {
            throw ValidationException("OKVED is required", "OKVED_REQUIRED")
        }

        val baseInfo = KkmInfo(
            id = kkmId,
            createdAt = now,
            updatedAt = now,
            mode = registeredMode,
            state = registeredState,
            ofdProvider = ofdTag,
            registrationNumber = registrationNumber,
            factoryNumber = request.factoryNumber,
            manufactureYear = request.manufactureYear,
            systemId = request.ofdSystemId,
            ofdServiceInfo = finalServiceInfo
        )

        return initializer.performKkmInitialization(
            KkmRegistrationInitializer.KkmInitializationParams(
                baseInfo = baseInfo,
                ofdToken = request.ofdToken,
                registrationNumber = registrationNumber,
                factoryNumber = request.factoryNumber,
                ofdTag = ofdTag,
                okvedOverride = request.okved,
                updateKkm = { updatedKkm ->
                    val created = storage.createKkm(updatedKkm)
                    if (!created) {
                        throw ConflictException(ErrorMessages.kkmExists(), "KKM_EXISTS")
                    }
                }
            )
        )
    }

    /**
     * Упрощенная инициализация ККМ без черновика.
     */
    fun initKkmSimple(pin: String, request: KkmInitSimpleRequest): KkmInfo {
        kkmCommonHelper.ensureSystemTimeValid()
        requireBootstrapAdminPin(pin)
        if (request.ofdSystemId.isBlank()) {
            throw ValidationException(ErrorMessages.kkmSystemIdRequired(), "KKM_SYSTEM_ID_REQUIRED")
        }
        val now = clock.now()
        val ofdTag = validateOfd(request.ofdId, request.ofdEnvironment)

        val existingBySystem = storage.findKkmBySystemId(request.ofdSystemId)
        if (existingBySystem != null) {
            throw ConflictException(
                ErrorMessages.kkmSystemIdExists(request.ofdSystemId),
                "KKM_SYSTEM_ID_EXISTS"
            )
        }

        val kkmId = idGenerator.nextId()
        val initialToken = tokenCodec.parseToken(request.ofdToken)

        val tempKkm = KkmInfo(
            id = kkmId,
            createdAt = now,
            updatedAt = now,
            mode = registeredMode,
            state = registeredState,
            ofdProvider = ofdTag,
            systemId = request.ofdSystemId,
            ofdServiceInfo = kkmCommonHelper.defaultServiceInfo()
        )

        val tempRegistrationNumber = "TEMP_REG_${kkmId.take(8)}"
        val tempFactoryNumber = "TEMP_FACTORY_${kkmId.take(8)}"
        val tempServiceInfo = kkmCommonHelper.defaultServiceInfo()

        val systemResult = kkmCommonHelper.sendOfdCommand(
            kkm = tempKkm,
            commandType = OfdCommandType.SYSTEM,
            payloadRef = idGenerator.nextId(),
            tokenOverride = initialToken,
            serviceInfoOverride = tempServiceInfo,
            registrationNumberOverride = tempRegistrationNumber,
            factoryNumberOverride = tempFactoryNumber,
            ofdProviderOverride = ofdTag
        )

        if (systemResult.status != OfdCommandStatus.OK) {
            throw ValidationException(
                ErrorMessages.ofdRequestFailed(systemResult.errorMessage),
                "OFD_COMMAND_FAILED"
            )
        }

        val infoToken = systemResult.responseToken ?: initialToken
        val infoResult = kkmCommonHelper.sendOfdCommand(
            kkm = tempKkm,
            commandType = OfdCommandType.INFO,
            payloadRef = idGenerator.nextId(),
            tokenOverride = infoToken,
            serviceInfoOverride = tempServiceInfo,
            registrationNumberOverride = tempRegistrationNumber,
            factoryNumberOverride = tempFactoryNumber,
            ofdProviderOverride = ofdTag
        )

        if (infoResult.status != OfdCommandStatus.OK) {
            throw ValidationException(
                ErrorMessages.ofdRequestFailed(infoResult.errorMessage),
                "OFD_COMMAND_FAILED"
            )
        }

        val rawServiceInfo = OfdResponseParser.extractServiceInfo(
            infoResult.responseJson,
            kkmCommonHelper.defaultServiceInfo()
        )
        val resolvedServiceInfo = if (request.okved != null) {
            rawServiceInfo.copy(orgOkved = request.okved)
        } else {
            rawServiceInfo
        }

        if (resolvedServiceInfo.orgOkved.isBlank() || resolvedServiceInfo.orgOkved == "00000") {
            throw ValidationException("OKVED is required", "OKVED_REQUIRED")
        }

        val registrationNumber = OfdResponseParser.extractRegistrationNumber(infoResult.responseJson) ?: tempRegistrationNumber
        val factoryNumber = OfdResponseParser.extractFactoryNumber(infoResult.responseJson) ?: tempFactoryNumber

        val existingByReg = storage.findKkmByRegistrationNumber(registrationNumber)
        if (existingByReg != null && existingByReg.id != kkmId) {
            throw ConflictException(ErrorMessages.kkmExists(), "KKM_EXISTS")
        }

        val now2 = clock.now()
        val shiftNo = OfdResponseParser.extractShiftNumber(infoResult.responseJson)
        val finalToken = infoResult.responseToken ?: initialToken
        val finalDefaultVatGroup = request.defaultVatGroup
        val finalTaxRegime = when (finalDefaultVatGroup) {
            kz.mybrain.superkassa.core.domain.model.VatGroup.NO_VAT ->
                kz.mybrain.superkassa.core.domain.model.TaxRegime.NO_VAT
            else ->
                kz.mybrain.superkassa.core.domain.model.TaxRegime.VAT_PAYER
        }

        val finalKkm = KkmInfo(
            id = kkmId,
            createdAt = now,
            updatedAt = now2,
            mode = registeredMode,
            state = registeredState,
            ofdProvider = ofdTag,
            registrationNumber = registrationNumber,
            factoryNumber = factoryNumber,
            manufactureYear = initializer.currentYear(),
            systemId = request.ofdSystemId,
            ofdServiceInfo = resolvedServiceInfo,
            tokenEncryptedBase64 = tokenCodec.encodeToken(finalToken),
            tokenUpdatedAt = now2,
            lastShiftNo = shiftNo,
            taxRegime = finalTaxRegime,
            defaultVatGroup = finalDefaultVatGroup
        )

        storage.inTransaction {
            val created = storage.createKkm(finalKkm)
            if (!created) {
                throw ConflictException(ErrorMessages.kkmExists(), "KKM_EXISTS")
            }
            initializer.updateCountersFromOfdInfo(finalKkm.id, infoResult.responseJson)
            initializer.ensureDefaultUsers(finalKkm.id, clock.now())
        }

        return finalKkm
    }

    private fun requireBootstrapAdminPin(pin: String) {
        if (pin != defaultAdminPin) {
            throw ForbiddenException(ErrorMessages.userForbidden(), "USER_FORBIDDEN")
        }
    }

    private fun validateOfd(providerId: String, environmentId: String): String =
        ofdConfig.validateAndFormatTag(providerId, environmentId)
}
