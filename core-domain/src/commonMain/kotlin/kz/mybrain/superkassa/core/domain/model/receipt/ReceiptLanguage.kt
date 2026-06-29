package kz.mybrain.superkassa.core.domain.model.receipt

import kotlinx.serialization.Serializable

/**
 * Поддерживаемые языки чеков.
 */
@Suppress("unused")
@Serializable
enum class ReceiptLanguage {
    /** Русский язык. */
    RU,

    /** Казахский язык. */
    KK,

    /** Смешанный (двуязычный) формат. */
    MIXED
}
