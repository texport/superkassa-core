package kz.mybrain.superkassa.core.domain.model.settings

/**
 * Настройки провайдера отправки Email уведомлений.
 */
data class EmailProviderSettings(
    val host: String = "localhost",
    val port: Int = 587,
    val user: String? = null,
    val password: String? = null,
    val from: String = "noreply@local"
)
