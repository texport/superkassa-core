package io.github.texport.superkassa.core.presentation.api.model.common

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Единица измерения из справочника ИС ЭСФ.
 */
@Serializable
@Schema(description = "Справочная информация о единице измерения ИС ЭСФ")
data class UnitOfMeasurementResponse(
    @Schema(description = "Код единицы измерения по классификатору ИС ЭСФ", example = "796")
    val code: String,
    @Schema(description = "Краткое наименование единицы измерения", example = "шт")
    val nameShort: String,
    @Schema(description = "Полное наименование единицы измерения", example = "Штука")
    val nameFull: String
)
