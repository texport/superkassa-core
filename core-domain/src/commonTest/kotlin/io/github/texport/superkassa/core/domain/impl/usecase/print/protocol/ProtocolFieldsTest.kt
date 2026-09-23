package io.github.texport.superkassa.core.domain.impl.usecase.print.protocol

import io.github.texport.superkassa.core.domain.api.model.ofd.OfdServiceInfo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Чего в пакете нет.
 *
 * Необязательное поле протокол не пишет вовсе, и у документа с чужой
 * кассы не бывает половины того, что есть у своего. Каждое такое место
 * обязано отвечать пустотой, а не падением: на экране владельца падение
 * разбора выглядит как сломанный кабинет.
 */
class ProtocolFieldsTest {

    private val kkm = ProtocolPackets.localKkm()

    private fun read(body: String): JsonObject = Json.parseToJsonElement(body) as JsonObject

    @Test
    fun `пустые поля читаются пустотой, а не падением`() {
        val empty = read("""{"nothing": null, "text": "", "wrong": [1, "два", {"a": 1}]}""")

        assertNull(empty.child("nothing"))
        assertTrue(empty.children("nothing").isEmpty())
        assertEquals(listOf("1", "два"), empty.texts("wrong"))
        assertEquals(1, empty.children("wrong").size)
        assertNull(empty.text("text"))
        assertNull(empty.text("nothing"))
        assertNull(empty.number("text"))
        assertFalse(empty.flag("nothing"))
        assertNull(empty.money("nothing"))
        assertEquals(0L, empty.tiyn("nothing"))
    }

    @Test
    fun `сумма без частей и признак строкой читаются как есть`() {
        val values = read("""{"sum": {}, "yes": "true", "no": false}""")

        assertEquals(0L, values.tiyn("sum"))
        assertTrue(values.flag("yes"))
        assertFalse(values.flag("no"))
    }

    @Test
    fun `момент без части даты не собирается`() {
        val moments = read(
            """
            {
              "whole": {"date": {"year": 2026, "month": 9, "day": 18}, "time": {"hour": 1}},
              "noDate": {"time": {"hour": 1}},
              "noYear": {"date": {"month": 9, "day": 18}},
              "noMonth": {"date": {"year": 2026, "day": 18}},
              "noDay": {"date": {"year": 2026, "month": 9}}
            }
            """.trimIndent()
        )

        assertEquals(1_789_675_200_000L, moments.moment("whole"))
        assertNull(moments.moment("missing"))
        assertNull(moments.moment("noDate"))
        assertNull(moments.moment("noYear"))
        assertNull(moments.moment("noMonth"))
        assertNull(moments.moment("noDay"))
        assertEquals(1_789_671_600_000L, read("""{"dayOnly": {"date": {"year": 2026, "month": 9, "day": 18}}}""").moment("dayOnly"))
    }

    @Test
    fun `реквизиты берутся по частям, а известное о кассе не теряется`() {
        val known = kkm.copy(
            ofdServiceInfo = OfdServiceInfo(
                orgTitle = "ТОО Прежнее", orgAddress = "Астана", orgAddressKz = "Астана қ.",
                orgInn = "000000000000", orgOkved = "00000",
                geoLatitude = 43, geoLongitude = 76, geoSource = "GPS"
            )
        )
        val bare = known.withProtocolRegistration(read("""{"regInfo": {"org": {}}}"""))

        assertEquals(kkm.id, bare.id)
        assertEquals(kkm.registrationNumber, bare.registrationNumber)
        assertEquals(kkm.factoryNumber, bare.factoryNumber)
        assertEquals("ТОО Прежнее", bare.ofdServiceInfo?.orgTitle)
        assertEquals(43, bare.ofdServiceInfo?.geoLatitude)
        assertEquals(76, bare.ofdServiceInfo?.geoLongitude)
        assertEquals("GPS", bare.ofdServiceInfo?.geoSource)
    }

    @Test
    fun `реквизиты без сведений о кассе остаются пустыми строками`() {
        val bare = kkm.withProtocolRegistration(read("""{"regInfo": {"org": {}}}"""))

        assertEquals("", bare.ofdServiceInfo?.orgTitle)
        assertEquals("", bare.ofdServiceInfo?.orgAddressKz)
        assertEquals("", bare.ofdServiceInfo?.orgOkved)
        assertEquals("", bare.ofdServiceInfo?.geoSource)
        assertEquals(kkm.copy(ofdServiceInfo = bare.ofdServiceInfo), bare)
    }

    @Test
    fun `служебный блок без реквизитов и без налогоплательщика ничего не выдумывает`() {
        val onlyDevice = kkm.withProtocolRegistration(read("""{"regInfo": {"kkm": {"kkmId": "77"}}}"""))

        assertEquals(kkm, kkm.withProtocolRegistration(read("""{"other": 1}""")))
        assertEquals("77", onlyDevice.id)
        assertNull(onlyDevice.ofdServiceInfo)
    }

    @Test
    fun `пакет без запроса и не объектом вовсе не читается`() {
        assertNull(ProtocolPacket.of("[1, 2]"))
        assertNull(ProtocolPacket.of("{"))
        assertEquals("", requireNotNull(ProtocolPacket.of("""{"request": {}}""")).command)
    }
}
