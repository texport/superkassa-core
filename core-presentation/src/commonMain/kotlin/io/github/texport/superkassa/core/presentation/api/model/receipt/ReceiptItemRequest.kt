package io.github.texport.superkassa.core.presentation.api.model.receipt

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import io.github.texport.superkassa.core.presentation.api.annotations.DecimalMax
import io.github.texport.superkassa.core.presentation.api.annotations.DecimalMin
import io.github.texport.superkassa.core.presentation.api.annotations.Max
import io.github.texport.superkassa.core.presentation.api.annotations.NotBlank
import io.github.texport.superkassa.core.presentation.api.annotations.NotNull
import io.github.texport.superkassa.core.presentation.api.annotations.Positive
import io.github.texport.superkassa.core.presentation.api.annotations.Size
import io.github.texport.superkassa.core.domain.api.annotations.ItemNameValid
import kotlinx.serialization.Serializable

/**
 * Товарная позиция чека.
 */
@Serializable
@Schema(description = "Позиция чека. Сумма позиции вычисляется на сервере по цене, количеству, скидке и наценке.")
data class ReceiptItemRequest(
    @Schema(description = "Штрихкод товара (опционально)", example = "4607011417556")
    val barcode: String? = null,
    @Schema(description = "Список акцизных марок (протокол ОФД list_excise_stamp)", example = "[\"12345678901234\"]")
    val listExciseStamp: List<String>? = null,
    @Schema(description = "Наценка на позицию: процент (0–100). Взаимоисключающе с markupSum.", example = "0")
    @field:DecimalMin("0", message = "Процент наценки не может быть отрицательным")
    @field:DecimalMax("100", message = "Процент наценки не может быть больше 100")
    val markupPercent: Double? = null,
    @Schema(description = "Наценка на позицию: сумма в тенге. Взаимоисключающе с markupPercent.", example = "0")
    @field:DecimalMin("0", message = "Сумма наценки не может быть отрицательной")
    val markupSum: Double? = null,
    @Schema(description = "Скидка на позицию: процент (0–100). Взаимоисключающе с discountSum.", example = "10")
    @field:DecimalMin("0", message = "Процент скидки не может быть отрицательным")
    @field:DecimalMax("100", message = "Процент скидки не может быть больше 100")
    val discountPercent: Double? = null,
    @Schema(description = "Скидка на позицию: сумма в тенге. Взаимоисключающе с discountPercent.", example = "30.10")
    @field:DecimalMin("0", message = "Сумма скидки не может быть отрицательной")
    val discountSum: Double? = null,
    @Schema(
        description = "Код единицы измерения (ОКЕИ). Только код (796, 116...). По умолчанию — штука (796). См. GET /units-of-measurement.",
        example = "796"
    )
    val measureUnitCode: String? = null,
    @Schema(
        description = "Наименование товара/услуги. Не допускаются обобщённые названия: «Товар», «Продукты», «Товар один» и т.п.",
        example = "Хлеб белый нарезной",
        minLength = 3,
        maxLength = 128
    )
    @field:NotBlank(message = "Наименование обязательно")
    @field:Size(min = 3, max = 128)
    @ItemNameValid
    val name: String,
    @Schema(description = "НТИН (протокол ОФД ntin)", example = "123456789012")
    val ntin: String? = null,
    @Schema(description = "Цена за единицу (в тенге)", example = "150.50")
    @field:NotNull
    @field:DecimalMin("0.01", message = "Цена должна быть положительной")
    val price: Double,
    @Schema(description = "Количество", example = "2")
    @field:NotNull
    @field:Positive(message = "Количество должно быть больше 0")
    @field:Max(999_999_999)
    val quantity: Double,
    @Schema(
        description = "Группа НДС для позиции. Допустимые значения: NO_VAT, VAT_0, VAT_5, VAT_10, VAT_16. Если не указана — используется defaultVatGroup кассы.",
        allowableValues = ["NO_VAT", "VAT_0", "VAT_5", "VAT_10", "VAT_16"],
        example = "VAT_16"
    )
    val vatGroup: String? = null,
    @Schema(description = "Признак сторно (аннулирования) этой товарной позиции", example = "false")
    val isStorno: Boolean? = false
) {
    init {
        require(discountPercent == null || discountSum == null) {
            "Укажите скидку на позицию либо в процентах (discountPercent), либо суммой (discountSum), но не оба значения"
        }
        require(markupPercent == null || markupSum == null) {
            "Укажите наценку на позицию либо в процентах (markupPercent), либо суммой (markupSum), но не оба значения"
        }
    }
}
