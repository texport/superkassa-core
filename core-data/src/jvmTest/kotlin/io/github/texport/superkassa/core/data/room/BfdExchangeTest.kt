package io.github.texport.superkassa.core.data.room

import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.ADMIN_PIN
import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.CASHIER_PIN
import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.KKM
import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.cash
import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.item
import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.presentation.api.model.kkm.CashOperationRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptSellRequest
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Обмен кассы с БФД при сбоях связи: номер запроса, токен и очередь досылки.
 *
 * БФД ведёт учёт кассы по правилам прод-референса: повтор с тем же токеном
 * и номером запроса он узнаёт и второй раз документ не учитывает.
 */
class BfdExchangeTest {
    private val clock = MovableClock()
    private val kassa = RoomKassa(clock = clock).also { it.api.openShift(KKM, ADMIN_PIN) }

    @Test
    fun `проверка связи после чека без ответа не занимает его номер, и БФД учитывает чек один раз`() {
        kassa.bfd.loseNextAnswer()
        val sale = sell("500.00", "sale-1")
        clock.move(PAST_RECONNECT_MILLIS)

        kassa.api.checkOfdConnection(KKM)
        kassa.reconnectAndResend(clock)

        assertSaleCountedOnceAndKassaWorks(sale)
    }

    @Test
    fun `проверка связи, нажатая пока чек ждёт ответа, ждёт конца обмена и номер чека не занимает`() {
        kassa.bfd.holdNextAnswerThenLose(maxMillis = HOLD_MILLIS)
        var sale = ""
        val cashier = thread { sale = sell("500.00", "sale-1") }
        awaitTicketArrived()

        kassa.api.checkOfdConnection(KKM)
        cashier.join()
        kassa.reconnectAndResend(clock)

        assertSaleCountedOnceAndKassaWorks(sale)
    }

    @Test
    fun `первая онлайн-операция после досылки не возвращает кассе старый токен`() {
        kassa.bfd.unreachableOnce()
        sell("100.00", "sale-1")
        kassa.reconnectAndResend(clock)

        kassa.api.cashIn(KKM, CASHIER_PIN, CashOperationRequest(Decimal.parse("50.00"), "in-1"))
        val next = sell("300.00", "sale-2")

        assertEquals(kassa.bfd.issuedToken, kassa.token())
        assertEquals("SENT", kassa.document(next).ofdStatus)
        assertEquals(2, kassa.bfd.countedTickets().size)
    }

    @Test
    fun `отказ БФД при досылке снимает документ с очереди и не повторяется`() {
        kassa.bfd.unreachableOnce()
        val sale = sell("100.00", "sale-1")
        kassa.bfd.refuseNext(INCORRECT_REQUEST_DATA)
        kassa.reconnectAndResend(clock)
        val sentBefore = kassa.bfd.exchanges.size

        kassa.reconnectAndResend(clock)

        val task = kassa.queueTasks(sale).single()
        assertEquals("REJECTED" to null, task.status to task.nextAttemptAt)
        assertEquals("FAILED" to INCORRECT_REQUEST_DATA, kassa.document(sale).let { it.ofdStatus to it.ofdErrorCode })
        assertEquals(sentBefore, kassa.bfd.exchanges.size, "a refused document is not resent")
        // Спецификация, п. 5.2: устойчивый отказ при досылке — блокировка до разбора.
        assertEquals("BLOCKED", kassa.kkm().state)
    }

    private fun assertSaleCountedOnceAndKassaWorks(sale: String) {
        val ticketReqNum = kassa.bfd.exchanges.first { it.request.ticket != null }.reqNum
        val others = kassa.bfd.exchanges.filter { it.request.ticket == null }
        assertTrue(others.none { it.reqNum == ticketReqNum }, "another request took the unanswered ticket's number")
        assertEquals(1, kassa.bfd.countedTickets().size, "the BFD counted the ticket once")
        assertEquals("SENT", kassa.document(sale).ofdStatus)
        assertEquals("ACTIVE", kassa.kkm().state)
        assertEquals(kassa.bfd.issuedToken, kassa.token())
        assertEquals("SENT", kassa.document(sell("700.00", "sale-next")).ofdStatus)
        assertNull(kassa.kkm().blockReasonCode)
    }

    private fun awaitTicketArrived() {
        val deadline = System.currentTimeMillis() + HOLD_MILLIS
        while (kassa.bfd.exchanges.none { it.request.ticket != null }) {
            check(System.currentTimeMillis() < deadline) { "the ticket never reached the BFD" }
            Thread.sleep(POLL_MILLIS)
        }
    }

    private fun sell(total: String, key: String): String = kassa.api.createSellReceipt(
        KKM, CASHIER_PIN,
        ReceiptSellRequest(idempotencyKey = key, items = listOf(item(total)), payments = listOf(cash(total)))
    ).documentId

    private companion object {
        const val PAST_RECONNECT_MILLIS = 61_000L
        const val HOLD_MILLIS = 1_500L
        const val POLL_MILLIS = 10L

        /** RESULT_TYPE_INCORRECT_REQUEST_DATA. */
        const val INCORRECT_REQUEST_DATA = 13
    }
}
