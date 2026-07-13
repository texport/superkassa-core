package io.github.texport.superkassa.core.domain.api.model.settings

/**
 * Настройки провайдера доставки уведомлений в Telegram.
 */
data class TelegramProviderSettings(
    val botToken: String? = null
)
