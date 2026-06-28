package kz.mybrain.superkassa.core.data.adapter

import io.github.texport.superkassa.delivery.application.service.DeliveryService
import io.github.texport.superkassa.delivery.domain.model.DeliveryChannel
import io.github.texport.superkassa.delivery.domain.model.DeliveryRequest
import kz.mybrain.superkassa.core.domain.model.delivery.DeliveryRequest as CoreDeliveryRequest
import kz.mybrain.superkassa.core.domain.port.DeliveryPort
import org.slf4j.LoggerFactory

/**
 * Адаптер доставки документов в внешние каналы на базе библиотеки superkassa-delivery.
 * Настраивается и создается как Spring-бин в superkassa-server.
 */
class DeliveryServiceAdapter(
    private val deliveryService: DeliveryService
) : DeliveryPort {
    private val logger = LoggerFactory.getLogger(DeliveryServiceAdapter::class.java)

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
                logger.warn("Unknown delivery channel: ${request.channel}, using PRINT")
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
