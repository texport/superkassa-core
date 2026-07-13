package io.github.texport.superkassa.core.presentation.api.model.kkm

import kotlinx.serialization.Serializable

@Serializable
enum class TaxRegime {
    NO_VAT,
    VAT_PAYER,
    MIXED
}
