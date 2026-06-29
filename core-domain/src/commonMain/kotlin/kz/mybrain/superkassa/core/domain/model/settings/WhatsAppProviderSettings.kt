package kz.mybrain.superkassa.core.domain.model.settings

import kotlinx.serialization.Serializable

/**
 * Настройки провайдера доставки уведомлений в WhatsApp.
 */
@Serializable
data class WhatsAppProviderSettings(
    val accessToken: String? = null,
    val phoneNumberId: String? = null
)
