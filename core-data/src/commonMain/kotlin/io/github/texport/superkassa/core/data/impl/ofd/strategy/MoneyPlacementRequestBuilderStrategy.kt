package io.github.texport.superkassa.core.data.impl.ofd.strategy

import io.github.texport.superkassa.core.data.impl.ofd.OfdConfig
import io.github.texport.superkassa.core.data.impl.ofd.OfdRequestFactory
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandRequest
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import kotlinx.serialization.json.JsonObject

/**
 * Стратегия построения запросов для COMMAND_MONEY_PLACEMENT (внесение/изъятие наличных средств).
 *
 * Извлекает фискальный документ типа CASH_IN или CASH_OUT из хранилища и формирует
 * JSON-запрос о внесении или изъятии наличных денег в кассе для передачи в ОФД.
 */
// Регистрируется и используется динамически через список стратегий сборщика запросов / DI
class MoneyPlacementRequestBuilderStrategy(
    private val storage: StoragePort
) : OfdRequestBuilderStrategy {

    /**
     * Проверяет, может ли стратегия обработать указанный тип команды [commandType].
     *
     * @param commandType тип команды ОФД.
     * @return `true`, если тип команды [OfdCommandType.MONEY_PLACEMENT], иначе `false`.
     */
    override fun canHandle(commandType: OfdCommandType): Boolean {
        return commandType == OfdCommandType.MONEY_PLACEMENT
    }

    /**
     * Строит JSON-запрос для операции внесения или изъятия денег на основе параметров команды и конфигурации ОФД.
     *
     * @param command запрос команды ОФД [OfdCommandRequest].
     * @param config настройки протокола ОФД [OfdConfig].
     * @return JSON-объект [JsonObject] запроса внесения/изъятия денег или `null`, если тип документа не поддерживается.
     */
    override fun build(command: OfdCommandRequest, config: OfdConfig): JsonObject? {
        val serviceBlock = buildServiceBlock(command) ?: return null
        val doc = storage.findFiscalDocumentById(command.payloadRef) ?: return null
        if (doc.docType != "CASH_IN" && doc.docType != "CASH_OUT") return null
        val amountBills = doc.totalAmount ?: 0L
        val ofdId = command.ofdProviderId.lowercase()
        return OfdRequestFactory.buildMoneyPlacementRequest(
            ofdId = ofdId,
            protocolVersion = config.protocolVersion,
            deviceId = command.deviceId,
            token = command.token,
            reqNum = command.reqNum,
            docType = doc.docType,
            amountBills = amountBills,
            createdAtMillis = doc.createdAt,
            serviceBlock = serviceBlock
        )
    }
}
