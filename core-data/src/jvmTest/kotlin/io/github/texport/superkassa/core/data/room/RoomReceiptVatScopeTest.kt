package io.github.texport.superkassa.core.data.room

import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.ADMIN_PIN
import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.CASHIER_PIN
import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.KKM
import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.cash
import io.github.texport.superkassa.core.domain.api.exception.SuperkassaException
import io.github.texport.superkassa.core.domain.api.model.common.CounterScopes
import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.domain.api.model.common.TaxRegime
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptItemRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptSellRequest
import kz.kazakhtelecom.proto.v203.TicketRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * НДС в чеке задаётся одним способом: одной ставкой на весь чек или
 * ставками позиций. Налог на весь чек уходит в БФД налогом самого чека
 * (CPCR, `TicketRequest.taxes`) и одинаков на бумаге, в счётчиках смены
 * и у БФД; смешать способы в одном чеке нельзя.
 */
class RoomReceiptVatScopeTest {
    private val kassa = RoomKassa(TaxRegime.VAT_PAYER, VatGroup.VAT_16).also { it.api.openShift(KKM, ADMIN_PIN) }

    @Test
    fun `НДС на весь чек - налог итога 89,66, а не сумма налогов позиций 89,65`() {
        val id = sell(listOf(line("150.00"), line("250.00"), line("250.00")), "650.00", receiptVat = VatGroup.VAT_16)

        val ticket = kassa.bfd.lastTicket()
        assertEquals(mapOf(VAT_16_PERCENT to 8_966L), bfdTicketTax(ticket))
        assertTaxedAsWholeReceipt(ticket)
        assertEquals(mapOf(VatGroup.VAT_16 to 8_966L), paperTax(id))
        assertEquals(8_966L to 65_000L, shiftCounter("tax.VAT_16.OPERATION_SELL.sum") to shiftCounter("tax.VAT_16.OPERATION_SELL.turnover"))
    }

    @Test
    fun `НДС на весь чек со скидкой - налог с итога после скидки, у скидки налога нет`() {
        val id = sell(listOf(line("1000.00")), "900.00", receiptVat = VatGroup.VAT_16, discountPercent = "10")

        val ticket = kassa.bfd.lastTicket()
        assertEquals(mapOf(VAT_16_PERCENT to 12_414L), bfdTicketTax(ticket))
        assertTaxedAsWholeReceipt(ticket)
        assertEquals(mapOf(VatGroup.VAT_16 to 12_414L), paperTax(id))
        assertEquals(90_000L, shiftCounter("tax.VAT_16.OPERATION_SELL.turnover"))
    }

    @Test
    fun `НДС на весь чек своей ставкой - на бумаге ставка чека, у позиций ставок нет`() {
        val id = sell(listOf(line("105.00"), line("210.00")), "315.00", receiptVat = VatGroup.VAT_5)

        assertEquals(mapOf(5_000 to 1_500L), bfdTicketTax(kassa.bfd.lastTicket()))
        assertTaxedAsWholeReceipt(kassa.bfd.lastTicket())
        val form = kassa.api.getReceiptHtml(KKM, id, CASHIER_PIN)
        assertEquals(0, "class=\"item-vat\"".toRegex().findAll(form).count(), "no rate on the lines")
        assertTrue("15,00" in form, "tax of the receipt on the form")
        assertEquals(mapOf(VatGroup.VAT_5 to 1_500L), paperTax(id))
    }

    @Test
    fun `ставка на весь чек и ставка позиции в одном чеке - отказ на трёх языках, чек не пробит`() {
        val lines = listOf(line("100.00"), line("50.00", vat = VatGroup.NO_VAT))

        val refusal = assertFailsWith<SuperkassaException> { sell(lines, "150.00", receiptVat = VatGroup.VAT_16) }

        assertEquals("RECEIPT_VAT_SCOPES_CONFLICT", refusal.code)
        with(refusal.trilingualMessage) { assertTrue("НДС" in ru && "ҚҚС" in kk && "VAT" in en, toString()) }
        assertTrue(kassa.bfd.requests.none { it.ticket != null }, "nothing reached the BFD")
        assertTrue(kassa.shiftDocuments().none { it.docType == "CHECK" }, "no receipt in the shift")
    }

    @Test
    fun `возврат суммой по чеку с НДС на весь чек - тоже налогом чека по ставке основания`() {
        val sale = sell(listOf(line("105.00"), line("210.00")), "315.00", receiptVat = VatGroup.VAT_5)

        kassa.refundByAmount(sale, "315.00", "210.00")

        val refund = kassa.bfd.lastTicket()
        assertEquals(mapOf(5_000 to 1_000L), bfdTicketTax(refund))
        assertTaxedAsWholeReceipt(refund)
        assertEquals(1_000L, shiftCounter("tax.VAT_5.OPERATION_SELL_RETURN.sum"))
    }

    private fun sell(
        items: List<ReceiptItemRequest>,
        total: String,
        receiptVat: VatGroup? = null,
        discountPercent: String? = null
    ): String = kassa.api.createSellReceipt(
        KKM, CASHIER_PIN,
        ReceiptSellRequest(
            idempotencyKey = "sale-$total-${items.size}", items = items, payments = listOf(cash(total)),
            vatGroup = receiptVat?.name, discountPercent = discountPercent?.let { Decimal.parse(it) }
        )
    ).documentId

    /** Налог стоит у самого чека, а у позиций, скидки и наценки его нет. */
    private fun assertTaxedAsWholeReceipt(ticket: TicketRequest) {
        assertTrue(ticket.taxes.isNotEmpty(), "the receipt carries the tax")
        assertFalse(taxesMixed(ticket), "items, discount and markup carry no taxes")
    }

    /** Налог чека, по которому печатается лента: ставка → налог в тиынах. */
    private fun paperTax(documentId: String): Map<VatGroup, Long> =
        checkNotNull(kassa.storage.findFiscalDocumentWithReceiptPayload(documentId)).second.ticketTaxes.orEmpty()
            .associate { it.vatGroup to it.taxSum.tiyn() }

    private fun shiftCounter(key: String): Long? {
        val shift = checkNotNull(kassa.storage.findOpenShift(KKM))
        return kassa.storage.loadCounters(KKM, CounterScopes.SHIFT, shift.id)[key]
    }
}
