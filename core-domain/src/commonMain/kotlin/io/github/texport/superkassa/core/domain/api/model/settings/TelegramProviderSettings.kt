package io.github.texport.superkassa.core.domain.api.model.settings

import kotlinx.serialization.Serializable

/**
 * Настройки провайдера доставки уведомлений в Telegram.
 */
@Serializable
data class TelegramProviderSettings(
    val botToken: String? = null
)
