package io.github.texport.superkassa.core.presentation.api.model.kkm

import kotlinx.serialization.Serializable

@Serializable
enum class VatGroup {
    NO_VAT,
    VAT_0,
    VAT_5,
    VAT_10,
    VAT_16
}
