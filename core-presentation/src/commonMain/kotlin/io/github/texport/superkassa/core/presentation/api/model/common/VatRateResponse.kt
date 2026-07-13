package io.github.texport.superkassa.core.presentation.api.model.common

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Ставка НДС.
 */
@Serializable
@Schema(description = "Сведения о применяемой ставке НДС")
data class VatRateResponse(
    @Schema(description = "Буквенно-символьный код ставки НДС (например, VAT_12, NO_VAT)", example = "VAT_12")
    val code: String,
    @Schema(description = "Процентная ставка НДС (в целых процентах)", example = "12")
    val percent: Int,
    @Schema(description = "Текстовое описание ставки НДС", example = "НДС 12%")
    val description: String
)
