package kz.mybrain.superkassa.core.domain.model.settings

/**
 * Настройки провайдера отправки SMS уведомлений.
 */
data class SmsProviderSettings(
    val providerUrl: String? = null,
    val apiKey: String? = null
)
