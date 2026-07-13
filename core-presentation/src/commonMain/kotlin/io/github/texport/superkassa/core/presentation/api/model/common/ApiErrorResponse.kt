package io.github.texport.superkassa.core.presentation.api.model.common

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Единый формат ответа с ошибкой от API.
 */
@Serializable
@Schema(description = "Ответ с ошибкой")
data class ApiErrorResponse(
    @Schema(description = "Код ошибки", example = "VALIDATION_ERROR") val code: String,
    @Schema(
        description = "Сообщение об ошибке",
        example = "Validation failed for argument [0]..."
    )
    val message: String,
    @Schema(
        description = "Детали ошибки (опционально)",
        example = "Field 'pin' must not be blank"
    )
    val details: String? = null
)
