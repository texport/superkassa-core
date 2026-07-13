package io.github.texport.superkassa.core.presentation.api.model.ofd

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Сведения о единичной номенклатурной позиции, полученной из ОФД.
 */
@Serializable
@Schema(description = "Сведения о номенклатурной позиции")
data class NomenclatureItemResponse(
    @Schema(description = "Внутренний идентификатор позиции в каталоге НКТ", example = "639308")
    val id: Long,
    @Schema(description = "Штрихкод товара", example = "5449000176431")
    val barcode: String,
    @Schema(description = "Наименование товара на русском языке", example = "Напиток Piko Pulpy апельсин 0,5л")
    val name: String,
    @Schema(description = "Наименование товара на казахском языке", example = "Напиток Piko Pulpy апельсин 0,5л")
    val nameKk: String?,
    @Schema(description = "Глобальный номер товарной позиции NTIN", example = "0200091550792")
    val ntin: String?,
    @Schema(description = "Рекомендованная цена продажи (в тенге)", example = "0.0")
    val price: Double,
    @Schema(description = "Код единицы измерения (ОКЕИ)", example = "166")
    val measureUnitCode: String?,
    @Schema(description = "Группа НДС товара (если определена)", example = "NO_VAT")
    val vatGroup: String?
)
