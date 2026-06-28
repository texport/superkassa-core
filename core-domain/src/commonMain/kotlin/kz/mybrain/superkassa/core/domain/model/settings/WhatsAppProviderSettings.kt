package kz.mybrain.superkassa.core.domain.model.settings

import kotlinx.serialization.Serializable

@Serializable
data /**
 * Настройки провайдера доставки уведомлений в WhatsApp.
 */
class WhatsAppProviderSettings(
    val accessToken: String? = null,
    val phoneNumberId: String? = null
)
