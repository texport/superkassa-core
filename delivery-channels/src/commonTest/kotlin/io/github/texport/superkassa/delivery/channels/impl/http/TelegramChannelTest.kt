package io.github.texport.superkassa.delivery.channels.impl.http

import io.github.texport.superkassa.delivery.api.model.DeliveryChannel
import io.github.texport.superkassa.delivery.channels.impl.common.DeliveryCodes
import io.github.texport.superkassa.delivery.channels.impl.common.RecordingJournal
import io.github.texport.superkassa.delivery.channels.impl.common.receiptRequest
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TelegramChannelTest {
    private val journal = RecordingJournal()

    @Test
    fun `сообщение уходит в Bot API с чатом и текстом в адресе`() {
        val provider = ProviderStub()

        val result = TelegramChannel(TOKEN, provider.clients, journal).send(receiptRequest(DeliveryChannel.TELEGRAM, destination = "4242"))

        assertTrue(result.ok)
        assertEquals(HttpMethod.Post, provider.request.method)
        val url = provider.request.url
        assertEquals("api.telegram.org", url.host)
        assertEquals(listOf("bot$TOKEN", "sendMessage"), url.segments)
        assertEquals("4242", url.parameters["chat_id"])
        assertEquals("Чек: https://receipt.test/doc-1", url.parameters["text"])
        assertTrue(journal.written.contains("Telegram delivered"))
    }

    @Test
    fun `чат не найден — отказ с ответом Bot API`() {
        val provider = ProviderStub(HttpStatusCode.BadRequest, """{"ok":false,"description":"Bad Request: chat not found"}""")

        val result = TelegramChannel(TOKEN, provider.clients, journal).send(receiptRequest(DeliveryChannel.TELEGRAM, destination = "1"))

        assertFalse(result.ok)
        assertEquals(DeliveryCodes.PROVIDER_REJECTED, result.code)
        assertTrue(result.message.orEmpty().contains("chat not found"))
    }

    @Test
    fun `токен бота из адреса не попадает ни в журнал, ни в отказ`() {
        val failure = IOException("too many redirects: https://api.telegram.org/bot$TOKEN/sendMessage")
        val provider = ProviderStub(failure = failure)
        val echo = ProviderStub(HttpStatusCode.NotFound, "no route https://api.telegram.org/bot$TOKEN/sendMessage")

        val failed = TelegramChannel(TOKEN, provider.clients, journal).send(receiptRequest(DeliveryChannel.TELEGRAM, destination = "1"))
        val rejected = TelegramChannel(TOKEN, echo.clients, journal).send(receiptRequest(DeliveryChannel.TELEGRAM, destination = "1"))

        assertFalse(failed.message.orEmpty().contains(TOKEN))
        assertFalse(rejected.message.orEmpty().contains(TOKEN))
        assertTrue(rejected.message.orEmpty().contains("/bot***/sendMessage"))
        assertFalse(journal.written.contains(TOKEN), journal.written)
        assertFalse(journal.written.contains("123456"), journal.written)
    }

    @Test
    fun `без чата отказ без запроса`() {
        val provider = ProviderStub()

        val result = TelegramChannel(TOKEN, provider.clients, journal).send(receiptRequest(DeliveryChannel.TELEGRAM, destination = null))

        assertEquals("DELIVERY_TELEGRAM_DESTINATION_REQUIRED", result.code)
        assertTrue(provider.requests.isEmpty())
    }

    private companion object {
        const val TOKEN = "123456:AAH-do-not-print-me"
    }
}
