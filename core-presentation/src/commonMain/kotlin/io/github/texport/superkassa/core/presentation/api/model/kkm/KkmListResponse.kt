package io.github.texport.superkassa.core.presentation.api.model.kkm

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

@Serializable
@Schema(description = "Список ККМ с общим количеством записей")
data class KkmListResponse(
    @Schema(description = "Список объектов ККМ")
    val items: List<KkmResponse>,
    @Schema(description = "Общее количество найденных записей ККМ", example = "1")
    val total: Int
)
