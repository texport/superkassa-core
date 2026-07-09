package io.github.texport.superkassa.delivery.impl

import io.github.texport.superkassa.delivery.api.DeliveryServiceApi
import io.github.texport.superkassa.delivery.api.model.DeliveryRequest
import io.github.texport.superkassa.delivery.api.model.DeliveryResult
import io.github.texport.superkassa.delivery.api.port.DeliveryPort

import io.github.texport.superkassa.core.string.api.CoreStrings

/**
 * Реализация сервиса доставки фискальных документов.
 */
internal class DeliveryServiceApiImpl(
    adapters: List<DeliveryPort>
) : DeliveryServiceApi {
    private val logger = getLogger(DeliveryServiceApiImpl::class)
    private val adapterByChannel = adapters.associateBy { it.channel }

    override fun deliver(request: DeliveryRequest): DeliveryResult {
        val adapter = adapterByChannel[request.channel]
            ?: return DeliveryResult(
                false,
                message = CoreStrings.noAdapterForChannel(request.channel.name).let {
                    "[EN] ${it.en} / [RU] ${it.ru} / [KK] ${it.kk}"
                }
            )
        logger.info("Delivery start. channel={}, documentId={}", request.channel, request.documentId)
        val result = adapter.send(request)
        if (result.ok) {
            logger.info("Delivery success. documentId={}", request.documentId)
        } else {
            logger.warn("Delivery failed. documentId={}, message={}", request.documentId, result.message)
        }
        return result
    }
}
