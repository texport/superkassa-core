package kz.mybrain.superkassa.core.domain.helper

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kz.mybrain.superkassa.core.domain.model.ofd.OfdServiceInfo

/**
 * Объект-парсер для извлечения различных бизнес-данных из JSON-ответов ОФД-серверов.
 *
 * Позволяет извлекать информацию о смене, сервисных данных организации, регистрационных
 * и заводских номерах ККМ, а также извлекать сырой блок Zx-отчета.
 */
object OfdResponseParser {

    /**
     * Извлекает номер смены из ответа ОФД.
     *
     * @param responseJson JSON-объект ответа от ОФД.
     * @return Номер смены или `null`, если его не удалось извлечь.
     */
    fun extractShiftNumber(responseJson: JsonObject?): Int? {
        val zxReport = extractZxReport(responseJson) ?: return null
        return zxReport.getNestedInt("shiftNumber")
    }

    /**
     * Извлекает информацию об организации и точке продаж (сервисную информацию) из ответа ОФД.
     *
     * Если какие-то поля отсутствуют в ответе, используются значения из переданного [fallback].
     *
     * @param responseJson JSON-объект ответа от ОФД.
     * @param fallback Объект [OfdServiceInfo] со значениями по умолчанию.
     * @return Заполненный объект [OfdServiceInfo].
     */
    fun extractServiceInfo(
        responseJson: JsonObject?,
        fallback: OfdServiceInfo
    ): OfdServiceInfo {
        val payload = responseJson?.getNestedObject(listOf("payload")) ?: return fallback
        val service = payload.getNestedObject(listOf("service")) ?: return fallback
        val regInfo = service.getNestedObject(listOf("regInfo")) ?: return fallback
        val org = regInfo.getNestedObject(listOf("org"))
        val pos = regInfo.getNestedObject(listOf("pos"))

        return OfdServiceInfo(
            orgTitle = org?.getNestedString("title") ?: fallback.orgTitle,
            orgAddress = org?.getNestedString("address")
                ?: pos?.getNestedString("address")
                ?: fallback.orgAddress,
            orgAddressKz = org?.getNestedString("addressKz")
                ?: pos?.getNestedString("addressKz")
                ?: fallback.orgAddressKz,
            orgInn = org?.getNestedString("inn") ?: fallback.orgInn,
            orgOkved = org?.getNestedString("okved") ?: fallback.orgOkved,
            geoLatitude = pos?.getNestedInt("latitude") ?: fallback.geoLatitude,
            geoLongitude = pos?.getNestedInt("longitude") ?: fallback.geoLongitude,
            geoSource = fallback.geoSource
        )
    }

    /**
     * Извлекает регистрационный номер ККМ из ответа ОФД.
     *
     * Пытается найти номер `fnsKkmId` в блоке `kkm`, при его отсутствии ищет `registrationNumber`
     * или `regNumber` в блоке `pos`.
     *
     * @param responseJson JSON-объект ответа от ОФД.
     * @return Регистрационный номер ККМ или `null`.
     */
    fun extractRegistrationNumber(responseJson: JsonObject?): String? {
        val regInfo = responseJson?.getNestedObject(listOf("payload", "service", "regInfo")) ?: return null

        val kkm = regInfo.getNestedObject(listOf("kkm"))
        val fnsKkmId = kkm?.getNestedString("fnsKkmId")
        if (!fnsKkmId.isNullOrBlank()) {
            return fnsKkmId
        }

        val pos = regInfo.getNestedObject(listOf("pos"))
        return pos?.getNestedString("registrationNumber")
            ?: pos?.getNestedString("regNumber")
    }

    /**
     * Извлекает заводской (серийный) номер ККМ из ответа ОФД.
     *
     * Пытается найти `serialNumber` в блоке `kkm`, при его отсутствии ищет `factoryNumber`
     * или `factoryNum` в блоке `pos`.
     *
     * @param responseJson JSON-объект ответа от ОФД.
     * @return Заводской номер ККМ или `null`.
     */
    fun extractFactoryNumber(responseJson: JsonObject?): String? {
        val regInfo = responseJson?.getNestedObject(listOf("payload", "service", "regInfo")) ?: return null

        val kkm = regInfo.getNestedObject(listOf("kkm"))
        val serialNumber = kkm?.getNestedString("serialNumber")
        if (!serialNumber.isNullOrBlank()) {
            return serialNumber
        }

        val pos = regInfo.getNestedObject(listOf("pos"))
        return pos?.getNestedString("factoryNumber")
            ?: pos?.getNestedString("factoryNum")
    }

    /**
     * Извлекает JSON-объект Zx-отчета (или последнего Z-отчета) из ответа ОФД.
     *
     * @param responseJson JSON-объект ответа от ОФД.
     * @return JSON-объект с данными Zx-отчета или `null`.
     */
    fun extractZxReport(responseJson: JsonObject?): JsonObject? {
        val payload = responseJson?.getNestedObject(listOf("payload")) ?: return null
        val report = payload.getNestedObject(listOf("report"))
        val zxReport = report?.getNestedObject(listOf("zxReport"))
        if (zxReport != null) return zxReport
        val service = payload.getNestedObject(listOf("service")) ?: return null
        val lastZReport = service.getNestedObject(listOf("lastZReport"))
        return lastZReport?.getNestedObject(listOf("zxReport"))
            ?: lastZReport
            ?: service.getNestedObject(listOf("zxReport"))
    }

    private fun JsonObject.getNestedString(key: String): String? {
        return this[key]?.jsonPrimitive?.contentOrNull
    }

    private fun JsonObject.getNestedInt(key: String): Int? {
        return this[key]?.jsonPrimitive?.intOrNull
    }

    private fun JsonObject.getNestedObject(path: List<String>): JsonObject? {
        var current: JsonObject? = this
        for (key in path) {
            current = current?.get(key)?.jsonObject ?: return null
        }
        return current
    }
}
