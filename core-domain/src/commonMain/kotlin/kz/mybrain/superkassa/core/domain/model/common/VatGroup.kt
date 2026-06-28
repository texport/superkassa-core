package kz.mybrain.superkassa.core.domain.model.common

import kotlinx.serialization.Serializable

/**
 * Группы ставок НДС.
 *
 * @property percent Процентная ставка НДС.
 * @property percentThousandths Ставка НДС в тысячных долях (например, 12% = 12000).
 * @property description Описание налоговой ставки.
 * @property taxTypeCode Код типа налога для протокола ОФД.
 */
@Serializable
enum class VatGroup(
    val percent: Int,
    val percentThousandths: Int,
    val description: String,
    val taxTypeCode: String
) {
    /** Без НДС */
    NO_VAT(0, 0, "Без НДС", "TAX_TYPE_NO_VAT"),
    /** НДС 0% */
    VAT_0(0, 0, "НДС 0%", "TAX_TYPE_VAT_0"),
    /** НДС 5% */
    VAT_5(5, 5_000, "НДС 5%", "TAX_TYPE_VAT_5"),
    /** НДС 10% */
    VAT_10(10, 10_000, "НДС 10%", "TAX_TYPE_VAT_10"),
    /** НДС 16% */
    VAT_16(16, 16_000, "НДС 16%", "TAX_TYPE_VAT_16")
}
