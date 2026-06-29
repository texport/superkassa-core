package kz.mybrain.superkassa.core.domain.model.receipt

import kotlinx.serialization.Serializable

/**
 * Шаблоны разметки (ширина ленты) печатной формы чека.
 */
@Suppress("unused")
@Serializable
enum class ReceiptLayoutType {
    /** Узкая лента 80 мм. */
    TAPE_80MM,

    /** Узкая лента 58 мм. */
    TAPE_58MM,

    /** Полноэкранный формат (например, для отображения на мониторах). */
    FULLSCREEN
}
