package io.github.texport.superkassa.core.presentation.api.model.receipt

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Контакт покупателя, по которому ему уходит чек.
 *
 * Чек уходит во включённый канал доставки того вида, что указан: телефон —
 * SMS и WhatsApp, почта — электронная почта, Telegram — Telegram. Без
 * контакта чек покупателю не отправляется.
 */
@Serializable
@Schema(description = "Контакт покупателя для отправки чека. Персональные данные: в журнал не пишутся.")
data class CustomerContactRequest(
    @Schema(description = "Телефон покупателя: чек уходит по SMS и в WhatsApp", example = "+77017654321")
    val phone: String? = null,
    @Schema(description = "Почта покупателя: чек уходит письмом", example = "buyer@example.kz")
    val email: String? = null,
    @Schema(description = "Чат покупателя в Telegram: чек уходит сообщением бота", example = "123456789")
    val telegram: String? = null
) {
    override fun toString(): String = "CustomerContactRequest(***)"
}
