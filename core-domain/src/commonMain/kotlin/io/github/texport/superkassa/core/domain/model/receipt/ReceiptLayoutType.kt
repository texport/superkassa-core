package io.github.texport.superkassa.core.domain.model.receipt

/**
 * Шаблоны разметки (ширина ленты) печатной формы чека.
 */
enum class ReceiptLayoutType {
    /** Узкая лента 80 мм. */
    TAPE_80MM,

    /** Узкая лента 58 мм. */
    TAPE_58MM,

    /** Полноэкранный формат (например, для отображения на мониторах). */
    FULLSCREEN
}
