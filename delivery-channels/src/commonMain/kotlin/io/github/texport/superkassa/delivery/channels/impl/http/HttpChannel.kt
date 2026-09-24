package io.github.texport.superkassa.delivery.channels.impl.http

import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.delivery.api.model.DeliveryChannel
import io.github.texport.superkassa.delivery.api.model.DeliveryRequest
import io.github.texport.superkassa.delivery.api.model.DeliveryResult
import io.github.texport.superkassa.delivery.api.port.DeliveryPort
import io.github.texport.superkassa.delivery.channels.impl.common.DeliveryCodes
import io.github.texport.superkassa.delivery.channels.impl.common.Journal
import io.github.texport.superkassa.delivery.channels.impl.common.MaskedJournal
import io.github.texport.superkassa.delivery.channels.impl.common.Secrets
import io.github.texport.superkassa.delivery.channels.impl.common.reasonOf
import io.github.texport.superkassa.delivery.channels.impl.common.refusal
import io.github.texport.superkassa.delivery.channels.impl.common.title
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.request
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException

/**
 * Канал доставки через HTTP API провайдера.
 *
 * Успех — только ответ 2xx. Иной ответ — отказ с кодом ответа и его текстом,
 * сбой связи — отказ с родом сбоя. Ключи канала прячутся и в журнале,
 * и в тексте отказа.
 *
 * @param secretValues ключи канала, которые не должны уйти ни в журнал, ни наружу.
 */
internal abstract class HttpChannel(
    final override val channel: DeliveryChannel,
    private val clients: HttpClients,
    journal: Journal,
    secretValues: List<String?>
) : DeliveryPort {
    private val secrets = Secrets(secretValues)
    private val journal = MaskedJournal(journal, secrets)

    /** Описывает запрос провайдеру: метод, адрес, заголовки и тело. */
    protected abstract fun HttpRequestBuilder.describe(destination: String, request: DeliveryRequest)

    final override fun send(request: DeliveryRequest): DeliveryResult {
        val destination = request.destination?.takeIf { it.isNotBlank() }
            ?: return refused(request, DeliveryCodes.recipientRequired(channel))
        return try {
            judge(request, runBlocking { exchange(destination, request) })
        } catch (e: IOException) {
            failed(request, e)
        } catch (e: IllegalArgumentException) {
            failed(request, e)
        } catch (e: IllegalStateException) {
            failed(request, e)
        }
    }

    private suspend fun exchange(destination: String, request: DeliveryRequest): Reply =
        clients.open().use { client ->
            val response = client.request { describe(destination, request) }
            Reply(response.status.isSuccess(), response.status.value, response.bodyAsText())
        }

    private fun judge(request: DeliveryRequest, reply: Reply): DeliveryResult {
        if (reply.success) {
            journal.info("${channel.title} delivered: documentId=${request.documentId}")
            return DeliveryResult(ok = true)
        }
        val answer = secrets.mask(reply.body).take(ANSWER_LIMIT)
        val line = "${channel.title} rejected: documentId=${request.documentId}, status=${reply.status}, answer=$answer"
        journal.warn(line)
        val message = CoreStrings.deliveryProviderRejected(channel.title, reply.status, answer)
        return refusal(DeliveryCodes.PROVIDER_REJECTED, message)
    }

    private fun failed(request: DeliveryRequest, failure: Exception): DeliveryResult {
        val reason = reasonOf(failure)
        journal.warn("${channel.title} failed: documentId=${request.documentId}, reason=$reason")
        return refusal(DeliveryCodes.CHANNEL_FAILED, CoreStrings.deliveryChannelFailed(channel.title, reason))
    }

    private fun refused(request: DeliveryRequest, code: String): DeliveryResult {
        journal.warn("${channel.title} refused: documentId=${request.documentId}, code=$code")
        return refusal(code, CoreStrings.deliveryRecipientRequired(channel.title), retryable = false)
    }

    private class Reply(val success: Boolean, val status: Int, val body: String)

    private companion object {
        /** Ответ провайдера бывает страницей целиком; для причины хватает начала. */
        const val ANSWER_LIMIT = 200
    }
}
