package io.github.texport.superkassa.core.domain.impl.helper

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.domain.api.model.receipt.TicketAd
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdServiceInfo
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdNomenclatureLookupResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdNomenclatureItem
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus

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
     * Извлекает номер фискального документа (чека/отчета) из ответа ОФД.
     */
    fun extractDocNumber(responseJson: JsonObject?): Long? {
        val payload = responseJson?.getNestedObject(listOf("payload"))
        val ticket = payload?.getNestedObject(listOf("ticket"))
        val report = payload?.getNestedObject(listOf("report"))
        val command = payload?.getNestedObject(listOf("command"))
        // Номер документа приходит числом не всегда: в схеме ОФД ticket_number
        // объявлен строкой. Читать только как число значит терять его молча.
        return numberAt(ticket, "ticketNumber")
            ?: numberAt(ticket, "docNumber")
            ?: numberAt(report, "reportNumber")
            ?: numberAt(report, "docNumber")
            ?: numberAt(command, "docNumber")
            ?: numberAt(payload, "documentNumber")
            ?: numberAt(payload, "docNumber")
    }

    /** Читает число по ключу, принимая и числовое, и строковое представление. */
    private fun numberAt(source: JsonObject?, key: String): Long? =
        source?.getNestedLong(key) ?: source?.getNestedString(key)?.trim()?.toLongOrNull()

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
            orgAddress = pos?.getNestedString("address")
                ?: org?.getNestedString("address")
                ?: fallback.orgAddress,
            orgAddressKz = pos?.getNestedString("addressKz")
                ?: org?.getNestedString("addressKz")
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

    private fun JsonObject.getNestedLong(key: String): Long? {
        return this[key]?.jsonPrimitive?.longOrNull
    }

    private fun JsonObject.getNestedObject(path: List<String>): JsonObject? {
        var current: JsonObject? = this
        for (key in path) {
            current = current?.get(key)?.jsonObject ?: return null
        }
        return current
    }

    /**
     * Парсит JSON-ответ ОФД для команды COMMAND_NOMENCLATURE в доменный результат.
     */
    fun parseNomenclature(
        responseJson: JsonObject?,
        commandStatus: OfdCommandStatus,
        defaultResultCode: Int?,
        defaultError: String?
    ): OfdNomenclatureLookupResult {
        if (commandStatus != OfdCommandStatus.OK || responseJson == null) {
            return OfdNomenclatureLookupResult(
                found = false,
                item = null,
                resultCode = defaultResultCode ?: -1,
                resultText = defaultError ?: "BFD command execution failed"
            )
        }

        val payload = responseJson["payload"] as? JsonObject
            ?: return OfdNomenclatureLookupResult(
                found = false,
                item = null,
                resultCode = defaultResultCode ?: -1,
                resultText = "Missing payload envelope in BFD response"
            )

        val nomenclatureObj = payload["nomenclature"] as? JsonObject
            ?: return OfdNomenclatureLookupResult(
                found = false,
                item = null,
                resultCode = defaultResultCode ?: -1,
                resultText = "Missing nomenclature payload in BFD response"
            )

        val resultCodeVal = nomenclatureObj["result"]?.jsonObject?.get("code")?.jsonPrimitive?.intOrNull ?: 0
        if (resultCodeVal != 0) {
            val resultName = nomenclatureObj["result"]?.jsonObject?.get("name")?.jsonPrimitive?.contentOrNull ?: "UNKNOWN"
            return OfdNomenclatureLookupResult(
                found = false,
                item = null,
                resultCode = resultCodeVal,
                resultText = resultName
            )
        }

        val elements = nomenclatureObj["elements"] as? kotlinx.serialization.json.JsonArray
        val element = elements?.firstOrNull() as? JsonObject
            ?: return OfdNomenclatureLookupResult(
                found = false,
                item = null,
                resultCode = 0,
                resultText = "No items found in nomenclature response"
            )

        val itemObj = element["item"] as? JsonObject
            ?: return OfdNomenclatureLookupResult(
                found = false,
                item = null,
                resultCode = 0,
                resultText = "Element type is group or item payload is missing"
            )

        val title = element["title"]?.jsonPrimitive?.content ?: ""
        val titleKk = element["titleKk"]?.jsonPrimitive?.contentOrNull
        val ntin = itemObj["ntin"]?.jsonPrimitive?.contentOrNull
        val idVal = element["id"]?.jsonPrimitive?.longOrNull ?: 0L

        // Price mapping: цена приходит парой bills/coins и остаётся точной.
        val sellPriceObj = itemObj["sellPrice"] as? JsonObject
        val priceVal = if (sellPriceObj != null) {
            val bills = sellPriceObj["bills"]?.jsonPrimitive?.longOrNull ?: 0L
            val coins = sellPriceObj["coins"]?.jsonPrimitive?.intOrNull ?: 0
            Decimal.ofScaled(bills * TIYN_IN_TENGE + coins, TIYN_SCALE)
        } else {
            Decimal.ZERO
        }

        val measureUnitCode = itemObj["measureUnitCode"]?.jsonPrimitive?.contentOrNull

        // Tax mapping
        val taxes = itemObj["taxes"] as? kotlinx.serialization.json.JsonArray
        val firstTax = taxes?.firstOrNull() as? JsonObject
        val vatGroupVal = if (firstTax != null) {
            val taxType = firstTax["taxType"]?.jsonPrimitive?.contentOrNull
            if (taxType == "VAT") {
                val taxPercent = firstTax["taxPercent"]?.jsonPrimitive?.intOrNull ?: 0
                if (taxPercent == 16000 || taxPercent == 16) "VAT_16" else if (taxPercent == 0) "VAT_0" else "NO_VAT"
            } else {
                "NO_VAT"
            }
        } else {
            null
        }

        val barcodeVal = itemObj["barcode"]?.jsonPrimitive?.contentOrNull ?: element["barcode"]?.jsonPrimitive?.contentOrNull ?: ""

        return OfdNomenclatureLookupResult(
            found = true,
            item = OfdNomenclatureItem(
                id = idVal,
                barcode = barcodeVal,
                name = title,
                nameKk = titleKk,
                ntin = ntin,
                price = priceVal,
                measureUnitCode = measureUnitCode,
                vatGroup = vatGroupVal
            ),
            resultCode = 0,
            resultText = "OK"
        )
    }

    /**
     * Извлекает рекламные тексты ОФД (ticket ads) для печати на чеке.
     *
     * @param responseJson JSON-объект ответа от ОФД.
     * @return Список рекламных текстов.
     */
    fun extractTicketAds(responseJson: JsonObject?): List<TicketAd> {
        val payload = responseJson?.getNestedObject(listOf("payload")) ?: return emptyList()
        val service = payload.getNestedObject(listOf("service")) ?: return emptyList()
        val ticketAdsArray = service["ticketAds"]?.jsonArray ?: return emptyList()
        return ticketAdsArray.mapNotNull { element ->
            val ad = element.jsonObject
            val text = ad["text"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val info = ad["info"]?.jsonObject
            TicketAd(
                // Вид и версия нужны, чтобы в следующем запросе сказать ОФД,
                // что у кассы уже есть: без них он присылал бы одно и то же
                // на каждый чек либо не присылал вовсе.
                type = info?.get("type")?.jsonPrimitive?.content ?: DEFAULT_AD_TYPE,
                version = info?.get("version")?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
                text = text
            )
        }
    }

    /** Вид объявления, если ОФД его не назвал: реклама самого оператора. */
    private const val DEFAULT_AD_TYPE = "TICKET_AD_OFD"
}

/** Знаков после запятой у тенге. */
private const val TIYN_SCALE: Int = 2

/** Тиынов в тенге. */
private const val TIYN_IN_TENGE: Long = 100
