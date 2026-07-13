package io.github.texport.superkassa.core.data.impl.ofd.strategy

import io.github.texport.superkassa.core.data.impl.ofd.OfdConfig
import io.github.texport.superkassa.core.data.impl.ofd.OfdRequestFactory
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandRequest
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import kotlinx.serialization.json.JsonObject

/**
 * Стратегия построения JSON-запроса к ОФД для конкретного типа команды.
 *
 * Позволяет расширять список поддерживаемых команд ОФД без изменения существующего
 * клиентского кода в соответствии с принципом открытости/закрытости (OCP).
 */
interface OfdRequestBuilderStrategy {
    /**
     * Определяет, поддерживает ли данная стратегия обработку указанного типа команды [commandType].
     *
     * @param commandType тип команды ОФД [OfdCommandType].
     * @return `true`, если стратегия может обработать этот тип команды, иначе `false`.
     */
    fun canHandle(commandType: OfdCommandType): Boolean

    /**
     * Строит JSON-запрос для выполнения переданной команды [command] на основе конфигурации [config].
     *
     * @param command запрос команды ОФД [OfdCommandRequest], содержащий параметры выполнения.
     * @param config настройки подключения и параметры ККМ [OfdConfig].
     * @return JSON-объект [JsonObject] запроса или `null`, если построить запрос не удалось.
     */
    fun build(command: OfdCommandRequest, config: OfdConfig): JsonObject?
}

/**
 * Вспомогательный метод построения служебного блока, общий для всех стратегий.
 */
fun OfdRequestBuilderStrategy.buildServiceBlock(command: OfdCommandRequest): JsonObject? {
    val serviceInfo = command.serviceInfo ?: return null
    val regNo = command.registrationNumber ?: return null
    val factoryNo = command.factoryNumber ?: return null
    val systemId = command.ofdSystemId ?: return null
    val begin = command.offlineBeginMillis ?: kotlin.time.Clock.System.now().toEpochMilliseconds()
    val end = command.offlineEndMillis ?: kotlin.time.Clock.System.now().toEpochMilliseconds()
    return OfdRequestFactory.buildServicePayload(
        serviceInfo = serviceInfo,
        registrationNumber = regNo,
        factoryNumber = factoryNo,
        systemId = systemId,
        offlineBeginMillis = begin,
        offlineEndMillis = end
    )
}
