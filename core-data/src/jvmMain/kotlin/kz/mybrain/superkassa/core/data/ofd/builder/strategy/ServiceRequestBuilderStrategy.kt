package kz.mybrain.superkassa.core.data.ofd.builder.strategy

import kotlinx.serialization.json.JsonObject
import kz.mybrain.superkassa.core.data.ofd.OfdConfig
import kz.mybrain.superkassa.core.data.ofd.OfdRequestFactory
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandRequest
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandType

/**
 * Стратегия построения служебных запросов для команд типа SYSTEM и INFO.
 *
 * Формирует служебные пакеты данных, передающие регистрационную информацию,
 * серийные номера, оффлайн-периоды и информацию о версии ККМ в ОФД.
 */
// Регистрируется и используется динамически через список стратегий сборщика запросов / DI
@Suppress("unused", "DuplicatedCode")
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
        val offlineBegin = command.offlineBeginMillis ?: System.currentTimeMillis()
        val offlineEnd = command.offlineEndMillis ?: System.currentTimeMillis()
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
