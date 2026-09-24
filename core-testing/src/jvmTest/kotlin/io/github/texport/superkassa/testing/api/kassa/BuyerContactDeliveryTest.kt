package io.github.texport.superkassa.testing.api.kassa

import io.github.texport.superkassa.core.domain.api.model.settings.DeliveryChannelSettings
import io.github.texport.superkassa.core.domain.api.model.settings.DeliverySettings
import io.github.texport.superkassa.core.presentation.api.model.receipt.CustomerContactRequest
import io.github.texport.superkassa.delivery.api.model.DeliveryChannel
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Чек уходит покупателю — на контакт из чека, в тот включённый канал,
 * что понимает этот вид контакта. Без контакта покупатель чека не просил,
 * и задач доставки нет.
 */
class BuyerContactDeliveryTest {
    private val directory = BenchDirectory()
    private var stand: DeliveryStand? = null

    @AfterTest
    fun tearDown() {
        stand?.close()
        directory.close()
    }

    @Test
    fun `чек уходит на телефон покупателя, а не на номер из настроек канала`() {
        val sms = RecordingSms()
        val kassa = open(DeliverySettings(channels = listOf(DeliveryChannelSettings("SMS", documentFormat = "HTML", destination = OWNER))), sms)

        val sale = kassa.sell(buyer = BUYER)
        kassa.deliverReceipts()

        assertEquals(listOf(sale.documentId to BUYER_PHONE), sms.sent.map { it.documentId to it.destination })
    }

    @Test
    fun `без контакта покупателя задач доставки нет`() {
        val sms = RecordingSms()
        val kassa = open(smsReceipt(), sms)

        val sale = kassa.sell()

        assertEquals(0, kassa.deliverReceipts())
        assertEquals(emptyList(), kassa.deliveries(sale))
        assertEquals(emptyList(), sms.sent)
    }

    @Test
    fun `почта покупателя - чек только в канал почты`() {
        val sms = RecordingSms()
        val email = RecordingSms(channel = DeliveryChannel.EMAIL)
        val kassa = open(smsReceipt(DeliverySettings(channels = listOf(DeliveryChannelSettings("EMAIL", documentFormat = "HTML")))), sms, email)

        val sale = kassa.sell(buyer = CustomerContactRequest(email = "buyer@mail.kz"))
        kassa.deliverReceipts()

        assertEquals(listOf("EMAIL"), kassa.deliveries(sale).map { it.channel })
        assertEquals(listOf("buyer@mail.kz"), email.sent.map { it.destination })
        assertEquals(emptyList(), sms.sent)
    }

    private fun open(delivery: DeliverySettings, vararg channels: RecordingSms): ReadyKassa =
        DeliveryStand(directory, delivery, channels.toList()).also { stand = it }.kassa

    private companion object {
        /** Номер владельца в настройках канала: чеку покупателя туда не место. */
        const val OWNER = "+77000000000"
    }
}
