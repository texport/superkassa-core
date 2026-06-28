package kz.mybrain.superkassa.core.domain.usecase.kkm

import kz.mybrain.superkassa.core.domain.exception.ConflictException
import kz.mybrain.superkassa.core.domain.exception.ErrorMessages
import kz.mybrain.superkassa.core.domain.exception.ForbiddenException
import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.model.common.TaxRegime
import kz.mybrain.superkassa.core.domain.model.common.VatGroup
import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kz.mybrain.superkassa.core.domain.model.kkm.KkmMode
import kz.mybrain.superkassa.core.domain.model.kkm.KkmState
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandStatus
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandType
import kz.mybrain.superkassa.core.domain.model.ofd.OfdServiceInfo
import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.IdGeneratorPort
import kz.mybrain.superkassa.core.domain.port.OfdConfigPort
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.port.TokenCodecPort
import kz.mybrain.superkassa.core.domain.helper.KkmCommonHelper
import kz.mybrain.superkassa.core.domain.helper.OfdResponseParser

/**
 * Сценарий регистрации, инициализации и подключения ККМ к системе.
 *
 * Поддерживает два варианта инициализации:
 * 1. Стандартная инициализация с ручным вводом всех реквизитов ([initKkm]).
 * 2. Упрощенная инициализация с автоматическим запросом параметров из ОФД ([initKkmSimple]).
 *
 * @property storage Порт для доступа к хранилищу данных.
 * @property ofdConfig Порт для работы с конфигурацией провайдеров ОФД.
 * @property tokenCodec Порт для шифрования и дешифрования токенов доступа ОФД.
 * @property idGenerator Порт для генерации уникальных идентификаторов.
 * @property clock Порт для работы с системным временем.
 * @property kkmCommonHelper Общий помощник для операций над ККМ.
 * @property initializeKkmRegistrationUseCase Вспомогательный сценарий для начальной инициализации состояния регистрации ККМ.
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
    private val registeredMode = KkmMode.REGISTRATION.name
    private val registeredState = KkmState.ACTIVE.name
    private val defaultAdminPin = "0000"

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
     * @param okved Код ОКВЭД организации.
     * @return Зарегистрированная информация о ККМ [KkmInfo].
     * @throws ValidationException Если не заполнен идентификатор системы ОФД или не валиден ОКВЭД.
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
        okved: String?
    ): KkmInfo {
        kkmCommonHelper.ensureSystemTimeValid()
        requireBootstrapAdminPin(pin)
        if (ofdSystemId.isBlank()) {
            throw ValidationException(ErrorMessages.kkmSystemIdRequired(), "KKM_SYSTEM_ID_REQUIRED")
        }
        val now = clock.now()
        val ofdTag = validateOfd(ofdId, ofdEnvironment)
        
        // Проверяем уникальность регистрационного номера
        val existingByReg = storage.findKkmByRegistrationNumber(kkmKgdId)
        if (existingByReg != null) {
            throw ConflictException(ErrorMessages.kkmExists(), "KKM_EXISTS")
        }
        
        // Проверяем уникальность ID кассы в ОФД
        val existingBySystem = storage.findKkmBySystemId(ofdSystemId)
        if (existingBySystem != null) {
            throw ConflictException(
                ErrorMessages.kkmSystemIdExists(ofdSystemId),
                "KKM_SYSTEM_ID_EXISTS"
            )
        }

        val kkmId = idGenerator.nextId()
        val rawServiceInfo = serviceInfo ?: kkmCommonHelper.defaultServiceInfo()
        val finalServiceInfo = resolveAndValidateServiceInfo(rawServiceInfo, okved)

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
                okvedOverride = okved,
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
     * Упрощенная инициализация ККМ с автоматическим получением регистрационных и технических данных из ОФД.
     *
     * @param pin ПИН-код администратора для авторизации операции (должен быть "0000").
     * @param ofdId Идентификатор провайдера ОФД.
     * @param ofdEnvironment Окружение ОФД.
     * @param ofdSystemId Уникальный идентификатор ККМ в ОФД.
     * @param ofdToken Начальный токен доступа ОФД.
     * @param defaultVatGroup Группа НДС по умолчанию.
     * @param okved Опциональный код ОКВЭД.
     * @return Зарегистрированная информация о ККМ [KkmInfo].
     * @throws ValidationException Если не заполнен идентификатор системы ОФД, не валиден ОКВЭД или команды ОФД завершились ошибкой.
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
        okved: String?
    ): KkmInfo {
        kkmCommonHelper.ensureSystemTimeValid()
        requireBootstrapAdminPin(pin)
        if (ofdSystemId.isBlank()) {
            throw ValidationException(ErrorMessages.kkmSystemIdRequired(), "KKM_SYSTEM_ID_REQUIRED")
        }
        val now = clock.now()
        val ofdTag = validateOfd(ofdId, ofdEnvironment)

        val existingBySystem = storage.findKkmBySystemId(ofdSystemId)
        if (existingBySystem != null) {
            throw ConflictException(
                ErrorMessages.kkmSystemIdExists(ofdSystemId),
                "KKM_SYSTEM_ID_EXISTS"
            )
        }

        val kkmId = idGenerator.nextId()
        val initialToken = tokenCodec.parseToken(ofdToken)

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

        if (systemResult.status != OfdCommandStatus.OK) {
            throw ValidationException(
                ErrorMessages.ofdRequestFailed(systemResult.errorMessage),
                "OFD_COMMAND_FAILED"
            )
        }

        // 2. Отправляем информационную (INFO) команду для получения актуальных регистрационных данных
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

        // Парсим ответ ОФД и извлекаем реквизиты сервиса
        val rawServiceInfo = OfdResponseParser.extractServiceInfo(
            infoResult.responseJson,
            kkmCommonHelper.defaultServiceInfo()
        )
        val resolvedServiceInfo = resolveAndValidateServiceInfo(rawServiceInfo, okved)

        val registrationNumber = OfdResponseParser.extractRegistrationNumber(infoResult.responseJson) ?: tempRegistrationNumber
        val factoryNumber = OfdResponseParser.extractFactoryNumber(infoResult.responseJson) ?: tempFactoryNumber

        // Проверяем, не занят ли полученный регистрационный номер другой кассой
        val existingByReg = storage.findKkmByRegistrationNumber(registrationNumber)
        if (existingByReg != null && existingByReg.id != kkmId) {
            throw ConflictException(ErrorMessages.kkmExists(), "KKM_EXISTS")
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

        // В рамках одной транзакции сохраняем ККМ, обновляем счетчики и создаем дефолтных пользователей (кассиров)
        storage.inTransaction {
            val created = storage.createKkm(finalKkm)
            if (!created) {
                throw ConflictException(ErrorMessages.kkmExists(), "KKM_EXISTS")
            }
            initializeKkmRegistrationUseCase.updateCountersFromOfdInfo(finalKkm.id, infoResult.responseJson)
            initializeKkmRegistrationUseCase.ensureDefaultUsers(finalKkm.id, clock.now())
        }

        return finalKkm
    }

    /**
     * Проверяет, соответствует ли переданный ПИН-код коду начальной настройки администратора.
     *
     * @param pin ПИН-код для проверки.
     * @throws ForbiddenException Если ПИН-код не совпадает со значением по умолчанию "0000".
     */
    private fun requireBootstrapAdminPin(pin: String) {
        if (pin != defaultAdminPin) {
            throw ForbiddenException(ErrorMessages.userForbidden(), "USER_FORBIDDEN")
        }
    }

    /**
     * Валидирует и форматирует тег провайдера ОФД.
     */
    private fun validateOfd(providerId: String, environmentId: String): String =
        ofdConfig.validateAndFormatTag(providerId, environmentId)

    /**
     * Разрешает и валидирует информацию о сервисе ОФД с учетом ОКВЭД.
     */
    private fun resolveAndValidateServiceInfo(
        rawServiceInfo: OfdServiceInfo,
        okved: String?
    ): OfdServiceInfo {
        val resolved = if (okved != null) {
            rawServiceInfo.copy(orgOkved = okved)
        } else {
            rawServiceInfo
        }
        if (resolved.orgOkved.isBlank() || resolved.orgOkved == "00000") {
            throw ValidationException(ErrorMessages.okvedRequired(), "OKVED_REQUIRED")
        }
        return resolved
    }
}
