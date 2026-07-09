package io.github.texport.superkassa.core.domain.model.settings

/**
 * Настройки провайдера доставки уведомлений в Telegram.
 */
data class TelegramProviderSettings(
    val botToken: String? = null
)
