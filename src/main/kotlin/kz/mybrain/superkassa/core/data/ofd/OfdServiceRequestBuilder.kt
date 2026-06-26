package kz.mybrain.superkassa.core.data.ofd

import kz.mybrain.superkassa.core.domain.model.OfdServiceInfo
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object OfdServiceRequestBuilder {

    fun buildServicePayload(
        serviceInfo: OfdServiceInfo,
        registrationNumber: String,
        factoryNumber: String,
        systemId: String,
        offlineBeginMillis: Long,
        offlineEndMillis: Long
    ): JsonObject {
        val begin = OfdCommonRequestHelper.toDateTime(offlineBeginMillis)
        val end = OfdCommonRequestHelper.toDateTime(offlineEndMillis)
        return buildJsonObject {
            put("getRegInfo", JsonPrimitive(true))
            put(
                "offlinePeriod",
                buildJsonObject {
                    put("beginTime", begin)
                    put("endTime", end)
                }
            )
            put(
                "securityStats",
                buildJsonObject {
                    put(
                        "geoPosition",
                        buildJsonObject {
                            put("latitude", JsonPrimitive(serviceInfo.geoLatitude))
                            put("longitude", JsonPrimitive(serviceInfo.geoLongitude))
                            put("source", JsonPrimitive(serviceInfo.geoSource))
                        }
                    )
                }
            )
            put(
                "regInfo",
                buildJsonObject {
                    put(
                        "kkm",
                        buildJsonObject {
                            put("fnsKkmId", JsonPrimitive(registrationNumber))
                            put("serialNumber", JsonPrimitive(factoryNumber))
                            put("kkmId", JsonPrimitive(systemId))
                        }
                    )
                    put(
                        "org",
                        buildJsonObject {
                            put("title", JsonPrimitive(serviceInfo.orgTitle))
                            put("address", JsonPrimitive(serviceInfo.orgAddress))
                            put("addressKz", JsonPrimitive(serviceInfo.orgAddressKz))
                            put("inn", JsonPrimitive(serviceInfo.orgInn))
                            put("okved", JsonPrimitive(serviceInfo.orgOkved))
                        }
                    )
                }
            )
        }
    }

    fun buildServiceRequest(
        ofdId: String,
        protocolVersion: String,
        commandType: String,
        deviceId: Long,
        token: Long,
        reqNum: Int,
        offlineBeginMillis: Long,
        offlineEndMillis: Long,
        registrationNumber: String,
        factoryNumber: String,
        systemId: String,
        serviceInfo: OfdServiceInfo
    ): JsonObject {
        val servicePayload = buildServicePayload(
            serviceInfo,
            registrationNumber,
            factoryNumber,
            systemId,
            offlineBeginMillis,
            offlineEndMillis
        )
        return buildJsonObject {
            put("ofdId", JsonPrimitive(ofdId))
            put("protocolVersion", JsonPrimitive(protocolVersion))
            put("messageType", JsonPrimitive("REQUEST"))
            put("commandType", JsonPrimitive(commandType))
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
                buildJsonObject { put("service", servicePayload) }
            )
        }
    }
}
