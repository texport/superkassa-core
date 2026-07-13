package io.github.texport.superkassa.core.presentation.api.model.ofd

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import io.github.texport.superkassa.core.presentation.api.annotations.NotBlank
import kotlinx.serialization.Serializable

/**
 * Запрос на обновление токена авторизации в ОФД.
 */
@Serializable
@Schema(description = "Запрос на обновление токена ОФД")
data class OfdTokenUpdateRequest(
    @Schema(description = "Новый токен ОФД", example = "new-token-123")
    @field:NotBlank
    val token: String
)
