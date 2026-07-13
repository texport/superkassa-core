package io.github.texport.superkassa.core.domain.impl.helper.zxreport

import io.github.texport.superkassa.core.domain.api.model.common.CounterKeyFormats
import io.github.texport.superkassa.core.domain.api.model.common.format
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

    @Test
    fun `resolveTicketOperations handles all payment types with large numbers`() {
        val op = "OPERATION_BUY_RETURN"
        val counters = mapOf(
            CounterKeyFormats.TICKET_SUM.format(op) to Long.MAX_VALUE,
            CounterKeyFormats.PAYMENT_SUM.format(op, "PAYMENT_CASH") to 1000L,
            CounterKeyFormats.PAYMENT_COUNT.format(op, "PAYMENT_CASH") to 1L,
            CounterKeyFormats.PAYMENT_SUM.format(op, "PAYMENT_CARD") to 2000L,
            CounterKeyFormats.PAYMENT_COUNT.format(op, "PAYMENT_CARD") to 2L,
            CounterKeyFormats.PAYMENT_SUM.format(op, "PAYMENT_CREDIT") to 3000L,
            CounterKeyFormats.PAYMENT_COUNT.format(op, "PAYMENT_CREDIT") to 3L,
            CounterKeyFormats.PAYMENT_SUM.format(op, "PAYMENT_TARE") to 4000L,
            CounterKeyFormats.PAYMENT_COUNT.format(op, "PAYMENT_TARE") to 4L,
            CounterKeyFormats.PAYMENT_SUM.format(op, "PAYMENT_MOBILE") to 5000L,
            CounterKeyFormats.PAYMENT_COUNT.format(op, "PAYMENT_MOBILE") to 5L,
            CounterKeyFormats.PAYMENT_SUM.format(op, "PAYMENT_ELECTRONIC") to 6000L,
            CounterKeyFormats.PAYMENT_COUNT.format(op, "PAYMENT_ELECTRONIC") to 6L
        )

        val result = ZxTicketOperationsBlockBuilder.resolveTicketOperations(counters)
        val targetOp = result.first { it.operation == op }

        assertEquals(Long.MAX_VALUE, targetOp.ticketsSumBills)

        val cash = targetOp.payments.first { it.payment == "PAYMENT_CASH" }
        assertEquals(1000L, cash.sumBills)
        assertEquals(1L, cash.count)

        val card = targetOp.payments.first { it.payment == "PAYMENT_CARD" }
        assertEquals(2000L, card.sumBills)
        assertEquals(2L, card.count)

        val credit = targetOp.payments.first { it.payment == "PAYMENT_CREDIT" }
        assertEquals(3000L, credit.sumBills)
        assertEquals(3L, credit.count)

        val tare = targetOp.payments.first { it.payment == "PAYMENT_TARE" }
        assertEquals(4000L, tare.sumBills)
        assertEquals(4L, tare.count)

        val mobile = targetOp.payments.first { it.payment == "PAYMENT_MOBILE" }
        assertEquals(5000L, mobile.sumBills)
        assertEquals(5L, mobile.count)

        val electronic = targetOp.payments.first { it.payment == "PAYMENT_ELECTRONIC" }
        assertEquals(6000L, electronic.sumBills)
        assertEquals(6L, electronic.count)
    }
}
