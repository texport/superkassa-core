package io.github.texport.superkassa.core.presentation.api.model.ofd

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import io.github.texport.superkassa.core.presentation.api.annotations.NotBlank
import kotlinx.serialization.Serializable

/**
 * Запрос для поиска номенклатурной позиции в каталоге ОФД.
 */
@Serializable
@Schema(description = "Запрос для поиска номенклатурной позиции")
data class NomenclatureLookupRequest(
    @Schema(description = "Идентификатор ККМ", example = "kkm-uuid")
    @field:NotBlank
    val kkmId: String,
    @Schema(description = "Штрихкод товара", example = "5449000176431")
    @field:NotBlank
    val barcode: String
)
