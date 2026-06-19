package kz.mybrain.superkassa.core.data.ofd

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object OfdMoneyPlacementRequestBuilder {

    fun buildMoneyPlacementRequest(
        ofdId: String,
        protocolVersion: String,
        deviceId: Long,
        token: Long,
        reqNum: Int,
        docType: String,
        amountBills: Long,
        createdAtMillis: Long,
        serviceBlock: JsonObject
    ): JsonObject {
        val operation = when (docType) {
            "CASH_IN" -> "MONEY_PLACEMENT_DEPOSIT"
            "CASH_OUT" -> "MONEY_PLACEMENT_WITHDRAWAL"
            else -> "MONEY_PLACEMENT_DEPOSIT"
        }
        return buildJsonObject {
            put("ofdId", JsonPrimitive(ofdId))
            put("protocolVersion", JsonPrimitive(protocolVersion))
            put("messageType", JsonPrimitive("REQUEST"))
            put("commandType", JsonPrimitive("COMMAND_MONEY_PLACEMENT"))
            put(
                "header",
                buildJsonObject {
                    put("deviceId", JsonPrimitive(deviceId))
                    put("token", JsonPrimitive(token))
                    put("reqNum", JsonPrimitive(reqNum))
                }
            )
            put(
                "payload",
                buildJsonObject {
                    put("service", serviceBlock)
                    put(
                        "moneyPlacement",
                        buildJsonObject {
                            put("dateTime", OfdCommonRequestHelper.toDateTime(createdAtMillis))
                            put("operation", JsonPrimitive(operation))
                            put("sum", OfdCommonRequestHelper.moneyObject(amountBills, 0))
                            put(
                                "operator",
                                buildJsonObject {
                                    put("code", JsonPrimitive(1))
                                    put("name", JsonPrimitive("Оператор"))
                                }
                            )
                        }
                    )
                }
            )
        }
    }
}
