package kz.mybrain.superkassa.core.domain.helper.zxreport

import kz.mybrain.superkassa.core.domain.model.common.CounterKeyFormats
import kotlin.test.Test
import kotlin.test.assertEquals

class ZxTicketOperationsBlockBuilderTest {

    @Test
    fun `resolveTicketOperations with empty counters returns default values`() {
        val counters = emptyMap<String, Long>()
        val result = ZxTicketOperationsBlockBuilder.resolveTicketOperations(counters)

        // There should be 4 operations (SELL, SELL_RETURN, BUY, BUY_RETURN)
        assertEquals(4, result.size)

        val sellOp = result.first { it.operation == "OPERATION_SELL" }
        assertEquals(0L, sellOp.ticketsTotalCount)
        assertEquals(0L, sellOp.ticketsCount)
        assertEquals(0L, sellOp.ticketsSumBills)
        assertEquals(0L, sellOp.offlineCount)
        assertEquals(0L, sellOp.discountSumBills)
        assertEquals(0L, sellOp.markupSumBills)
        assertEquals(0L, sellOp.changeSumBills)

        // Payments should be 6 default payment types
        assertEquals(6, sellOp.payments.size)
        sellOp.payments.forEach { payment ->
            assertEquals(0L, payment.sumBills)
            assertEquals(0L, payment.count)
        }
    }

    @Test
    fun `resolveTicketOperations maps all fields and payments correctly`() {
        val op = "OPERATION_SELL"
        val counters = mapOf(
            CounterKeyFormats.TICKET_TOTAL_COUNT.format(op) to 15L,
            CounterKeyFormats.TICKET_COUNT.format(op) to 12L,
            CounterKeyFormats.TICKET_SUM.format(op) to 25000L,
            CounterKeyFormats.TICKET_OFFLINE_COUNT.format(op) to 3L,
            CounterKeyFormats.TICKET_DISCOUNT_SUM.format(op) to 500L,
            CounterKeyFormats.TICKET_MARKUP_SUM.format(op) to 200L,
            CounterKeyFormats.TICKET_CHANGE_SUM.format(op) to 300L,
            CounterKeyFormats.PAYMENT_SUM.format(op, "PAYMENT_CASH") to 15000L,
            CounterKeyFormats.PAYMENT_COUNT.format(op, "PAYMENT_CASH") to 8L,
            CounterKeyFormats.PAYMENT_SUM.format(op, "PAYMENT_CARD") to 10000L,
            CounterKeyFormats.PAYMENT_COUNT.format(op, "PAYMENT_CARD") to 4L
        )

        val result = ZxTicketOperationsBlockBuilder.resolveTicketOperations(counters)
        val sellOp = result.first { it.operation == op }

        assertEquals(15L, sellOp.ticketsTotalCount)
        assertEquals(12L, sellOp.ticketsCount)
        assertEquals(25000L, sellOp.ticketsSumBills)
        assertEquals(3L, sellOp.offlineCount)
        assertEquals(500L, sellOp.discountSumBills)
        assertEquals(200L, sellOp.markupSumBills)
        assertEquals(300L, sellOp.changeSumBills)

        val cashPayment = sellOp.payments.first { it.payment == "PAYMENT_CASH" }
        assertEquals(15000L, cashPayment.sumBills)
        assertEquals(8L, cashPayment.count)

        val cardPayment = sellOp.payments.first { it.payment == "PAYMENT_CARD" }
        assertEquals(10000L, cardPayment.sumBills)
        assertEquals(4L, cardPayment.count)

        val creditPayment = sellOp.payments.first { it.payment == "PAYMENT_CREDIT" }
        assertEquals(0L, creditPayment.sumBills)
        assertEquals(0L, creditPayment.count)
    }
}
