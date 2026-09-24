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
import kz.kazakhtelecom.proto.v203.OperationTypeEnum.OPERATION_SELL
import kz.kazakhtelecom.proto.v203.TicketRequest.Item.ItemTypeEnum
import kz.kazakhtelecom.proto.v203.ZXReport
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Скидка и наценка на позицию уходят в БФД модификатором позиции, как
 * требует CPCR: предмет потребления — полной суммой строки, за ним
 * элемент скидки или наценки со своей суммой и налогом. По ним БФД
 * строит X/Z: отделы и операции — без скидок и наценок, скидки
 * и наценки — отдельными строками, итог — с ними.
 */
class RoomItemModifierTest {
    private val kassa = RoomKassa(TaxRegime.VAT_PAYER, VatGroup.VAT_16).also { it.api.openShift(KKM, ADMIN_PIN) }

    @Test
    fun `скидка на позицию - позиция полной суммой, за ней скидка`() {
        sell(line("1000.00").copy(discountSum = Decimal.parse("100.00")), "900.00")

        val items = kassa.bfd.lastTicket().items
        assertEquals(listOf(ItemTypeEnum.ITEM_TYPE_COMMODITY, ItemTypeEnum.ITEM_TYPE_DISCOUNT), items.map { it.type })
        assertEquals(100_000L to 100_000L, items[0].commodity?.price?.tiyn() to items[0].commodity?.sum?.tiyn())
        assertEquals(10_000L, items[1].discount?.sum?.tiyn())
    }

    @Test
    fun `наценка на позицию - позиция полной суммой, за ней наценка`() {
        sell(line("1000.00").copy(markupSum = Decimal.parse("50.00")), "1050.00")

        val items = kassa.bfd.lastTicket().items
        assertEquals(listOf(ItemTypeEnum.ITEM_TYPE_COMMODITY, ItemTypeEnum.ITEM_TYPE_MARKUP), items.map { it.type })
        assertEquals(100_000L to 5_000L, items[0].commodity?.sum?.tiyn() to items[1].markup?.sum?.tiyn())
    }

    @Test
    fun `скидка сторнированной позиции уходит сторно скидки, налог чека - с сумм после скидок`() {
        val storno = line("500.00", storno = true).copy(discountSum = Decimal.parse("50.00"))
        kassa.api.createSellReceipt(
            KKM, CASHIER_PIN,
            ReceiptSellRequest(idempotencyKey = "sale-storno", items = listOf(line("1000.00"), storno), payments = listOf(cash("550.00")))
        )

        val ticket = kassa.bfd.lastTicket()
        val types = listOf(ItemTypeEnum.ITEM_TYPE_COMMODITY, ItemTypeEnum.ITEM_TYPE_STORNO_COMMODITY, ItemTypeEnum.ITEM_TYPE_STORNO_DISCOUNT)
        assertEquals(types, ticket.items.map { it.type })
        assertEquals(50_000L to 5_000L, ticket.items[1].storno_commodity?.sum?.tiyn() to ticket.items[2].storno_discount?.sum?.tiyn())
        assertEquals(mapOf(VAT_16_PERCENT to 7_586L), bfdTicketTax(ticket))
    }

    @Test
    fun `налог позиции со скидкой у БФД и в смене - с суммы после скидки, 124,14`() {
        sell(line("1000.00").copy(discountSum = Decimal.parse("100.00")), "900.00")

        assertEquals(mapOf(VAT_16_PERCENT to 12_414L), bfdTicketTax(kassa.bfd.lastTicket()))
        assertEquals(12_414L to 90_000L, shiftCounter("tax.VAT_16.OPERATION_SELL.sum") to shiftCounter("tax.VAT_16.OPERATION_SELL.turnover"))
    }

    @Test
    fun `X-отчёт - отдел и операции без скидок, скидки строкой, итог со скидками`() {
        sell(line("1000.00").copy(discountSum = Decimal.parse("100.00")), "900.00")
        sell(line("2000.00"), "1800.00", discountPercent = "10")

        kassa.api.createReport(KKM, CASHIER_PIN)

        val x = kassa.bfd.xReports().last()
        assertEquals(300_000L, x.sections.single().operations.sell().sum.tiyn())
        assertEquals(300_000L, x.operations.sell().sum.tiyn())
        assertEquals(30_000L, x.discounts.sell().sum.tiyn())
        assertEquals(270_000L, x.total_result.sell().sum.tiyn())
        assertEquals(20_000L, x.ticket_operations.single { it.operation == OPERATION_SELL }.discount_sum?.tiyn())
    }

    private fun List<ZXReport.Operation>.sell() = single { it.operation == OPERATION_SELL }

    private fun sell(item: ReceiptItemRequest, total: String, discountPercent: String? = null) {
        kassa.api.createSellReceipt(
            KKM, CASHIER_PIN,
            ReceiptSellRequest(
                idempotencyKey = "sale-${item.price}-$total", items = listOf(item), payments = listOf(cash(total)),
                discountPercent = discountPercent?.let { Decimal.parse(it) }
            )
        )
    }

    private fun shiftCounter(key: String): Long? {
        val shift = checkNotNull(kassa.storage.findOpenShift(KKM))
        return kassa.storage.loadCounters(KKM, CounterScopes.SHIFT, shift.id)[key]
    }
}
