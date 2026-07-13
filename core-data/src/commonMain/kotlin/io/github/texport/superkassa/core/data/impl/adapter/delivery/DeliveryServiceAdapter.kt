package io.github.texport.superkassa.core.data.impl.adapter.delivery

import io.github.texport.superkassa.core.domain.impl.logging.getLogger
import io.github.texport.superkassa.core.domain.api.port.integration.DeliveryPort
import io.github.texport.superkassa.delivery.api.DeliveryServiceApi
import io.github.texport.superkassa.delivery.api.model.DeliveryChannel
import io.github.texport.superkassa.delivery.api.model.DeliveryRequest
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
     * Адаптирует доменный запрос [CoreDeliveryRequest] в системный запрос библиотеки доставки.
     * @param request Запрос на доставку, содержащий канал, реквизиты получателя и полезную нагрузку.
     * @return true, если отправка завершилась успешно; false в случае сбоя.
     */
    override fun deliver(request: CoreDeliveryRequest): Boolean {
        return try {
            // Маппим строковый канал в перечисление DeliveryChannel из внешней библиотеки
            val channel = try {
                DeliveryChannel.valueOf(request.channel.uppercase())
            } catch (e: IllegalArgumentException) {
                logger.warn("Unknown delivery channel: ${request.channel}, using PRINT", e)
                DeliveryChannel.PRINT
            }
            // Вызываем внешний сервис доставки
            val result = deliveryService.deliver(
                DeliveryRequest(
                    cashboxId = request.kkmId,
                    documentId = request.documentId,
                    channel = channel,
                    destination = request.destination,
                    payloadUrl = request.payloadUrl,
                    payloadBytes = request.payloadBytes
                )
            )
            result.ok
        } catch (ex: Exception) {
            logger.error("Failed to deliver document: ${request.documentId} for KKM: ${request.kkmId}", ex)
            false
        }
    }
}
