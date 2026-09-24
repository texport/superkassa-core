package io.github.texport.superkassa.testing.api.kassa

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptItemRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptPaymentRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptSellRequest
import io.github.texport.superkassa.testing.api.bfd.counters
import io.github.texport.superkassa.testing.api.kassa.BenchDirectory.Companion.NOT_PAYER
import io.github.texport.superkassa.testing.impl.kassa.Receipts
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * X- и Z-отчёт кассы совпадают с итогами, которые БФД сам считает по принятым
 * документам (`OperationCalculator` референса): по отделам, операциям, скидкам,
 * наценкам, окончательному итогу, чекам и оплатам, внесениям и изъятиям,
 * необнуляемым суммам, ящику и выручке.
 *
 * Вторая смена несёт счёт чеков и операций «за всё время» из первой.
 * Прежде касса ставила в строку операций число чеков, а БФД — число
 * позиций, и число скидок и наценок касса брала оттуда же.
 */
class ShiftReportParityTest {
    private val directory = BenchDirectory()
    private val bench = directory.open()
    private val kassa = bench.registerKassa(NOT_PAYER)

    @AfterTest
    fun tearDown() {
        bench.close()
        directory.close()
    }

    @Test
    fun `X и Z кассы совпадают с итогами БФД на смене с позициями, скидками и наценками`() {
        firstShift()
        kassa.openShift()
        val receipts = listOf(
            kassa.sell(itemModifiers()),
            kassa.sell(receiptMarkupWithChange()),
            kassa.sellWithDiscount(),
            kassa.refund(kassa.sell()),
            kassa.offlineSale()
        )
        kassa.resendQueue()
        kassa.cashIn()
        kassa.cashOut()

        kassa.xReport()
        assertEquals(bench.bfd.shiftReport(kassa.systemId), bench.bfd.xReports().last().counters(), "X-report")
        kassa.closeShift()

        assertEquals(List(receipts.size) { DeliveryStatus.ONLINE_OK }.dropLast(1), receipts.dropLast(1).map { it.deliveryStatus })
        assertZ(shift = 1)
    }

    private fun firstShift() {
        kassa.openShift()
        kassa.sell()
        kassa.cashIn()
        kassa.closeShift()
        assertZ(shift = 0)
    }

    private fun assertZ(shift: Int) =
        assertEquals(bench.bfd.closedShiftReports(kassa.systemId)[shift], bench.bfd.closeShifts()[shift].z_report?.counters(), "Z-report")

    /** Скидка и наценка на позиции и они же у сторнированных позиций: сторно идёт после скидки. */
    private fun itemModifiers() = ReceiptSellRequest(
        idempotencyKey = Receipts.key("sale"),
        items = listOf(
            item("Хлеб бородинский", "1000.00", "2").copy(discountSum = money("150.00")),
            item("Молоко 2,5%", "300.00", "1").copy(markupSum = money("20.00")),
            item("Сыр косичка", "200.00", "1").copy(discountSum = money("10.00"), isStorno = true),
            item("Чай зелёный", "100.00", "1").copy(markupSum = money("5.00"), isStorno = true)
        ),
        payments = listOf(pay("CASH", "1000.00"), pay("CARD", "875.00"))
    )

    /** Наценка на чек, наличные со сдачей. */
    private fun receiptMarkupWithChange() = ReceiptSellRequest(
        idempotencyKey = Receipts.key("sale"),
        items = listOf(item("Яблоки апорт", "400.00", "1"), item("Кумыс", "250.00", "3")),
        markupSum = money("50.00"),
        payments = listOf(pay("CASH", "1200.00")),
        taken = money("1500.00")
    )

    private fun item(name: String, price: String, quantity: String) =
        ReceiptItemRequest(name = name, price = money(price), quantity = Decimal.parse(quantity))

    private fun pay(type: String, sum: String) = ReceiptPaymentRequest(type = type, sum = money(sum))

    private fun money(value: String) = Decimal.parse(value)
}
