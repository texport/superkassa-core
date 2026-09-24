package io.github.texport.superkassa.core.domain.api.model.settings

import kotlinx.serialization.Serializable

/**
 * Настройки провайдера отправки SMS уведомлений.
 */
@Serializable
data class SmsProviderSettings(
    val providerUrl: String? = null,
    val apiKey: String? = null
) {
    /** Без ключа шлюза: см. [hiddenSecret]. */
    override fun toString(): String = "SmsProviderSettings(providerUrl=$providerUrl, apiKey=${hiddenSecret(apiKey)})"
}
