package kz.mybrain.superkassa.core.domain.model.settings

import kotlinx.serialization.Serializable

/**
 * Настройки каналов и провайдеров доставки фискальных документов.
 */
@Serializable
data class DeliverySettings(
    val print: PrintDeliverySettings? = null,
    val channels: List<DeliveryChannelSettings> = emptyList(),
    val email: EmailProviderSettings? = null,
    val sms: SmsProviderSettings? = null,
    val telegram: TelegramProviderSettings? = null,
    val whatsapp: WhatsAppProviderSettings? = null
)
