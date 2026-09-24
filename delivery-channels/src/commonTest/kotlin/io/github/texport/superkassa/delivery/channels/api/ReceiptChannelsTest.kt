package io.github.texport.superkassa.delivery.channels.api

import io.github.texport.superkassa.delivery.api.model.DeliveryChannel
import io.github.texport.superkassa.delivery.api.port.DeliveryPort
import io.github.texport.superkassa.delivery.channels.impl.common.DeliveryCodes
import io.github.texport.superkassa.delivery.channels.impl.common.receiptRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Канал без настроек не исчезает и не выдаёт себя за доставленный: он отказывает с причиной. */
class ReceiptChannelsTest {

    @Test
    fun `ненастроенный канал отказывает кодом и словами на трёх языках`() {
        val unconfigured = listOf(
            smsChannel(null, "key"),
            smsChannel(" ", null),
            telegramChannel(null),
            telegramChannel(""),
            whatsAppChannel(null, "sender"),
            whatsAppChannel(" ", "sender"),
            whatsAppChannel("token", " "),
            whatsAppChannel("token", null),
            emailChannel(null)
        )

        unconfigured.forEach { channel -> assertNotConfigured(channel) }
    }

    @Test
    fun `настроенный канал отправляет по-настоящему и сбой связи называет отказом`() {
        val closedPort = "http://127.0.0.1:1/send?to={phone}&text={text}"
        val sms = smsChannel(closedPort, "key")

        val result = sms.send(receiptRequest(DeliveryChannel.SMS))

        assertEquals(DeliveryChannel.SMS, sms.channel)
        assertEquals(DeliveryChannel.SMS, smsChannel(closedPort, null).channel)
        assertEquals(DeliveryChannel.SMS, smsChannel(closedPort, " ").channel)
        assertFalse(result.ok)
        assertEquals(DeliveryCodes.CHANNEL_FAILED, result.code)
        assertEquals(DeliveryChannel.TELEGRAM, telegramChannel("1:AA").channel)
        assertEquals(DeliveryChannel.WHATSAPP, whatsAppChannel("token", "sender").channel)
    }

    private fun assertNotConfigured(channel: DeliveryPort) {
        val result = channel.send(receiptRequest(channel.channel))

        assertFalse(result.ok)
        assertEquals("DELIVERY_${channel.channel.name}_NOT_CONFIGURED", result.code)
        assertEquals(false, result.retryable)
        val message = result.message.orEmpty()
        assertTrue(message.contains("не настроен"), message)
        assertTrue(message.contains("бапталмаған"), message)
        assertTrue(message.contains("is not configured"), message)
    }
}
