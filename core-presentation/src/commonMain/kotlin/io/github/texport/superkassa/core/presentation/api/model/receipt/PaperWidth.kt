package io.github.texport.superkassa.core.presentation.api.model.receipt

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Варианты ширины чековой ленты.
 *
 * @property widthCode Кодовое обозначение ширины.
 */
@Serializable
@Schema(description = "Ширина чековой ленты")
enum class PaperWidth(val widthCode: String) {
    /** 58мм лента */
    @Schema(description = "58мм")
    WIDTH_58("58"),

    /** 80мм лента */
    @Schema(description = "80мм")
    WIDTH_80("80"),

    /** Полноэкранный вывод (A4/A5) */
    @Schema(description = "Полноэкранный")
    FULLSCREEN("FULLSCREEN")
}
