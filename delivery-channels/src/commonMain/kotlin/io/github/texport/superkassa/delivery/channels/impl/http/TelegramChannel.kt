package io.github.texport.superkassa.delivery.channels.impl.http

import io.github.texport.superkassa.delivery.api.model.DeliveryChannel
import io.github.texport.superkassa.delivery.api.model.DeliveryRequest
import io.github.texport.superkassa.delivery.channels.impl.common.Journal
import io.github.texport.superkassa.delivery.channels.impl.common.receiptText
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import io.ktor.http.appendPathSegments
import io.ktor.http.takeFrom

/**
 * Telegram через Bot API: `POST /bot<токен>/sendMessage` с чатом и текстом
 * в параметрах адреса, как у узла.
 *
 * @param botToken токен бота; в адресе запроса, поэтому прячется в журнале.
 */
internal class TelegramChannel(
    private val botToken: String,
    clients: HttpClients,
    journal: Journal
) : HttpChannel(DeliveryChannel.TELEGRAM, clients, journal, listOf(botToken)) {

    override fun HttpRequestBuilder.describe(destination: String, request: DeliveryRequest) {
        method = HttpMethod.Post
        url {
            takeFrom(API)
            appendPathSegments("bot$botToken", "sendMessage")
            parameters.append("chat_id", destination)
            parameters.append("text", receiptText(request))
        }
    }

    private companion object {
        const val API = "https://api.telegram.org"
    }
}
