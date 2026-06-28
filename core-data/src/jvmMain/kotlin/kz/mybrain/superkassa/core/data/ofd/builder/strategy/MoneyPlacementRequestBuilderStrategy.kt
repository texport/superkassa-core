package kz.mybrain.superkassa.core.data.ofd.builder.strategy

import kotlinx.serialization.json.JsonObject
import kz.mybrain.superkassa.core.data.ofd.OfdConfig
import kz.mybrain.superkassa.core.data.ofd.OfdRequestFactory
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandRequest
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandType
import kz.mybrain.superkassa.core.domain.port.StoragePort

/**
 * Стратегия построения запросов для COMMAND_MONEY_PLACEMENT (внесение/изъятие наличных средств).
 *
 * Извлекает фискальный документ типа CASH_IN или CASH_OUT из хранилища и формирует
 * JSON-запрос о внесении или изъятии наличных денег в кассе для передачи в ОФД.
 */
// Регистрируется и используется динамически через список стратегий сборщика запросов / DI
@Suppress("unused", "DuplicatedCode")
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

    /**
     * Формирует служебный JSON-блок (payload) с регистрационной информацией и геолокацией.
     *
     * @param command запрос команды ОФД.
     * @return JSON-объект [JsonObject] со служебной информацией или `null`, если параметры неполны.
     */
    private fun buildServiceBlock(command: OfdCommandRequest): JsonObject? {
        val serviceInfo = command.serviceInfo ?: return null
        val regNo = command.registrationNumber ?: return null
        val factoryNo = command.factoryNumber ?: return null
        val systemId = command.ofdSystemId ?: return null
        val begin = command.offlineBeginMillis ?: System.currentTimeMillis()
        val end = command.offlineEndMillis ?: System.currentTimeMillis()
        return OfdRequestFactory.buildServicePayload(
            serviceInfo = serviceInfo,
            registrationNumber = regNo,
            factoryNumber = factoryNo,
            systemId = systemId,
            offlineBeginMillis = begin,
            offlineEndMillis = end
        )
    }
}
