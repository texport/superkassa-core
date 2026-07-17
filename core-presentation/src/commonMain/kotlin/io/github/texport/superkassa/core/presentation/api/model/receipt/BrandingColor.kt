package io.github.texport.superkassa.core.presentation.api.model.receipt

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Цвета брендирования печатных форм чеков.
 *
 * @property hexCode HEX-код цвета.
 */
@Serializable
@Schema(description = "Цвет брендирования")
enum class BrandingColor(val hexCode: String) {
    /** Темно-фиолетовый (По умолчанию) */
    @Schema(description = "Темно-фиолетовый")
    DARK_PURPLE("#1F1C2C"),

    /** Глубокий черный */
    @Schema(description = "Глубокий черный")
    DEEP_BLACK("#000000"),

    /** Синий */
    @Schema(description = "Синий")
    BLUE("#007AFF"),

    /** Зеленый */
    @Schema(description = "Зеленый")
    GREEN("#34C759"),

    /** Оранжевый */
    @Schema(description = "Оранжевый")
    ORANGE("#FF9500"),

    /** Красный */
    @Schema(description = "Красный")
    RED("#FF3B30"),

    /** Фиолетовый */
    @Schema(description = "Фиолетовый")
    PURPLE("#5856D6")
}
