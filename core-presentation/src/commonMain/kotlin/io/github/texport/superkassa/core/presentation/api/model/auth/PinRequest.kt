package io.github.texport.superkassa.core.presentation.api.model.auth

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Запрос с подтверждением PIN-кода.
 */
@Serializable
@Schema(description = "Запрос с подтверждением ПИН-кода")
data class PinRequest(
    @Schema(
        description = "Не используется. ПИН-код передается только в заголовке Authorization",
        example = "deprecated"
    )
    val _unused: String? = null
)
