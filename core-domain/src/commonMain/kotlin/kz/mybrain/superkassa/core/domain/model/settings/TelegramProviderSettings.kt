package kz.mybrain.superkassa.core.domain.model.settings

import kotlinx.serialization.Serializable

/**
 * Настройки провайдера доставки уведомлений в Telegram.
 */
@Serializable
data class TelegramProviderSettings(
    val botToken: String? = null
)
