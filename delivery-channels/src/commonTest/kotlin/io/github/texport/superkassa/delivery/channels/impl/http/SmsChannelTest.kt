package io.github.texport.superkassa.delivery.channels.impl.http

import io.github.texport.superkassa.delivery.api.model.DeliveryChannel
import io.github.texport.superkassa.delivery.channels.impl.common.DeliveryCodes
import io.github.texport.superkassa.delivery.channels.impl.common.RecordingJournal
import io.github.texport.superkassa.delivery.channels.impl.common.receiptRequest
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SmsChannelTest {
    private val journal = RecordingJournal()

    private fun channel(provider: ProviderStub, key: String? = KEY) =
        SmsChannel("https://sms.test/send?to={phone}&text={text}", key, provider.clients, journal)

    @Test
    fun `номер и текст подставляются в адрес шлюза закодированными, ключ уходит заголовком`() {
        val provider = ProviderStub()

        val result = channel(provider).send(receiptRequest(DeliveryChannel.SMS))

        assertTrue(result.ok)
        assertEquals(HttpMethod.Get, provider.request.method)
        assertEquals(
            "https://sms.test/send?to=%2B7%20%28777%29%20000-11-22&text=%D0%A7%D0%B5%D0%BA%3A%20https%3A%2F%2Freceipt.test%2Fdoc-1",
            provider.request.url.toString()
        )
        assertEquals("Bearer $KEY", provider.request.headers[HttpHeaders.Authorization])
        assertTrue(journal.written.contains("SMS delivered: documentId=DOC-1"))
    }

    @Test
    fun `без ключа запрос уходит без заголовка, без ссылки — с номером готового чека`() {
        val provider = ProviderStub()

        channel(provider, key = null).send(receiptRequest(DeliveryChannel.SMS, payloadUrl = null))

        assertNull(provider.request.headers[HttpHeaders.Authorization])
        assertEquals("Чек DOC-1 готов", provider.request.url.parameters["text"])
    }

    @Test
    fun `отказ шлюза — отказ доставки с кодом ответа, ключ не уходит ни наружу, ни в журнал`() {
        val provider = ProviderStub(HttpStatusCode.Forbidden, "rejected: https://sms.test/send?api_key=$KEY and $KEY")

        val result = channel(provider).send(receiptRequest(DeliveryChannel.SMS))

        assertFalse(result.ok)
        assertEquals(DeliveryCodes.PROVIDER_REJECTED, result.code)
        val message = result.message.orEmpty()
        assertTrue(message.contains("кодом 403"), message)
        assertTrue(message.contains("status 403"), message)
        assertFalse(message.contains(KEY), message)
        assertTrue(journal.written.contains("status=403"))
        assertFalse(journal.written.contains(KEY), journal.written)
    }

    @Test
    fun `сбой связи — отказ с родом сбоя, без текста исключения`() {
        val provider = ProviderStub(failure = IOException("refused: https://sms.test/send?api_key=$KEY"))

        val result = channel(provider).send(receiptRequest(DeliveryChannel.SMS))

        assertFalse(result.ok)
        assertEquals(DeliveryCodes.CHANNEL_FAILED, result.code)
        assertTrue(result.message.orEmpty().contains("IOException"))
        assertFalse(result.message.orEmpty().contains(KEY))
        assertFalse(journal.written.contains(KEY), journal.written)
        assertTrue(journal.written.contains("SMS failed: documentId=DOC-1, reason=IOException"))
    }

    @Test
    fun `адрес шлюза, который не разбирается, — отказ, а не падение кассы`() {
        val provider = ProviderStub()
        val broken = SmsChannel("http://sms.test:99999999999/{phone}/{text}", null, provider.clients, journal)

        val result = broken.send(receiptRequest(DeliveryChannel.SMS))

        assertFalse(result.ok)
        assertEquals(DeliveryCodes.CHANNEL_FAILED, result.code)
    }

    @Test
    fun `отвергнутый клиентом запрос — тот же отказ, что и сбой связи`() {
        val provider = ProviderStub(failure = IllegalArgumentException("bad host"))

        val result = channel(provider).send(receiptRequest(DeliveryChannel.SMS))

        assertEquals(DeliveryCodes.CHANNEL_FAILED, result.code)
        assertTrue(result.message.orEmpty().contains("IllegalArgumentException"))
    }

    @Test
    fun `без получателя шлюз не вызывается`() {
        val provider = ProviderStub()

        val result = channel(provider).send(receiptRequest(DeliveryChannel.SMS, destination = " "))

        assertFalse(result.ok)
        assertEquals("DELIVERY_SMS_DESTINATION_REQUIRED", result.code)
        assertTrue(provider.requests.isEmpty())
    }

    private companion object {
        const val KEY = "sms-key-do-not-print"
    }
}
