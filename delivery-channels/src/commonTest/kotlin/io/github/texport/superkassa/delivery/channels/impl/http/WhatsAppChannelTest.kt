package io.github.texport.superkassa.delivery.channels.impl.http

import io.github.texport.superkassa.delivery.api.model.DeliveryChannel
import io.github.texport.superkassa.delivery.channels.impl.common.DeliveryCodes
import io.github.texport.superkassa.delivery.channels.impl.common.RecordingJournal
import io.github.texport.superkassa.delivery.channels.impl.common.receiptRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WhatsAppChannelTest {
    private val journal = RecordingJournal()

    @Test
    fun `сообщение уходит в Cloud API текстом на номер из одних цифр`() {
        val provider = ProviderStub()
        val quoted = "https://receipt.test/doc-1?q=\"a\"\n"

        val result = WhatsAppChannel(TOKEN, "10987", provider.clients, journal)
            .send(receiptRequest(DeliveryChannel.WHATSAPP, payloadUrl = quoted))

        assertTrue(result.ok)
        assertEquals(HttpMethod.Post, provider.request.method)
        assertEquals("https://graph.facebook.com/v18.0/10987/messages", provider.request.url.toString())
        assertEquals("Bearer $TOKEN", provider.request.headers[HttpHeaders.Authorization])
        assertEquals(ContentType.Application.Json, provider.request.body.contentType)
        val message = Json.parseToJsonElement(provider.body).jsonObject
        assertEquals("whatsapp", message.getValue("messaging_product").jsonPrimitive.content)
        assertEquals("77770001122", message.getValue("to").jsonPrimitive.content)
        assertEquals("text", message.getValue("type").jsonPrimitive.content)
        assertEquals("Чек: $quoted", message.getValue("text").jsonObject.getValue("body").jsonPrimitive.content)
    }

    @Test
    fun `отказ Cloud API — отказ доставки, ключ в ответе спрятан`() {
        val provider = ProviderStub(HttpStatusCode.Unauthorized, """{"error":"invalid token $TOKEN"}""")

        val result = WhatsAppChannel(TOKEN, "10987", provider.clients, journal).send(receiptRequest(DeliveryChannel.WHATSAPP))

        assertFalse(result.ok)
        assertEquals(DeliveryCodes.PROVIDER_REJECTED, result.code)
        assertTrue(result.message.orEmpty().contains("401"))
        assertFalse(result.message.orEmpty().contains(TOKEN))
        assertFalse(journal.written.contains(TOKEN), journal.written)
    }

    @Test
    fun `без номера отказ без запроса`() {
        val provider = ProviderStub()

        val result = WhatsAppChannel(TOKEN, "10987", provider.clients, journal).send(receiptRequest(DeliveryChannel.WHATSAPP, destination = ""))

        assertEquals("DELIVERY_WHATSAPP_DESTINATION_REQUIRED", result.code)
        assertTrue(provider.requests.isEmpty())
    }

    private companion object {
        const val TOKEN = "EAAG-whatsapp-do-not-print"
    }
}
