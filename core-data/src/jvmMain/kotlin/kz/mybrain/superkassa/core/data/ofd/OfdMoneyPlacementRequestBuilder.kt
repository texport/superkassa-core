package kz.mybrain.superkassa.core.data.ofd

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kz.mybrain.superkassa.core.data.ofd.builder.OfdCommonRequestHelper

/**
 * Построитель запросов операций внесения и изъятия наличных средств (Money Placement) для ККМ в формате ОФД.
 */
object OfdMoneyPlacementRequestBuilder {

    /**
     * Строит JSON-объект запроса `COMMAND_MONEY_PLACEMENT` (внесение/изъятие наличных).
     *
     * @param ofdId Идентификатор ОФД.
     * @param protocolVersion Версия протокола ОФД.
     * @param deviceId Идентификатор устройства в системе ОФД.
     * @param token Сессионный токен авторизации устройства.
     * @param reqNum Порядковый номер запроса ККМ.
     * @param docType Тип документа операции ("CASH_IN" для внесения, "CASH_OUT" для изъятия).
     * @param amountBills Сумма операции в тиынах/копейках.
     * @param createdAtMillis Время создания документа в миллисекундах.
     * @param serviceBlock Сервисный блок метаданных ККМ.
     * @return JSON-запрос для отправки в ОФД.
     */
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
