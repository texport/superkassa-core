package io.github.texport.superkassa.core.presentation.api.model.ofd

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import io.github.texport.superkassa.core.presentation.api.annotations.NotBlank
import kotlinx.serialization.Serializable

/**
 * Запрос для получения параметров авторизации ОФД.
 */
@Serializable
@Schema(description = "Запрос информации об авторизации в ОФД")
data class OfdAuthInfoRequest(
    @Schema(description = "Идентификатор ККМ", example = "kkm-uuid")
    @field:NotBlank
    val kkmId: String
)
