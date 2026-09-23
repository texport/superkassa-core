package io.github.texport.superkassa.core.domain.impl.usecase.kkm

import io.github.texport.superkassa.core.domain.api.exception.ConflictException
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.domain.api.exception.ForbiddenException
import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.auth.StandardPin
import io.github.texport.superkassa.core.domain.api.model.common.TaxRegime
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmMode
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdServiceInfo
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.internal.IdGeneratorPort
import io.github.texport.superkassa.core.domain.api.port.internal.OfdConfigPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.api.port.integration.inTransaction
import io.github.texport.superkassa.core.domain.api.port.internal.TokenCodecPort
import io.github.texport.superkassa.core.domain.impl.helper.KkmCommonHelper
import io.github.texport.superkassa.core.domain.impl.helper.OfdResponseParser
import io.github.texport.superkassa.core.domain.impl.logging.getLogger

/**
 * Сценарий регистрации, инициализации и подключения ККМ к системе.
 */
class RegisterKkmUseCase(
    private val storage: StoragePort,
    private val ofdConfig: OfdConfigPort,
    private val tokenCodec: TokenCodecPort,
    private val idGenerator: IdGeneratorPort,
    private val clock: ClockPort,
    private val kkmCommonHelper: KkmCommonHelper,
    private val initializeKkmRegistrationUseCase: InitializeKkmRegistrationUseCase
) {
    private val logger = getLogger(RegisterKkmUseCase::class)
    private val registeredMode = KkmMode.REGISTRATION.name
    private val registeredState = KkmState.ACTIVE.name

    /**
     * Стандартная инициализация ККМ с явным указанием заводского номера, регистрационного номера КГД и ОФД-реквизитов.
     *
     * @param pin ПИН-код администратора для авторизации операции (должен быть "0000").
     * @param ofdId Идентификатор провайдера ОФД.
     * @param ofdEnvironment Окружение ОФД (например, PRODUCTION или TEST).
     * @param ofdSystemId Уникальный идентификатор ККМ в ОФД.
     * @param ofdToken Токен доступа ОФД.
     * @param kkmKgdId Регистрационный номер ККМ, выданный Комитетом государственных доходов (КГД).
     * @param factoryNumber Заводской номер устройства.
     * @param manufactureYear Год выпуска устройства.
     * @param serviceInfo Дополнительные метаданные сервиса (если null, используются дефолтные).
     * @param oked Код ОКЭД организации.
     * @param adminPin Пин администратора новой кассы; `null` — прежний стандартный.
     * @return Зарегистрированная информация о ККМ [KkmInfo].
     * @throws ValidationException Если не заполнен идентификатор системы ОФД или не валиден ОКЭД.
     * @throws ForbiddenException Если передан неверный ПИН-код администратора.
     * @throws ConflictException Если касса с таким регистрационным номером или системным ID уже существует.
     */
    fun initKkm(
        pin: String,
        ofdId: String,
        ofdEnvironment: String,
        ofdSystemId: String,
        ofdToken: String,
        kkmKgdId: String,
        factoryNumber: String,
        manufactureYear: Int,
        serviceInfo: OfdServiceInfo?,
        oked: String?,
        adminPin: String? = null
    ): KkmInfo {
        logger.info("initKkm: starting registration for systemId='$ofdSystemId', factoryNumber='$factoryNumber'")
        kkmCommonHelper.ensureSystemTimeValid()
        requireBootstrapAdminPin(pin)
        requireUsableAdminPin(adminPin)
        if (ofdSystemId.isBlank()) {
            throw ValidationException(CoreStrings.kkmSystemIdRequired(), "KKM_SYSTEM_ID_REQUIRED")
        }
        val now = clock.now()
        val ofdTag = validateOfd(ofdId, ofdEnvironment)

        // Проверяем уникальность регистрационного номера
        val existingByReg = storage.findKkmByRegistrationNumber(kkmKgdId)
        if (existingByReg != null) {
            throw ConflictException(CoreStrings.kkmExists(), "KKM_EXISTS")
        }

        // Проверяем уникальность ID кассы в ОФД
        val existingBySystem = storage.findKkmBySystemId(ofdSystemId)
        if (existingBySystem != null) {
            throw ConflictException(
                CoreStrings.kkmSystemIdExists(ofdSystemId),
                "KKM_SYSTEM_ID_EXISTS"
            )
        }

        val kkmId = idGenerator.nextId()
        val rawServiceInfo = serviceInfo ?: kkmCommonHelper.defaultServiceInfo()
        val finalServiceInfo = resolveAndValidateServiceInfo(rawServiceInfo, oked)

        val baseInfo = KkmInfo(
            id = kkmId,
            createdAt = now,
            updatedAt = now,
            mode = registeredMode,
            state = registeredState,
            ofdProvider = ofdTag,
            registrationNumber = kkmKgdId,
            factoryNumber = factoryNumber,
            manufactureYear = manufactureYear,
            systemId = ofdSystemId,
            ofdServiceInfo = finalServiceInfo
        )

        return initializeKkmRegistrationUseCase.execute(
            InitializeKkmRegistrationUseCase.KkmInitializationParams(
                baseInfo = baseInfo,
                ofdToken = ofdToken,
                registrationNumber = kkmKgdId,
                factoryNumber = factoryNumber,
                ofdTag = ofdTag,
                okedOverride = oked,
                adminPin = adminPin,
                updateKkm = { updatedKkm ->
                    val created = storage.createKkm(updatedKkm)
                    if (!created) {
                        throw ConflictException(CoreStrings.kkmExists(), "KKM_EXISTS")
                    }
                }
            )
        )
    }

    /**
     * Упрощенная инициализация ККМ с автоматическим получением регистрационных и технических данных из ОФД.
     *
     * @param pin ПИН-код администратора для авторизации операции (должен быть "0000").
     * @param ofdId Идентификатор провайдера ОФД.
     * @param ofdEnvironment Окружение ОФД.
     * @param ofdSystemId Уникальный идентификатор ККМ в ОФД.
     * @param ofdToken Начальный токен доступа ОФД.
     * @param defaultVatGroup Группа НДС по умолчанию.
     * @param oked Опциональный код ОКЭД.
     * @param adminPin Пин администратора новой кассы; `null` — прежний стандартный.
     * @return Зарегистрированная информация о ККМ [KkmInfo].
     * @throws ValidationException Если не заполнен идентификатор системы ОФД, не валиден ОКЭД
     * или команды ОФД завершились ошибкой.
     * @throws ForbiddenException Если передан неверный ПИН-код администратора.
     * @throws ConflictException Если касса с таким ОФД ID или регистрационным номером уже существует.
     */
    fun initKkmSimple(
        pin: String,
        ofdId: String,
        ofdEnvironment: String,
        ofdSystemId: String,
        ofdToken: String,
        defaultVatGroup: VatGroup,
        oked: String?,
        adminPin: String? = null
    ): KkmInfo {
        logger.info("initKkmSimple: start initialization for systemId='$ofdSystemId', ofdId='$ofdId'")
        kkmCommonHelper.ensureSystemTimeValid()
        requireBootstrapAdminPin(pin)
        requireUsableAdminPin(adminPin)
        if (ofdSystemId.isBlank()) {
            logger.warn("initKkmSimple: ofdSystemId is blank")
            throw ValidationException(CoreStrings.kkmSystemIdRequired(), "KKM_SYSTEM_ID_REQUIRED")
        }
        val now = clock.now()
        val ofdTag = validateOfd(ofdId, ofdEnvironment)

        val existingBySystem = storage.findKkmBySystemId(ofdSystemId)
        if (existingBySystem != null) {
            logger.warn("initKkmSimple: systemId '$ofdSystemId' already exists in DB")
            throw ConflictException(
                CoreStrings.kkmSystemIdExists(ofdSystemId),
                "KKM_SYSTEM_ID_EXISTS"
            )
        }

        val kkmId = idGenerator.nextId()
        val initialToken = tokenCodec.parseToken(ofdToken)
        logger.debug("initKkmSimple: generated kkmId='$kkmId', sending OFD SYSTEM command...")

        // Создаем временную ККМ для отправки запросов инициализации в ОФД
        val tempKkm = KkmInfo(
            id = kkmId,
            createdAt = now,
            updatedAt = now,
            mode = registeredMode,
            state = registeredState,
            ofdProvider = ofdTag,
            systemId = ofdSystemId,
            ofdServiceInfo = kkmCommonHelper.defaultServiceInfo()
        )

        val tempRegistrationNumber = "TEMP_REG_${kkmId.take(8)}"
        val tempFactoryNumber = "TEMP_FACTORY_${kkmId.take(8)}"
        val tempServiceInfo = kkmCommonHelper.defaultServiceInfo()

        // 1. Отправляем системную (SYSTEM) команду в ОФД для проверки подключения и обновления токена
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

        logger.debug("initKkmSimple: OFD SYSTEM result status='${systemResult.status}'")

        if (systemResult.status != OfdCommandStatus.OK) {
            logger.error(
                "initKkmSimple: OFD SYSTEM command failed for systemId='$ofdSystemId': " +
                    describeFailure(systemResult)
            )
            throw registrationRefused(systemResult)
        }

        // 2. Отправляем информационную (INFO) команду для получения актуальных регистрационных данных
        val infoToken = systemResult.responseToken ?: initialToken
        logger.debug("initKkmSimple: sending OFD INFO command...")
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

        logger.debug("initKkmSimple: OFD INFO result status='${infoResult.status}'")

        if (infoResult.status != OfdCommandStatus.OK) {
            logger.error(
                "initKkmSimple: OFD INFO command failed for systemId='$ofdSystemId': " +
                    describeFailure(infoResult)
            )
            throw registrationRefused(infoResult)
        }

        // Парсим ответ ОФД и извлекаем реквизиты сервиса
        val rawServiceInfo = OfdResponseParser.extractServiceInfo(
            infoResult.responseJson,
            kkmCommonHelper.defaultServiceInfo()
        )
        val resolvedServiceInfo = resolveAndValidateServiceInfo(rawServiceInfo, oked)

        val registrationNumber = OfdResponseParser.extractRegistrationNumber(infoResult.responseJson) ?: tempRegistrationNumber
        val factoryNumber = OfdResponseParser.extractFactoryNumber(infoResult.responseJson) ?: tempFactoryNumber

        // Проверяем, не занят ли полученный регистрационный номер другой кассой
        val existingByReg = storage.findKkmByRegistrationNumber(registrationNumber)
        if (existingByReg != null && existingByReg.id != kkmId) {
            throw ConflictException(CoreStrings.kkmExists(), "KKM_EXISTS")
        }

        val now2 = clock.now()
        val shiftNo = OfdResponseParser.extractShiftNumber(infoResult.responseJson)
        val finalToken = infoResult.responseToken ?: initialToken
        val finalTaxRegime = if (defaultVatGroup == VatGroup.NO_VAT) {
            TaxRegime.NO_VAT
        } else {
            TaxRegime.VAT_PAYER
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
            manufactureYear = clock.currentYear(),
            systemId = ofdSystemId,
            ofdServiceInfo = resolvedServiceInfo,
            tokenEncryptedBase64 = tokenCodec.encodeToken(finalToken),
            tokenUpdatedAt = now2,
            lastShiftNo = shiftNo,
            taxRegime = finalTaxRegime,
            defaultVatGroup = defaultVatGroup
        )

        // В рамках одной транзакции сохраняем ККМ, обновляем счётчики и заводим администратора
        storage.inTransaction {
            val created = storage.createKkm(finalKkm)
            if (!created) {
                throw ConflictException(CoreStrings.kkmExists(), "KKM_EXISTS")
            }
            initializeKkmRegistrationUseCase.updateCountersFromOfdInfo(finalKkm.id, infoResult.responseJson)
            initializeKkmRegistrationUseCase.ensureAdministrator(finalKkm.id, clock.now(), adminPin)
        }

        return finalKkm
    }

    /**
     * Проверяет, соответствует ли переданный ПИН-код коду начальной настройки администратора.
     *
     * @param pin ПИН-код для проверки.
     * @throws ForbiddenException Если ПИН-код не совпадает с кодом начальной настройки.
     */
    private fun requireBootstrapAdminPin(pin: String) {
        if (pin != StandardPin.BOOTSTRAP) {
            throw ForbiddenException(CoreStrings.userForbidden(), "USER_FORBIDDEN")
        }
    }

    /**
     * Проверяет, что пин администратора новой кассы позволит в неё войти.
     *
     * @param adminPin Пин администратора, заданный при заведении кассы.
     * @throws ValidationException Если пин стандартный: с ним касса останется недоступной.
     */
    private fun requireUsableAdminPin(adminPin: String?) {
        if (adminPin != null && StandardPin.isStandard(adminPin)) {
            throw ValidationException(CoreStrings.defaultPinNotAllowed(), "DEFAULT_PIN_NOT_ALLOWED")
        }
    }

    /**
     * Валидирует и форматирует тег провайдера ОФД.
     */
    private fun validateOfd(providerId: String, environmentId: String): String =
        ofdConfig.validateAndFormatTag(providerId, environmentId)

    /**
     * Разрешает и валидирует информацию о сервисе ОФД с учетом ОКЭД.
     */
    private fun resolveAndValidateServiceInfo(
        rawServiceInfo: OfdServiceInfo,
        oked: String?
    ): OfdServiceInfo {
        val resolved = if (oked != null) {
            rawServiceInfo.copy(orgOked = oked)
        } else {
            rawServiceInfo
        }
        if (resolved.orgOked.isBlank() || resolved.orgOked == "00000") {
            throw ValidationException(CoreStrings.okedRequired(), "OKED_REQUIRED")
        }
        return resolved
    }
}
