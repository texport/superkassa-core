package io.github.texport.superkassa.testing.api.kassa

import io.github.texport.superkassa.core.presentation.api.model.delivery.ReceiptDeliveryState
import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus
import io.github.texport.superkassa.testing.impl.kassa.Receipts
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Чек покупателю уходит в фоне: кассир получает ответ, когда БФД принял
 * чек, а не когда провайдер SMS ответил. Задача доставки живёт в базе
 * и переживает перезапуск.
 */
class ReceiptDeliveryTest {
    private val directory = BenchDirectory()
    private var stand: DeliveryStand? = null

    @AfterTest
    fun tearDown() {
        stand?.close()
        directory.close()
    }

    @Test
    fun `чек отвечает кассиру, пока медленный провайдер ещё отправляет`() {
        val provider = CountDownLatch(1)
        val sms = RecordingSms(gate = provider)
        val kassa = open(listOf(sms), background = true)

        val sale = try {
            // Прежде чек ждал провайдера в потоке пробития: здесь это был бы срыв срока ожидания.
            CompletableFuture.supplyAsync { kassa.sell() }.get(WAIT.inWholeSeconds, TimeUnit.SECONDS).also {
                assertEquals(true, sms.entered.await(WAIT.inWholeSeconds, TimeUnit.SECONDS), "background did not start sending")
                assertEquals(ReceiptDeliveryState.PENDING, kassa.deliveries(it).sms().state)
            }
        } finally {
            provider.countDown()
        }
        assertEquals(DeliveryStatus.ONLINE_OK, sale.deliveryStatus)
        eventually { kassa.deliveries(sale).sms().state == ReceiptDeliveryState.DELIVERED }
        assertEquals(1, sms.sent.size)
    }

    @Test
    fun `доставка доходит фоном без участия кассира`() {
        val sms = RecordingSms()
        val kassa = open(listOf(sms), background = true)

        val sale = kassa.sell()

        eventually { kassa.deliveries(sale).sms().state == ReceiptDeliveryState.DELIVERED }
        assertEquals(listOf(sale.documentId to "+77017654321"), sms.sent.map { it.documentId to it.destination })
        assertEquals("HTML", sms.sent.single().payloadType)
    }

    @Test
    fun `фон и заход платформы вместе не отправляют чек дважды`() {
        val provider = CountDownLatch(1)
        val sms = RecordingSms(gate = provider)
        val kassa = open(listOf(sms), background = true)
        val sale = kassa.sell()

        try {
            sms.entered.await(WAIT.inWholeSeconds, TimeUnit.SECONDS)
            assertEquals(0, kassa.deliverReceipts(), "the task is being sent by the background")
        } finally {
            provider.countDown()
        }
        eventually { kassa.deliveries(sale).sms().state == ReceiptDeliveryState.DELIVERED }
        assertEquals(0, kassa.deliverReceipts())
        assertEquals(1, sms.sent.size)
    }

    @Test
    fun `повтор кассиром доставленный чек второй раз не отправляет`() {
        val sms = RecordingSms()
        val kassa = open(listOf(sms), background = false)
        val request = Receipts.sale("500.00", "1")
        val sale = kassa.sell(request)
        kassa.deliverReceipts()

        val resent = kassa.resendReceipt(sale)
        val sameKey = kassa.sell(request)

        assertEquals(listOf(ReceiptDeliveryState.DELIVERED), resent.map { it.state })
        assertEquals(sale.documentId, sameKey.documentId)
        assertEquals(0, kassa.deliverReceipts())
        assertEquals(1, sms.sent.size)
    }

    @Test
    fun `после перезапуска кассы недосланный чек уходит`() {
        val before = RecordingSms()
        val kassa = open(listOf(before), background = false)
        val sale = kassa.sell()
        assertEquals(0, before.sent.size, "delivery must not run while the receipt is being issued")

        val after = RecordingSms()
        checkNotNull(stand).restart(listOf(after))
        checkNotNull(stand).kassa.deliverReceipts()

        eventually { checkNotNull(stand).kassa.deliveries(sale).sms().state == ReceiptDeliveryState.DELIVERED }
        assertEquals(listOf(sale.documentId), after.sent.map { it.documentId })
    }

    @Test
    fun `автономный чек уходит покупателю, когда БФД принял его из очереди, и один раз`() {
        val sms = RecordingSms()
        val kassa = open(listOf(sms), background = false)
        val sale = kassa.offlineSale()
        assertEquals(0, kassa.deliverReceipts(), "BFD has not accepted the receipt yet")

        kassa.resendQueue()
        kassa.deliverReceipts()
        kassa.resendQueue()

        assertEquals(0, kassa.deliverReceipts())
        assertEquals(ReceiptDeliveryState.DELIVERED, kassa.deliveries(sale).sms().state)
        assertEquals(listOf(sale.documentId), sms.sent.map { it.documentId })
    }

    /** Касса с SMS [channels]; [background] — фон доставляет сам, иначе чеки уходят только заходом проверки. */
    private fun open(channels: List<RecordingSms>, background: Boolean): ReadyKassa {
        val config = if (background) ::backgroundConfig else ::testSuperkassaConfig
        return DeliveryStand(directory, smsReceipt(), channels, config).also { stand = it }.kassa
    }
}
