package io.github.texport.superkassa.core.presentation.api.model.receipt

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Типы шаблонов/макетов чеков.
 */
@Serializable
@Schema(description = "Тип макета чека")
enum class ReceiptLayoutType {
    /** Чековая лента шириной 80мм */
    @Schema(description = "Чековая лента 80мм")
    TAPE_80MM,

    /** Чековая лента шириной 58мм */
    @Schema(description = "Чековая лента 58мм")
    TAPE_58MM,

    /** Полноразмерный документ (A4/A5) */
    @Schema(description = "Полноэкранный (A4/А5)")
    FULLSCREEN
}
