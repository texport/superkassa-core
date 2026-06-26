package kz.mybrain.superkassa.core.application.service

import kz.mybrain.superkassa.core.domain.model.OfdServiceInfo
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object OfdResponseParser {

    fun extractShiftNumber(responseJson: JsonObject?): Int? {
        val zxReport = extractZxReport(responseJson) ?: return null
        return zxReport.getNestedInt(listOf("shiftNumber"))
    }

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
            orgTitle = org?.getNestedString(listOf("title")) ?: fallback.orgTitle,
            orgAddress = org?.getNestedString(listOf("address"))
                ?: pos?.getNestedString(listOf("address"))
                ?: fallback.orgAddress,
            orgAddressKz = org?.getNestedString(listOf("addressKz"))
                ?: pos?.getNestedString(listOf("addressKz"))
                ?: fallback.orgAddressKz,
            orgInn = org?.getNestedString(listOf("inn")) ?: fallback.orgInn,
            orgOkved = org?.getNestedString(listOf("okved")) ?: fallback.orgOkved,
            geoLatitude = pos?.getNestedInt(listOf("latitude")) ?: fallback.geoLatitude,
            geoLongitude = pos?.getNestedInt(listOf("longitude")) ?: fallback.geoLongitude,
            geoSource = fallback.geoSource
        )
    }

    fun extractRegistrationNumber(responseJson: JsonObject?): String? {
        val regInfo = responseJson?.getNestedObject(listOf("payload", "service", "regInfo")) ?: return null

        val kkm = regInfo.getNestedObject(listOf("kkm"))
        val fnsKkmId = kkm?.getNestedString(listOf("fnsKkmId"))
        if (!fnsKkmId.isNullOrBlank()) {
            return fnsKkmId
        }

        val pos = regInfo.getNestedObject(listOf("pos"))
        return pos?.getNestedString(listOf("registrationNumber"))
            ?: pos?.getNestedString(listOf("regNumber"))
    }

    fun extractFactoryNumber(responseJson: JsonObject?): String? {
        val regInfo = responseJson?.getNestedObject(listOf("payload", "service", "regInfo")) ?: return null

        val kkm = regInfo.getNestedObject(listOf("kkm"))
        val serialNumber = kkm?.getNestedString(listOf("serialNumber"))
        if (!serialNumber.isNullOrBlank()) {
            return serialNumber
        }

        val pos = regInfo.getNestedObject(listOf("pos"))
        return pos?.getNestedString(listOf("factoryNumber"))
            ?: pos?.getNestedString(listOf("factoryNum"))
    }

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

    private fun JsonObject.getNestedString(path: List<String>): String? {
        var current: JsonObject? = this
        for (i in 0 until path.size - 1) {
            current = current?.get(path[i])?.jsonObject
            if (current == null) return null
        }
        return current?.get(path.last())?.jsonPrimitive?.contentOrNull
    }

    private fun JsonObject.getNestedInt(path: List<String>): Int? {
        var current: JsonObject? = this
        for (i in 0 until path.size - 1) {
            current = current?.get(path[i])?.jsonObject
            if (current == null) return null
        }
        return current?.get(path.last())?.jsonPrimitive?.intOrNull
    }

    private fun JsonObject.getNestedObject(path: List<String>): JsonObject? {
        var current: JsonObject? = this
        for (key in path) {
            current = current?.get(key)?.jsonObject
            if (current == null) return null
        }
        return current
    }
}
