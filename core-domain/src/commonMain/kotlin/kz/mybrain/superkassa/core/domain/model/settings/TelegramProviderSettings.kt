package kz.mybrain.superkassa.core.domain.model.settings

import kotlinx.serialization.Serializable

@Serializable
data /**
 * Настройки провайдера доставки уведомлений в Telegram.
 */
class TelegramProviderSettings(
    val botToken: String? = null
)
