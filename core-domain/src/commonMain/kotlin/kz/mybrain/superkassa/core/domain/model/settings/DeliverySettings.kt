package kz.mybrain.superkassa.core.domain.model.settings

import kotlinx.serialization.Serializable

@Serializable
data /**
 * Настройки каналов и провайдеров доставки фискальных документов.
 */
class DeliverySettings(
    val print: PrintDeliverySettings? = null,
    val channels: List<DeliveryChannelSettings> = emptyList(),
    val email: EmailProviderSettings? = null,
    val sms: SmsProviderSettings? = null,
    val telegram: TelegramProviderSettings? = null,
    val whatsapp: WhatsAppProviderSettings? = null
)
