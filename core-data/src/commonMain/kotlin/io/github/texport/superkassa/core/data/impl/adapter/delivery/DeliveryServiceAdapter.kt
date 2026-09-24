package io.github.texport.superkassa.core.data.impl.adapter.delivery

import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryFailure
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryOutcome
import io.github.texport.superkassa.core.domain.impl.logging.getLogger
import io.github.texport.superkassa.core.domain.api.port.integration.DeliveryPort
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.delivery.api.DeliveryServiceApi
import io.github.texport.superkassa.delivery.api.model.DeliveryChannel
import io.github.texport.superkassa.delivery.api.model.DeliveryRequest
import io.github.texport.superkassa.delivery.api.toOutcome
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryRequest as CoreDeliveryRequest

/**
 * Адаптер доставки документов во внешние каналы на базе библиотеки superkassa-delivery.
 * Настраивается и создается как Spring-бин в superkassa-server.
 */
internal class DeliveryServiceAdapter(
    private val deliveryService: DeliveryServiceApi
) : DeliveryPort {
    private val logger = getLogger(DeliveryServiceAdapter::class)

    /**
     * Отправляет фискальный документ покупателю (на печать, email, Telegram, SMS или WhatsApp).
     * @param request Запрос на доставку, содержащий канал, реквизиты получателя и полезную нагрузку.
     * @return true, если отправка завершилась успешно; false в случае сбоя.
     */
    override fun deliver(request: CoreDeliveryRequest): Boolean = send(request).delivered

    /**
     * Отправляет документ и передаёт код, текст и повторяемость отказа канала.
     * Незнакомый канал уходит на печать — так узел вёл себя всегда.
     */
    override fun send(request: CoreDeliveryRequest): DeliveryOutcome {
        val channel = DeliveryChannel.entries.firstOrNull { it.name == request.channel.uppercase() } ?: run {
            logger.warn("Unknown delivery channel {}, using PRINT", request.channel)
            DeliveryChannel.PRINT
        }
        return runCatching { deliveryService.deliver(request.toChannelRequest(channel)) }
            .map { it.toOutcome(channel.name) }
            .getOrElse { failure ->
                logger.warn("Failed to deliver document {}: {}", request.documentId, failure::class.simpleName)
                val reason = failure::class.simpleName.orEmpty()
                DeliveryOutcome.failed(
                    DeliveryFailure(CHANNEL_FAILED, CoreStrings.deliveryChannelFailed(channel.name, reason)),
                    retryable = true
                )
            }
    }

    private fun CoreDeliveryRequest.toChannelRequest(channel: DeliveryChannel) = DeliveryRequest(
        cashboxId = kkmId,
        documentId = documentId,
        channel = channel,
        destination = destination,
        payloadUrl = payloadUrl,
        payloadBytes = payloadBytes,
        payloadType = payloadType
    )

    private companion object {
        const val CHANNEL_FAILED = "DELIVERY_CHANNEL_FAILED"
    }
}
