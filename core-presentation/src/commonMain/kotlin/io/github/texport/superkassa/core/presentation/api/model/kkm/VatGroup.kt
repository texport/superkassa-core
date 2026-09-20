package io.github.texport.superkassa.core.presentation.api.model.kkm

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Налоговые группы НДС.
 */
@Serializable
@Schema(description = "Группа НДС")
enum class VatGroup {
    /** Без НДС */
    @Schema(description = "Без НДС")
    NO_VAT,

    /** НДС 0% */
    @Schema(description = "НДС 0%")
    VAT_0,

    /** НДС 5% */
    @Schema(description = "НДС 5%")
    VAT_5,

    /** НДС 10% */
    @Schema(description = "НДС 10%")
    VAT_10,

    /** НДС 12%, ставка до 2026 года: нужна для возврата по старому чеку. */
    @Schema(description = "НДС 12%")
    VAT_12,

    /** НДС 16% */
    @Schema(description = "НДС 16%")
    VAT_16
}
