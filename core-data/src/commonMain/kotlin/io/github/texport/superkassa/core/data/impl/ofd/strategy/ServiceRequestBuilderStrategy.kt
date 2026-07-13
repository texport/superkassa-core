package io.github.texport.superkassa.core.data.impl.ofd.strategy

import io.github.texport.superkassa.core.data.impl.ofd.OfdConfig
import io.github.texport.superkassa.core.data.impl.ofd.OfdRequestFactory
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandRequest
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import kotlinx.serialization.json.JsonObject

/**
 * Стратегия построения служебных запросов для команд типа SYSTEM и INFO.
 *
 * Формирует служебные пакеты данных, передающие регистрационную информацию,
 * серийные номера, оффлайн-периоды и информацию о версии ККМ в ОФД.
 */
// Регистрируется и используется динамически через список стратегий сборщика запросов / DI
class ServiceRequestBuilderStrategy : OfdRequestBuilderStrategy {

    /**
     * Проверяет, поддерживает ли стратегия указанный тип команды [commandType].
     *
     * @param commandType тип команды ОФД.
     * @return `true`, если тип команды [OfdCommandType.SYSTEM] или [OfdCommandType.INFO], иначе `false`.
     */
    override fun canHandle(commandType: OfdCommandType): Boolean {
        return commandType == OfdCommandType.SYSTEM || commandType == OfdCommandType.INFO
    }

    /**
     * Строит JSON-запрос служебного характера на основе переданной команды и конфигурации ОФД.
     *
     * @param command запрос команды ОФД [OfdCommandRequest].
     * @param config настройки протокола ОФД [OfdConfig].
     * @return JSON-объект [JsonObject] служебного запроса или `null`, если отсутствуют обязательные параметры ККМ.
     */
    override fun build(command: OfdCommandRequest, config: OfdConfig): JsonObject? {
        val serviceInfo = command.serviceInfo ?: return null
        val registrationNumber = command.registrationNumber ?: return null
        val factoryNumber = command.factoryNumber ?: return null
        val systemId = command.ofdSystemId ?: return null
        val offlineBegin = command.offlineBeginMillis ?: kotlin.time.Clock.System.now().toEpochMilliseconds()
        val offlineEnd = command.offlineEndMillis ?: kotlin.time.Clock.System.now().toEpochMilliseconds()
        val ofdId = command.ofdProviderId.lowercase()

        return OfdRequestFactory.buildServiceRequest(
            ofdId = ofdId,
            protocolVersion = config.protocolVersion,
            commandType = command.commandType.value,
            deviceId = command.deviceId,
            token = command.token,
            reqNum = command.reqNum,
            offlineBeginMillis = offlineBegin,
            offlineEndMillis = offlineEnd,
            registrationNumber = registrationNumber,
            factoryNumber = factoryNumber,
            systemId = systemId,
            serviceInfo = serviceInfo
        )
    }
}
