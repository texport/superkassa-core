package kz.mybrain.superkassa.core.domain.model.settings

import kotlinx.serialization.Serializable

@Serializable
data /**
 * Настройки провайдера отправки Email уведомлений.
 */
class EmailProviderSettings(
    val host: String = "localhost",
    val port: Int = 587,
    val user: String? = null,
    val password: String? = null,
    val from: String = "noreply@local"
)
