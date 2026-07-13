package io.github.texport.superkassa.core.presentation.api.model.kkm

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Запрос на обновление параметров черновика ККМ.
 */
@Serializable
@Schema(description = "Запрос на обновление параметров черновика ККМ")
data class KkmDraftUpdateRequest(
    @Schema(description = "ID провайдера ОФД", example = "kazakhtelecom")
    val ofdId: String? = null,
    @Schema(description = "Среда ОФД (test/prod)", example = "test")
    val ofdEnvironment: String? = null,
    @Schema(description = "Системный ID ККМ в ОФД", example = "sys-123")
    val ofdSystemId: String? = null,
    @Schema(description = "Заводской номер ККМ", example = "SWK-0001")
    val factoryNumber: String? = null,
    @Schema(description = "Год выпуска ККМ", example = "2026")
    val manufactureYear: Int? = null
)
