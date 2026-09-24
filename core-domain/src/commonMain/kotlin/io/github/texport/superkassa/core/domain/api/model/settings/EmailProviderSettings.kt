package io.github.texport.superkassa.core.domain.api.model.settings

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
) {
    /** Без пароля почты: см. [hiddenSecret]. */
    override fun toString(): String =
        "EmailProviderSettings(host=$host, port=$port, user=$user, password=${hiddenSecret(password)}, from=$from)"
}
