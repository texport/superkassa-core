package kz.mybrain.superkassa.core.domain.helper.ofd

import kz.mybrain.superkassa.core.domain.exception.ErrorMessages
import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandRequest
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandType
import kz.mybrain.superkassa.core.domain.model.ofd.OfdServiceInfo
import kz.mybrain.superkassa.core.domain.port.OfdConfigPort

/**
 * Фабрика для конструирования объектов [OfdCommandRequest], отправляемых в ОФД.
 *
 * Сценарий конструирования учитывает параметры ККМ, тип выполняемой команды ОФД,
 * а также возможные переопределения (офлайн-режим, тестовые данные).
 *
 * @property ofdConfig Порт для разбора и валидации конфигурации провайдеров ОФД.
 */
class OfdCommandRequestFactory(
    private val ofdConfig: OfdConfigPort
) {
    /**
     * Создает структурированный запрос к ОФД.
     *
     * Выполняет валидацию провайдера ОФД, идентификаторов ККМ (systemId, deviceId),
     * а также проверяет обязательность сервисных полей (регистрационный и серийный номера)
     * для команд, работающих непосредственно через ККМ.
     *
     * @param kkm Информация о ККМ, для которой создается запрос.
     * @param commandType Тип отправляемой команды ОФД.
     * @param payloadRef Ссылка-идентификатор полезной нагрузки (UUID или локальный идентификатор).
     * @param token Токен ОФД для авторизации запроса.
     * @param reqNum Номер запроса (последовательный счетчик).
     * @param now Текущее системное время в миллисекундах.
     * @param serviceInfoOverride Необязательное переопределение информации об услугах ОФД.
     * @param registrationNumberOverride Необязательное переопределение регистрационного номера ККМ.
     * @param factoryNumberOverride Необязательное переопределение заводского (серийного) номера ККМ.
     * @param ofdProviderOverride Необязательное переопределение провайдера ОФД.
     * @param defaultServiceInfo Лямбда-функция, возвращающая дефолтную сервисную информацию, если она не задана.
     * @return Полностью сформированный объект запроса [OfdCommandRequest].
     * @throws ValidationException если отсутствует или некорректен провайдер ОФД, системный идентификатор ККМ
     * или обязательные для кассовых команд регистрационный/заводской номера.
     */
    fun build(
        kkm: KkmInfo,
        commandType: OfdCommandType,
        payloadRef: String,
        token: Long,
        reqNum: Int,
        now: Long,
        serviceInfoOverride: OfdServiceInfo? = null,
        registrationNumberOverride: String? = null,
        factoryNumberOverride: String? = null,
        ofdProviderOverride: String? = null,
        defaultServiceInfo: () -> OfdServiceInfo
    ): OfdCommandRequest {
        val ofdTag = ofdProviderOverride
            ?: kkm.ofdProvider
            ?: throw ValidationException(ErrorMessages.ofdProviderRequired(), "OFD_PROVIDER_REQUIRED")

        val (providerId, environmentId) = ofdConfig.parseTag(ofdTag)
        ofdConfig.validateAndFormatTag(providerId, environmentId)

        val systemId = kkm.systemId
            ?: throw ValidationException(ErrorMessages.kkmSystemIdRequired(), "KKM_SYSTEM_ID_REQUIRED")
        val deviceId = systemId.toLongOrNull()
            ?: throw ValidationException(ErrorMessages.kkmSystemIdInvalid(systemId), "KKM_SYSTEM_ID_INVALID")

        // payload.service обязателен для всех команд от кассы (протокол ОФД)
        val requiresService = commandType in setOf(
            OfdCommandType.SYSTEM,
            OfdCommandType.INFO,
            OfdCommandType.TICKET,
            OfdCommandType.MONEY_PLACEMENT,
            OfdCommandType.REPORT,
            OfdCommandType.CLOSE_SHIFT
        )
        val serviceInfo = if (requiresService) {
            serviceInfoOverride ?: kkm.ofdServiceInfo ?: defaultServiceInfo()
        } else {
            null
        }
        val registrationNumber = if (requiresService) {
            registrationNumberOverride ?: kkm.registrationNumber
                ?: throw ValidationException(ErrorMessages.kkmRegistrationRequired(), "KKM_REG_REQUIRED")
        } else {
            registrationNumberOverride ?: kkm.registrationNumber
        }
        val factoryNumber = if (requiresService) {
            factoryNumberOverride ?: kkm.factoryNumber
                ?: throw ValidationException(ErrorMessages.kkmFactoryRequired(), "KKM_FACTORY_REQUIRED")
        } else {
            factoryNumberOverride ?: kkm.factoryNumber
        }

        // Первая попытка онлайн: begin = end = now; при офлайн-повторе end обновит worker
        val (offlineBegin, offlineEnd) = if (requiresService) {
            now to now
        } else {
            (now - 60_000) to now
        }
        return OfdCommandRequest(
            kkmId = kkm.id,
            commandType = commandType,
            payloadRef = payloadRef,
            ofdProviderId = providerId,
            ofdEnvironmentId = environmentId,
            deviceId = deviceId,
            token = token,
            reqNum = reqNum,
            registrationNumber = registrationNumber,
            factoryNumber = factoryNumber,
            ofdSystemId = systemId,
            serviceInfo = serviceInfo,
            offlineBeginMillis = offlineBegin,
            offlineEndMillis = offlineEnd
        )
    }
}
