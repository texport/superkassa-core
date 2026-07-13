package io.github.texport.superkassa.core.presentation.api.model.common

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Сгенерированный заводской номер ККМ.
 */
@Serializable
@Schema(description = "Сгенерированный заводской номер и год выпуска ККМ")
data class FactoryNumberResponse(
    @Schema(description = "Заводской номер ККМ", example = "KZT2026000001")
    val factoryNumber: String,

    @Schema(description = "Год выпуска ККМ", example = "2026")
    val manufactureYear: Int
)
