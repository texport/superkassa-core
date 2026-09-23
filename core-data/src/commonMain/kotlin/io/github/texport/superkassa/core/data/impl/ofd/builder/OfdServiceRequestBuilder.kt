package io.github.texport.superkassa.core.data.impl.ofd.builder

import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import io.github.texport.superkassa.core.domain.api.model.receipt.TicketAd
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdServiceInfo
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Строитель запросов для отправки служебных команд в ОФД.
 *
 * Формирует структуру JSON для отправки регистрационной информации,
 * геопозиции, а также периодов работы ККМ в автономном (оффлайн) режиме.
 */
object OfdServiceRequestBuilder {

    /**
     * Формирует полезную нагрузку (payload) для служебного запроса.
     *
     * Нагрузка включает в себя информацию об оффлайн-периоде ККМ, данных геолокации,
     * а также регистрационных данных организации и самой кассы.
     *
     * @param serviceInfo служебная информация об организации и ККМ [OfdServiceInfo].
     * @param registrationNumber регистрационный номер ККМ в налоговом органе.
     * @param factoryNumber заводской (серийный) номер устройства ККМ.
     * @param systemId системный уникальный идентификатор ККМ в системе.
     * @param offlineBeginMillis время начала автономной работы ККМ (в миллисекундах epoch).
     * @param offlineEndMillis время окончания автономной работы ККМ (в миллисекундах epoch).
     * @return JSON-объект [JsonObject] с полезной нагрузкой "service".
     */
    fun buildServicePayload(
        serviceInfo: OfdServiceInfo,
        registrationNumber: String,
        factoryNumber: String,
        systemId: String,
        offlineBeginMillis: Long,
        offlineEndMillis: Long,
        knownTicketAds: List<TicketAd> = emptyList()
    ): JsonObject {
        val begin = OfdCommonRequestHelper.toDateTime(offlineBeginMillis)
        val end = OfdCommonRequestHelper.toDateTime(offlineEndMillis)
        return buildJsonObject {
            put("getRegInfo", JsonPrimitive(true))
            // Версии объявлений, которые есть у кассы, — по строке на каждый
            // вид, как требует спецификация («n == TicketAdTypeEnum::size»).
            // Ноль означает, что объявления этого вида у кассы нет: без него
            // ОФД нечего сравнивать и первое объявление не приходит никогда.
            put(
                "ticketAdInfos",
                buildJsonArray {
                    TICKET_AD_TYPES.forEach { type ->
                        add(
                            buildJsonObject {
                                put("type", JsonPrimitive(type))
                                put(
                                    "version",
                                    JsonPrimitive(knownTicketAds.firstOrNull { it.type == type }?.version ?: 0L)
                                )
                            }
                        )
                    }
                }
            )
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
                            // Имена полей — из схемы протокола 2.0.3 кодека:
                            // там БИН/ИИН и ОКЭД называются inn и okved.
                            put("inn", JsonPrimitive(serviceInfo.orgIinOrBin))
                            put("okved", JsonPrimitive(serviceInfo.orgOked))
                        }
                    )
                }
            )
        }
    }

    /**
     * Формирует полный JSON-запрос служебного типа для отправки в ОФД.
     *
     * Запрос состоит из метаданных (ID ОФД, версия протокола, тип сообщения, тип команды),
     * заголовка (ID устройства, токен авторизации, порядковый номер запроса) и полезной нагрузки.
     *
     * @param ofdId идентификатор ОФД.
     * @param protocolVersion версия протокола взаимодействия с ОФД.
     * @param commandType тип отправляемой команды.
     * @param deviceId уникальный идентификатор устройства ККМ в ОФД.
     * @param token токен сессии/авторизации устройства.
     * @param reqNum порядковый номер отправляемого запроса.
     * @param offlineBeginMillis время начала автономной работы ККМ.
     * @param offlineEndMillis время окончания автономной работы ККМ.
     * @param registrationNumber регистрационный номер ККМ в налоговом органе.
     * @param factoryNumber заводской номер ККМ.
     * @param systemId системный идентификатор ККМ.
     * @param serviceInfo служебные метаданные организации и устройства [OfdServiceInfo].
     * @return Полный JSON-объект [JsonObject] служебного запроса.
     */
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

/** Виды рекламных текстов чека по CPCR, п. 4.11. */
private val TICKET_AD_TYPES = listOf(
    "TICKET_AD_OFD",
    "TICKET_AD_ORG",
    "TICKET_AD_POS",
    "TICKET_AD_KKM",
    "TICKET_AD_INFO"
)
