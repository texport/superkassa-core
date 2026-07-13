package io.github.texport.superkassa.core.domain.api.model.common

/**
 * Режимы налогообложения кассового ядра (ККМ).
 */
enum class TaxRegime {
    /** Неплательщик НДС. */
    NO_VAT,

    /** Плательщик НДС. */
    VAT_PAYER,

    /** Смешанный режим налогообложения. */
    MIXED
}
