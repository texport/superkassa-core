package kz.mybrain.superkassa.core.domain.model.settings

/**
 * Настройки провайдера доставки уведомлений в WhatsApp.
 */
data class WhatsAppProviderSettings(
    val accessToken: String? = null,
    val phoneNumberId: String? = null
)
