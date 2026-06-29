package kz.mybrain.superkassa.core.domain.model.settings

import kotlinx.serialization.Serializable

/**
 * Настройки провайдера отправки Email уведомлений.
 */
@Serializable
data class EmailProviderSettings(
    val host: String = "localhost",
    val port: Int = 587,
    val user: String? = null,
    val password: String? = null,
    val from: String = "noreply@local"
)
