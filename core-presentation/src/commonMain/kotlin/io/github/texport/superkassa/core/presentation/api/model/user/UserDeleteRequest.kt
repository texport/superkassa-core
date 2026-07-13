package io.github.texport.superkassa.core.presentation.api.model.user

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Запрос на удаление пользователя.
 */
@Serializable
@Schema(description = "Запрос на удаление пользователя")
data class UserDeleteRequest(
    @Schema(
        description = "Не используется. ПИН-код передается только в заголовке Authorization",
        example = "deprecated"
    )
    val _unused: String? = null
)
