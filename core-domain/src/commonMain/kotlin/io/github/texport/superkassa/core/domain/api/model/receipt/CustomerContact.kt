package io.github.texport.superkassa.core.domain.api.model.receipt

import kotlinx.serialization.Serializable

/**
 * Контакт покупателя, по которому ему уходит чек.
 *
 * Чек уходит в тот включённый канал доставки, что соответствует виду
 * контакта: телефон — SMS и WhatsApp, почта — электронная почта,
 * Telegram — Telegram. Нет контакта нужного вида — в этот канал чек
 * не ставится. Это персональные данные: в журнал они не пишутся,
 * и [toString] их не раскрывает.
 *
 * @property phone телефон покупателя.
 * @property email почта покупателя.
 * @property telegram чат покупателя в Telegram.
 */
@Serializable
data class CustomerContact(
    val phone: String? = null,
    val email: String? = null,
    val telegram: String? = null
) {
    /** Куда отправить чек по каналу [channel]; `null` — такого контакта покупатель не оставил. */
    fun destinationFor(channel: String): String? = when (channel.uppercase()) {
        SMS, WHATSAPP -> phone
        EMAIL -> email
        TELEGRAM -> telegram
        else -> null
    }?.takeIf { it.isNotBlank() }

    override fun toString(): String = "CustomerContact(***)"

    private companion object {
        const val SMS = "SMS"
        const val WHATSAPP = "WHATSAPP"
        const val EMAIL = "EMAIL"
        const val TELEGRAM = "TELEGRAM"
    }
}
