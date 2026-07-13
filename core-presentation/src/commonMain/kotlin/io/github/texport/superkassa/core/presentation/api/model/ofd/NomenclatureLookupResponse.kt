package io.github.texport.superkassa.core.presentation.api.model.ofd

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Результат поиска номенклатурной позиции в каталоге ОФД.
 */
@Serializable
@Schema(description = "Результат поиска номенклатурной позиции")
data class NomenclatureLookupResponse(
    @Schema(description = "Флаг успешности поиска", example = "true")
    val found: Boolean,
    @Schema(description = "Детали найденной позиции (если найдена)")
    val item: NomenclatureItemResponse?,
    @Schema(description = "Код результата обработки каталога ОФД", example = "0")
    val resultCode: Int,
    @Schema(description = "Текстовое описание результата", example = "OK")
    val resultText: String?
)
