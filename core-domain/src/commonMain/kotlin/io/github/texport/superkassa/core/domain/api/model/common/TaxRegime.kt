package io.github.texport.superkassa.core.domain.api.model.common

import kotlinx.serialization.Serializable

/**
 * Режимы налогообложения кассового ядра (ККМ).
 */
@Serializable
enum class TaxRegime {
    /** Неплательщик НДС. */
    NO_VAT,

    /** Плательщик НДС. */
    VAT_PAYER,

    /** Смешанный режим налогообложения. */
    MIXED
}
