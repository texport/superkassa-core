package io.github.texport.superkassa.delivery.channels.impl.http

import io.github.texport.superkassa.delivery.api.model.DeliveryChannel
import io.github.texport.superkassa.delivery.api.model.DeliveryRequest
import io.github.texport.superkassa.delivery.channels.impl.common.Journal
import io.github.texport.superkassa.delivery.channels.impl.common.receiptText
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.appendPathSegments
import io.ktor.http.takeFrom
import io.ktor.http.content.TextContent
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * WhatsApp через Cloud API: `POST /v18.0/<номер отправителя>/messages`
 * с текстовым сообщением, как у узла.
 *
 * Номер покупателя уходит одними цифрами: Cloud API не принимает
 * ни `+`, ни скобок, ни пробелов.
 *
 * @param accessToken ключ доступа Cloud API.
 * @param phoneNumberId идентификатор номера отправителя.
 */
internal class WhatsAppChannel(
    private val accessToken: String,
    private val phoneNumberId: String,
    clients: HttpClients,
    journal: Journal
) : HttpChannel(DeliveryChannel.WHATSAPP, clients, journal, listOf(accessToken)) {

    override fun HttpRequestBuilder.describe(destination: String, request: DeliveryRequest) {
        method = HttpMethod.Post
        url {
            takeFrom(API)
            appendPathSegments(VERSION, phoneNumberId, "messages")
        }
        bearerAuth(accessToken)
        setBody(TextContent(message(destination, request), ContentType.Application.Json))
    }

    private fun message(destination: String, request: DeliveryRequest): String = buildJsonObject {
        put("messaging_product", "whatsapp")
        put("to", destination.filter { it.isDigit() })
        put("type", "text")
        putJsonObject("text") { put("body", receiptText(request)) }
    }.toString()

    private companion object {
        const val API = "https://graph.facebook.com"
        const val VERSION = "v18.0"
    }
}
