package io.github.texport.superkassa.core.data.impl.ofd.builder

import io.github.texport.superkassa.core.domain.api.model.ofd.OfdServiceInfo
import io.github.texport.superkassa.core.domain.api.model.receipt.TicketAd
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class OfdServiceRequestBuilderAdsTest {

    @Test
    fun `касса сообщает виды и версии объявлений, которые у неё уже есть`() {
        // ОФД присылает только то, что новее присланных версий. Пока касса
        // молчала о своих, сравнивать было не с чем — и реклама не доходила.
        val payload = OfdServiceRequestBuilder.buildServicePayload(
            serviceInfo = serviceInfo,
            registrationNumber = "KGD-2000302",
            factoryNumber = "KZT26E2C509A200",
            systemId = "2000302",
            offlineBeginMillis = MOMENT,
            offlineEndMillis = MOMENT,
            knownTicketAds = listOf(TicketAd("TICKET_AD_OFD", 17L, "Проверьте чек"))
        )

        // Строка на каждый вид: спецификация требует полного списка,
        // и ноль в нём означает «этого вида у кассы нет».
        val infos = payload["ticketAdInfos"]!!.jsonArray
        assertEquals(AD_TYPES, infos.size)
        val known = infos.first { it.jsonObject["type"]!!.jsonPrimitive.content == "TICKET_AD_OFD" }
        assertEquals("17", known.jsonObject["version"]!!.jsonPrimitive.content)
        val unknown = infos.first { it.jsonObject["type"]!!.jsonPrimitive.content == "TICKET_AD_ORG" }
        assertEquals("0", unknown.jsonObject["version"]!!.jsonPrimitive.content)
    }

    @Test
    fun `без объявлений уходят нули по каждому виду`() {
        val payload = OfdServiceRequestBuilder.buildServicePayload(
            serviceInfo = serviceInfo,
            registrationNumber = "KGD-2000302",
            factoryNumber = "KZT26E2C509A200",
            systemId = "2000302",
            offlineBeginMillis = MOMENT,
            offlineEndMillis = MOMENT
        )

        val infos = payload["ticketAdInfos"]!!.jsonArray
        assertEquals(AD_TYPES, infos.size)
        assertTrue(infos.all { it.jsonObject["version"]!!.jsonPrimitive.content == "0" })
    }

    private val serviceInfo = OfdServiceInfo(
        orgTitle = "ТОО «Сарыарқа Сауда»",
        orgAddress = "Алматы, пр. Абая, 150",
        orgAddressKz = "Алматы қаласы, Абай даңғылы, 150",
        orgIinOrBin = "123456789012",
        orgOked = "47111",
        geoLatitude = 1,
        geoLongitude = 1,
        geoSource = "TEST"
    )

    private companion object {
        const val MOMENT = 1_788_000_000_000L

        /** Видов рекламных текстов в CPCR, п. 4.11. */
        const val AD_TYPES = 5
    }
}
