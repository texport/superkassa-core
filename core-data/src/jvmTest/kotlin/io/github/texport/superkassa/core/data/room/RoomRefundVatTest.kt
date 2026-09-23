package io.github.texport.superkassa.core.data.room

import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.ADMIN_PIN
import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.CASHIER_PIN
import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.KGD_NUMBER
import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.KKM
import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.cash
import io.github.texport.superkassa.core.domain.api.exception.SuperkassaException
import io.github.texport.superkassa.core.domain.api.model.common.CounterScopes
import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.domain.api.model.common.TaxRegime
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup
import io.github.texport.superkassa.core.presentation.api.model.receipt.ParentTicketRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptItemRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptSellRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptSellReturnRequest
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Возврат суммой облагается ставкой чека-основания, а строка весового
 * товара считается одним правилом: к ближайшему тиыну.
 */
class RoomRefundVatTest {
    private val kassa = RoomKassa(TaxRegime.MIXED, VatGroup.VAT_16).also { it.api.openShift(KKM, ADMIN_PIN) }

    @Test
    fun `возврат суммой продажи без НДС уходит без налога, а не по ставке кассы`() {
        val sale = sell(listOf(line("1000.00", vat = VatGroup.NO_VAT)), "1000.00")

        val refund = refundByAmount(sale, "1000.00", "1000.00")

        assertEquals(emptyMap(), bfdTicketTax(kassa.bfd.lastTicket()))
        assertEquals(emptyList(), stored(refund).ticketTaxes.orEmpty())
        assertNull(shiftCounter("tax.VAT_16.OPERATION_SELL_RETURN.sum"))
    }

    @Test
    fun `возврат суммой по чеку с двумя ставками делится между ними в пропорции основания`() {
        val sale = sell(listOf(line("1160.00", vat = VatGroup.VAT_16), line("1000.00", vat = VatGroup.NO_VAT)), "2160.00")

        val refund = refundByAmount(sale, "2160.00", "1080.00")

        assertEquals(mapOf(VAT_16_PERCENT to 8_000L), bfdTicketTax(kassa.bfd.lastTicket()))
        assertEquals(
            listOf(VatGroup.VAT_16 to 58_000L, VatGroup.NO_VAT to 50_000L),
            stored(refund).items.map { it.vatGroup to it.sum.tiyn() }
        )
        assertEquals(8_000L, shiftCounter("tax.VAT_16.OPERATION_SELL_RETURN.sum"))
    }

    @Test
    fun `весовой товар 1,5 по 333,33 - строка 500,00 к ближайшему тиыну`() {
        val weighed = ReceiptItemRequest(name = "Сыр", price = Decimal.parse("333.33"), quantity = Decimal.parse("1.5"))

        val failure = assertFailsWith<SuperkassaException> { sell(listOf(weighed), "499.99") }
        assertEquals("PAYMENTS_TOTAL_MISMATCH", failure.code)

        val sale = sell(listOf(weighed), "500.00")
        assertEquals(50_000L, kassa.bfd.lastTicket().items.single().commodity?.sum?.tiyn())
        assertEquals(50_000L, stored(sale).total.tiyn())
    }

    private fun sell(items: List<ReceiptItemRequest>, total: String): String = kassa.api.createSellReceipt(
        KKM, CASHIER_PIN,
        ReceiptSellRequest(idempotencyKey = "sale-$total", items = items, payments = listOf(cash(total)))
    ).documentId

    /** Возврат одной строкой без ставки, как его собирает касса при возврате суммой. */
    private fun refundByAmount(saleId: String, saleTotal: String, amount: String): String {
        val sale = kassa.document(saleId)
        val basis = ParentTicketRequest(
            parentTicketNumber = checkNotNull(sale.docNo),
            parentTicketDateTime = Instant.ofEpochMilli(sale.createdAt).truncatedTo(ChronoUnit.SECONDS)
                .atOffset(ZoneOffset.UTC).toLocalDateTime().toString(),
            kgdKkmId = KGD_NUMBER,
            parentTicketTotal = Decimal.parse(saleTotal),
            parentTicketIsOffline = false
        )
        val byAmount = ReceiptItemRequest(name = "Возврат", price = Decimal.parse(amount), quantity = Decimal.parse("1"))
        return kassa.api.createSellReturnReceipt(
            KKM, CASHIER_PIN,
            ReceiptSellReturnRequest(
                idempotencyKey = "return-$amount", items = listOf(byAmount), payments = listOf(cash(amount)), parentTicket = basis
            )
        ).documentId
    }

    private fun stored(documentId: String) = checkNotNull(kassa.storage.findFiscalDocumentWithReceiptPayload(documentId)).second

    private fun shiftCounter(key: String): Long? {
        val shift = checkNotNull(kassa.storage.findOpenShift(KKM))
        return kassa.storage.loadCounters(KKM, CounterScopes.SHIFT, shift.id)[key]
    }
}
