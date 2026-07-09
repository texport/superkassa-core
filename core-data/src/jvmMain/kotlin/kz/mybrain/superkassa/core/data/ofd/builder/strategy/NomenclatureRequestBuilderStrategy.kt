package kz.mybrain.superkassa.core.data.ofd.builder.strategy

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kz.mybrain.superkassa.core.data.ofd.OfdConfig
import kz.mybrain.superkassa.core.data.ofd.builder.OfdServiceRequestBuilder
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandRequest
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandType

/**
 * Стратегия построения запроса номенклатуры для команды типа NOMENCLATURE.
 */
@Suppress("DuplicatedCode")
class NomenclatureRequestBuilderStrategy : OfdRequestBuilderStrategy {

    override fun canHandle(commandType: OfdCommandType): Boolean {
        return commandType == OfdCommandType.NOMENCLATURE
    }

    override fun build(command: OfdCommandRequest, config: OfdConfig): JsonObject? {
        val serviceInfo = command.serviceInfo ?: return null
        val registrationNumber = command.registrationNumber ?: return null
        val factoryNumber = command.factoryNumber ?: return null
        val systemId = command.ofdSystemId ?: return null
        val offlineBegin = command.offlineBeginMillis ?: System.currentTimeMillis()
        val offlineEnd = command.offlineEndMillis ?: System.currentTimeMillis()
        val ofdId = command.ofdProviderId.lowercase()

        val servicePayload = OfdServiceRequestBuilder.buildServicePayload(
            serviceInfo,
            registrationNumber,
            factoryNumber,
            systemId,
            offlineBegin,
            offlineEnd
        )

        val barcode = command.payloadRef

        return buildJsonObject {
            put("ofdId", JsonPrimitive(ofdId))
            put("protocolVersion", JsonPrimitive(config.protocolVersion))
            put("messageType", JsonPrimitive("REQUEST"))
            put("commandType", JsonPrimitive(command.commandType.value))
            put(
                "header",
                buildJsonObject {
                    put("deviceId", JsonPrimitive(command.deviceId))
                    put("token", JsonPrimitive(command.token))
                    put("reqNum", JsonPrimitive(command.reqNum))
                }
            )
            put(
                "payload",
                buildJsonObject {
                    put("service", servicePayload)
                    put(
                        "nomenclature",
                        buildJsonObject {
                            put("currentVersion", JsonPrimitive(1))
                            put("barcode", JsonPrimitive(barcode))
                        }
                    )
                }
            )
        }
    }
}
