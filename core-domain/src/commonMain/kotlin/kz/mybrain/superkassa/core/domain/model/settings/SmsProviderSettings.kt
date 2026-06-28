package kz.mybrain.superkassa.core.domain.model.settings

import kotlinx.serialization.Serializable

@Serializable
data /**
 * Настройки провайдера отправки SMS уведомлений.
 */
class SmsProviderSettings(
    val providerUrl: String? = null,
    val apiKey: String? = null
)
