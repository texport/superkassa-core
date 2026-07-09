package io.github.texport.superkassa.core.domain.model.settings

/**
 * Настройки провайдера доставки уведомлений в WhatsApp.
 */
data class WhatsAppProviderSettings(
    val accessToken: String? = null,
    val phoneNumberId: String? = null
)
