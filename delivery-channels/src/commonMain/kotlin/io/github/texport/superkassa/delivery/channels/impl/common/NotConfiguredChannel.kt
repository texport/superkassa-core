package io.github.texport.superkassa.delivery.channels.impl.common

import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.delivery.api.model.DeliveryChannel
import io.github.texport.superkassa.delivery.api.model.DeliveryRequest
import io.github.texport.superkassa.delivery.api.model.DeliveryResult
import io.github.texport.superkassa.delivery.api.port.DeliveryPort

/**
 * Канал, который выбран, но не настроен: отвечает отказом с причиной.
 *
 * На его месте прежде стояла заглушка с успехом: чек не уходил,
 * а касса записывала его доставленным.
 */
internal class NotConfiguredChannel(
    override val channel: DeliveryChannel,
    private val journal: Journal
) : DeliveryPort {
    override fun send(request: DeliveryRequest): DeliveryResult {
        val code = DeliveryCodes.notConfigured(channel)
        journal.warn("${channel.title} refused: documentId=${request.documentId}, code=$code")
        return refusal(code, CoreStrings.deliveryChannelNotConfigured(channel.title))
    }
}
