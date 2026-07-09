package io.github.texport.superkassa.core.data.ofd.strategy

import io.github.texport.superkassa.core.data.ofd.OfdConfig
import io.github.texport.superkassa.core.data.ofd.builder.OfdServiceRequestBuilder
import io.github.texport.superkassa.core.domain.model.ofd.OfdCommandRequest
import io.github.texport.superkassa.core.domain.model.ofd.OfdCommandType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Стратегия построения запроса номенклатуры для команды типа NOMENCLATURE.
 */
class NomenclatureRequestBuilderStrategy : OfdRequestBuilderStrategy {

    override fun canHandle(commandType: OfdCommandType): Boolean {
        return commandType == OfdCommandType.NOMENCLATURE
    }

    override fun build(command: OfdCommandRequest, config: OfdConfig): JsonObject? {
        val serviceInfo = command.serviceInfo ?: return null
        val registrationNumber = command.registrationNumber ?: return null
        val factoryNumber = command.factoryNumber ?: return null
        val systemId = command.ofdSystemId ?: return null
        val offlineBegin = command.offlineBeginMillis ?: kotlin.time.Clock.System.now().toEpochMilliseconds()
        val offlineEnd = command.offlineEndMillis ?: kotlin.time.Clock.System.now().toEpochMilliseconds()
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
