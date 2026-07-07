package kz.mybrain.superkassa.core.domain.model.settings

/**
 * Настройки провайдера доставки уведомлений в Telegram.
 */
data class TelegramProviderSettings(
    val botToken: String? = null
)
