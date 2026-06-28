package kz.mybrain.superkassa.core.data.ofd.builder.strategy

import kotlinx.serialization.json.JsonObject
import kz.mybrain.superkassa.core.data.ofd.OfdConfig
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandRequest
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandType

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
