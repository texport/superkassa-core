package io.github.texport.superkassa.core.domain.impl.usecase.print.protocol

import io.github.texport.superkassa.core.domain.api.model.common.VatGroup
import io.github.texport.superkassa.core.domain.api.model.receipt.PaymentType
import io.github.texport.superkassa.core.domain.api.model.common.TaxRegime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Чего нет в чеке пакета: необязательные поля чека читаются пустотой, а не падением. */
class ProtocolReceiptDefaultsTest {

    private val kkm = ProtocolPackets.localKkm()

    private fun documentOf(body: String): ProtocolDocument? {
        val packet = requireNotNull(ProtocolPacket.of(body))
        return documentOf(packet, kkm.withProtocolRegistration(packet.service))
    }

    @Test
    fun `чек без единого необязательного поля собирается пустым`() {
        val document = documentOf("""{"request": {"command": "COMMAND_TICKET", "ticket": {}}}""")
            as ProtocolDocument.Receipt

        assertTrue(document.receipt.items.isEmpty())
        assertTrue(document.receipt.payments.isEmpty())
        assertEquals(0L, document.receipt.total.tiyn())
        assertNull(document.receipt.taken)
        assertNull(document.receipt.change)
        assertNull(document.receipt.discount)
        assertNull(document.receipt.parentTicket)
        assertNull(document.receipt.customerBin)
        assertNull(document.receipt.operatorName)
        assertNull(document.receipt.defaultVatGroup)
        assertEquals(TaxRegime.NO_VAT, document.receipt.taxRegime)
        assertEquals("", document.receipt.idempotencyKey)
        assertEquals("", document.document.id)
        assertNull(document.document.docNo)
        assertNull(document.document.shiftNo)
        assertEquals(0L, document.document.createdAt)
    }

    @Test
    fun `налог на весь чек печатается ставкой чека с оборотом итога`() {
        val document = documentOf(
            """
            {"request": {"command": "COMMAND_TICKET", "ticket": {
              "amounts": {"total": {"bills": "650", "coins": 0}},
              "items": [{"type": "ITEM_TYPE_COMMODITY", "commodity": {"sum": {"bills": "650", "coins": 0}}}],
              "taxes": [{"taxType": 100, "percent": 16000, "sum": {"bills": "89", "coins": 66}, "isInTotalSum": true}]
            }}}
            """.trimIndent()
        ) as ProtocolDocument.Receipt

        val tax = document.receipt.ticketTaxes.orEmpty().single()
        assertEquals(VatGroup.VAT_16, document.receipt.vatGroup)
        assertEquals(8_966L to 56_034L, tax.taxSum.tiyn() to tax.taxBase.tiyn())
        assertNull(document.receipt.items.single().vatGroup)
    }

    @Test
    fun `итоги без сумм и позиция без полей не мешают чеку`() {
        val document = documentOf(
            """
            {"request": {"command": "COMMAND_TICKET", "ticket": {
              "amounts": {},
              "items": [
                {"type": "ITEM_TYPE_COMMODITY", "commodity": {}},
                {"type": "ITEM_TYPE_MARKUP", "markup": {"sum": {"bills": "5", "coins": 0}}}
              ],
              "payments": [{}]
            }}}
            """.trimIndent()
        ) as ProtocolDocument.Receipt
        val item = document.receipt.items.single()

        assertEquals("", item.name)
        assertEquals("", item.sectionCode)
        assertEquals(0L, item.quantity)
        assertEquals(0L, item.price.tiyn())
        assertNull(item.vatGroup)
        assertNull(item.listExciseStamp)
        assertFalse(item.isStorno)
        assertEquals(PaymentType.CASH, document.receipt.payments.single().type)
        assertEquals(0L, document.receipt.payments.single().sum.tiyn())
        assertEquals(500L, document.receipt.markup?.tiyn())
    }

    @Test
    fun `скидка и наценка из итогов чека берутся прежде состава`() {
        val document = documentOf(
            """
            {"request": {"command": "COMMAND_TICKET", "ticket": {
              "amounts": {"discount": {"sum": ${ProtocolPackets.money(3)}},
                          "markup": {"sum": ${ProtocolPackets.money(4)}}},
              "items": [{"type": "ITEM_TYPE_DISCOUNT", "discount": {"sum": ${ProtocolPackets.money(9)}}}]
            }}}
            """.trimIndent()
        ) as ProtocolDocument.Receipt

        assertEquals(300L, document.receipt.discount?.tiyn())
        assertEquals(400L, document.receipt.markup?.tiyn())
    }

    @Test
    fun `все виды оплаты протокола узнаются`() {
        val kinds = mapOf(
            "PAYMENT_CASH" to PaymentType.CASH,
            "PAYMENT_CARD" to PaymentType.CARD,
            "PAYMENT_MOBILE" to PaymentType.MOBILE,
            "PAYMENT_ELECTRONIC" to PaymentType.ELECTRONIC,
            "PAYMENT_CREDIT" to PaymentType.CREDIT,
            "PAYMENT_TARE" to PaymentType.TARE
        )
        val payments = kinds.keys.joinToString(",") { """{"type": "$it"}""" }
        val document = documentOf(
            """{"request": {"command": "COMMAND_TICKET", "ticket": {"payments": [$payments]}}}"""
        ) as ProtocolDocument.Receipt

        assertEquals(kinds.values.toList(), document.receipt.payments.map { it.type })
    }

    @Test
    fun `исходный чек возврата читается и пустым, и под именем поля кассы`() {
        val bare = documentOf(
            """{"request": {"command": "COMMAND_TICKET", "ticket": {"parentTicket": {}}}}"""
        ) as ProtocolDocument.Receipt
        val kassaName = documentOf(
            """
            {"request": {"command": "COMMAND_TICKET", "ticket": {"parentTicket": {
              "parentTicketNumber": "77", "kgdKkmId": "260940000021", "parentTicketIsOffline": true,
              "parentTicketDateTime": ${ProtocolPackets.moment()}, "parentTicketTotal": ${ProtocolPackets.money(10)}
            }}}}
            """.trimIndent()
        ) as ProtocolDocument.Receipt

        assertEquals(0L, bare.receipt.parentTicket?.parentTicketNumber)
        assertEquals(0L, bare.receipt.parentTicket?.parentTicketDateTimeMillis)
        assertEquals("", bare.receipt.parentTicket?.kgdKkmId)
        assertEquals(0L, bare.receipt.parentTicket?.parentTicketTotal?.tiyn())
        assertFalse(bare.receipt.parentTicket?.parentTicketIsOffline ?: true)
        val schemaName = documentOf(
            """
            {"request": {"command": "COMMAND_TICKET", "ticket": {"parentTicket": {
              "parentTicketDataTime": ${ProtocolPackets.moment()}
            }}}}
            """.trimIndent()
        ) as ProtocolDocument.Receipt

        assertEquals(1_789_725_187_000L, schemaName.receipt.parentTicket?.parentTicketDateTimeMillis)
        assertEquals(77L, kassaName.receipt.parentTicket?.parentTicketNumber)
        assertEquals(1_789_725_187_000L, kassaName.receipt.parentTicket?.parentTicketDateTimeMillis)
        assertTrue(kassaName.receipt.parentTicket?.parentTicketIsOffline ?: false)
    }

    @Test
    fun `сторно уменьшает и оборот, и налог своей ставки`() {
        val rate = """"taxes": [{"percent": 16000, "sum": ${ProtocolPackets.money(16)}}]"""
        val document = documentOf(
            """
            {"request": {"command": "COMMAND_TICKET", "ticket": {"items": [
              {"type": "ITEM_TYPE_COMMODITY", "commodity": {"sum": ${ProtocolPackets.money(116)}, $rate}},
              {"type": "ITEM_TYPE_STORNO_COMMODITY", "stornoCommodity": {"sum": ${ProtocolPackets.money(116)}, $rate}},
              {"type": "ITEM_TYPE_COMMODITY", "commodity": {"sum": ${ProtocolPackets.money(10)},
                "taxes": [{"percent": 9999}]}}
            ]}}}
            """.trimIndent()
        ) as ProtocolDocument.Receipt

        assertEquals(VatGroup.VAT_16, document.receipt.ticketTaxes?.single()?.vatGroup)
        assertEquals(0L, document.receipt.ticketTaxes?.single()?.taxSum?.tiyn())
        assertEquals(0L, document.receipt.ticketTaxes?.single()?.taxBase?.tiyn())
    }

    @Test
    fun `ссылка на чек берётся, только когда она ссылка`() {
        val plain = documentOf(
            """
            {"request": {"command": "COMMAND_TICKET", "ticket": {}},
             "response": {"ticket": {"qrCodeBase64": "https://consumer.oofd.kz/r/1"}}}
            """.trimIndent()
        ) as ProtocolDocument.Receipt
        val garbage = documentOf(
            """
            {"request": {"command": "COMMAND_TICKET", "ticket": {}},
             "response": {"ticket": {"qrCode": "0J/RgNC40LLQtdGCLCDQvNC40YA="}}}
            """.trimIndent()
        ) as ProtocolDocument.Receipt

        val silent = documentOf(
            """
            {"request": {"command": "COMMAND_TICKET", "ticket": {}},
             "response": {"ticket": {"ticketNumber": "1"}}}
            """.trimIndent()
        ) as ProtocolDocument.Receipt

        assertEquals("https://consumer.oofd.kz/r/1", plain.document.receiptUrl)
        assertNull(garbage.document.receiptUrl)
        assertNull(silent.document.receiptUrl)
    }

    @Test
    fun `автономный чек опознаётся своим номером`() {
        val document = documentOf(
            """{"request": {"command": "COMMAND_TICKET", "ticket": {"offlineTicketNumber": 42}}}"""
        ) as ProtocolDocument.Receipt

        assertTrue(document.document.isAutonomous)
        assertEquals("42", document.document.autonomousSign)
    }
}
