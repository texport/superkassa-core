package io.github.texport.superkassa.delivery.channels.impl.http

import io.github.texport.superkassa.delivery.api.model.DeliveryChannel
import io.github.texport.superkassa.delivery.api.model.DeliveryRequest
import io.github.texport.superkassa.delivery.channels.impl.common.Journal
import io.github.texport.superkassa.delivery.channels.impl.common.receiptText
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import io.ktor.http.encodeURLParameter

/**
 * SMS через HTTP-шлюз: `GET` по адресу-шаблону, как у узла.
 *
 * Номер и текст подставляются закодированными: `+` в номере без кодирования
 * шлюз прочёл бы как пробел.
 *
 * @param providerUrl адрес шлюза с местами `{phone}` и `{text}`.
 * @param apiKey ключ шлюза для заголовка `Authorization: Bearer`; `null` — без заголовка.
 */
internal class SmsChannel(
    private val providerUrl: String,
    private val apiKey: String?,
    clients: HttpClients,
    journal: Journal
) : HttpChannel(DeliveryChannel.SMS, clients, journal, listOf(apiKey)) {

    override fun HttpRequestBuilder.describe(destination: String, request: DeliveryRequest) {
        method = HttpMethod.Get
        url(
            providerUrl
                .replace(PHONE, destination.encodeURLParameter())
                .replace(TEXT, receiptText(request).encodeURLParameter())
        )
        apiKey?.let { bearerAuth(it) }
    }

    private companion object {
        const val PHONE = "{phone}"
        const val TEXT = "{text}"
    }
}
