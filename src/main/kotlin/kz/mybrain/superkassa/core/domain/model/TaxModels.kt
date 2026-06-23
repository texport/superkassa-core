package kz.mybrain.superkassa.core.domain.model

import kotlinx.serialization.Serializable

/**
 * Налоговый режим ККМ.
 *
 * NO_VAT   - касса не является плательщиком НДС (только без НДС / освобожденный оборот).
 * VAT_PAYER - касса является плательщиком НДС (доступны ставки 0% и 16%).
 * MIXED    - смешанный режим (на будущее, для касс с разным типом оборотов).
 */
@Serializable
enum class TaxRegime {
    NO_VAT,
    VAT_PAYER,
    MIXED
}

@Serializable
enum class VatGroup(
    val percent: Int,
    val percentThousandths: Int,
    val description: String,
    val taxTypeCode: String
) {
    NO_VAT(0, 0, "Без НДС", "TAX_TYPE_NO_VAT"),
    VAT_0(0, 0, "НДС 0%", "TAX_TYPE_VAT_0"),
    VAT_5(5, 5_000, "НДС 5%", "TAX_TYPE_VAT_5"),
    VAT_10(10, 10_000, "НДС 10%", "TAX_TYPE_VAT_10"),
    VAT_16(16, 16_000, "НДС 16%", "TAX_TYPE_VAT_16")
}

