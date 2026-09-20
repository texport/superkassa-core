package io.github.texport.superkassa.core.presentation.api.model.receipt

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Позиция проданного чека, как её вернуть.
 *
 * Отдельно от [ReceiptItemRequest]: там то, что кассир вводит, здесь —
 * то, что уже пробито. Количество в тысячных долях, как в протоколе,
 * а цена и сумма — точной десятичной записью.
 */
@Serializable
@Schema(description = "Позиция пробитого чека")
data class ReceiptItemView(
    @Schema(description = "Наименование") val name: String,
    @Schema(description = "Наименование на казахском") val nameKk: String? = null,
    @Schema(description = "Цена за единицу в тенге") val price: Decimal,
    @Schema(description = "Количество в тысячных долях") val quantityThousandths: Long,
    @Schema(description = "Сумма позиции в тенге") val sum: Decimal,
    @Schema(description = "Группа НДС") val vatGroup: String? = null,
    @Schema(description = "Код единицы измерения") val measureUnitCode: String? = null,
    @Schema(description = "Штрихкод") val barcode: String? = null,
    @Schema(description = "Сторнирована ли позиция") val isStorno: Boolean = false
)
