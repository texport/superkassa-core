package io.github.texport.superkassa.core.data.room

import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.ADMIN_PIN
import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.CASHIER_PIN
import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.KKM
import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.cash
import io.github.texport.superkassa.core.domain.api.model.common.CounterScopes
import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.domain.api.model.common.TaxRegime
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptItemRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptSellRequest
import kz.kazakhtelecom.proto.v203.OperationTypeEnum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * НДС 16 % «в том числе» одинаков в трёх местах: на бумаге (налог чека,
 * по которому печатается лента), в счётчиках смены и X/Z, в запросе в БФД
 * (налог, который БФД сложит из позиций, сторно и скидки).
 */
class RoomVatTest {
    private val kassa = RoomKassa(TaxRegime.VAT_PAYER, VatGroup.VAT_16).also { it.api.openShift(KKM, ADMIN_PIN) }

    @Test
    fun `сторно уходит в БФД со своим налогом - налог чека 137,93, а не 206,90`() {
        val id = sell(listOf(line("1000.00"), line("500.00"), line("500.00", storno = true)), "1000.00")

        assertEquals(mapOf(VAT_16_PERCENT to 13_793L), bfdTicketTax(kassa.bfd.lastTicket()))
        assertEquals(mapOf(VatGroup.VAT_16 to 13_793L), paperTax(id))
        assertEquals(13_793L, shiftCounter("tax.VAT_16.OPERATION_SELL.sum"))
    }

    @Test
    fun `скидка на чек уменьшает налог - 1000 минус 10 процентов дают 124,14`() {
        val id = sell(listOf(line("1000.00")), "900.00", discountPercent = "10")

        assertEquals(mapOf(VAT_16_PERCENT to 12_414L), bfdTicketTax(kassa.bfd.lastTicket()))
        assertEquals(mapOf(VatGroup.VAT_16 to 12_414L), paperTax(id))
        assertTrue("124,14" in kassa.api.getReceiptHtml(KKM, id, CASHIER_PIN), "tax on the printed form")
        assertEquals(12_414L to 90_000L, shiftCounter("tax.VAT_16.OPERATION_SELL.sum") to shiftCounter("tax.VAT_16.OPERATION_SELL.turnover"))
    }

    @Test
    fun `плательщик НДС - позиция без НДС не облагается ни на бумаге, ни в счётчиках, ни у БФД`() {
        val id = sell(listOf(line("1160.00"), line("1000.00", vat = VatGroup.NO_VAT)), "2160.00")

        assertEquals(mapOf(VAT_16_PERCENT to 16_000L), bfdTicketTax(kassa.bfd.lastTicket()))
        assertEquals(mapOf(VatGroup.VAT_16 to 16_000L), paperTax(id))
        val form = kassa.api.getReceiptHtml(KKM, id, CASHIER_PIN)
        assertEquals(1, "class=\"item-vat\"".toRegex().findAll(form).count(), "the exempt line shows its own rate")
        assertEquals(16_000L, shiftCounter("tax.VAT_16.OPERATION_SELL.sum"))
    }

    @Test
    fun `оборот по налогу в X-отчёте - с НДС, как у БФД`() {
        sell(listOf(line("1160.00")), "1160.00")

        kassa.api.createReport(KKM, CASHIER_PIN)

        val sale = kassa.bfd.reportTax(VAT_16_PERCENT, OperationTypeEnum.OPERATION_SELL)
        assertEquals(Triple(116_000L, 16_000L, 100_000L), Triple(sale.turnover.tiyn(), sale.sum.tiyn(), sale.turnover_without_tax?.tiyn()))
        assertEquals(116_000L, shiftCounter("tax.VAT_16.OPERATION_SELL.turnover"))
    }

    @Test
    fun `НДС 0 процентов уходит налогом со ставкой 0, а не позицией без НДС`() {
        val id = sell(listOf(line("500.00", vat = VatGroup.VAT_0)), "500.00")

        val tax = kassa.bfd.lastTicket().items.single().commodity?.taxes?.single()
        assertEquals(0 to 0L, tax?.percent to tax?.sum?.tiyn())
        assertEquals(mapOf(VatGroup.VAT_0 to 0L), paperTax(id))

        kassa.api.createReport(KKM, CASHIER_PIN)
        assertEquals(50_000L, kassa.bfd.reportTax(0, OperationTypeEnum.OPERATION_SELL).turnover.tiyn())
    }

    private fun sell(items: List<ReceiptItemRequest>, total: String, discountPercent: String? = null): String =
        kassa.api.createSellReceipt(
            KKM, CASHIER_PIN,
            ReceiptSellRequest(
                idempotencyKey = "sale-${items.size}-$total", items = items, payments = listOf(cash(total)),
                discountPercent = discountPercent?.let { Decimal.parse(it) }
            )
        ).documentId

    /** Налог чека, по которому печатается лента: ставка → налог в тиынах. */
    private fun paperTax(documentId: String): Map<VatGroup, Long> =
        checkNotNull(kassa.storage.findFiscalDocumentWithReceiptPayload(documentId)).second.ticketTaxes.orEmpty()
            .associate { it.vatGroup to it.taxSum.tiyn() }

    private fun shiftCounter(key: String): Long? {
        val shift = checkNotNull(kassa.storage.findOpenShift(KKM))
        return kassa.storage.loadCounters(KKM, CounterScopes.SHIFT, shift.id)[key]
    }
}

/** Позиция на всю сумму; ставка — своя либо ставка кассы. */
internal fun line(sum: String, vat: VatGroup? = null, storno: Boolean = false) = ReceiptItemRequest(
    name = "Нан", price = Decimal.parse(sum), quantity = Decimal.parse("1"), vatGroup = vat?.name, isStorno = storno
)
