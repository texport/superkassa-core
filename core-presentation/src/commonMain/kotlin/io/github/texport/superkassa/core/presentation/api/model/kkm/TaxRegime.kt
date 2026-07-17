package io.github.texport.superkassa.core.presentation.api.model.kkm

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Налоговые режимы организации.
 */
@Serializable
@Schema(description = "Налоговый режим")
enum class TaxRegime {
    /** Без НДС */
    @Schema(description = "Без НДС")
    NO_VAT,

    /** Плательщик НДС */
    @Schema(description = "Плательщик НДС")
    VAT_PAYER,

    /** Смешанный налоговый режим */
    @Schema(description = "Смешанный режим")
    MIXED
}
