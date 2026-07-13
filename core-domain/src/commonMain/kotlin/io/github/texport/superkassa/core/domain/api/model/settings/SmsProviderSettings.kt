package io.github.texport.superkassa.core.domain.api.model.settings

/**
 * Настройки провайдера отправки SMS уведомлений.
 */
data class SmsProviderSettings(
    val providerUrl: String? = null,
    val apiKey: String? = null
)
