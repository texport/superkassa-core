package io.github.texport.superkassa.core.presentation.api.model.user

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Запрос на обновление параметров пользователя ККМ.
 */
@Serializable
@Schema(description = "Запрос на обновление пользователя")
data class UserUpdateRequest(
    @Schema(description = "Новое имя пользователя", example = "Петр Петров")
    val name: String? = null,
    @Schema(description = "Новая роль", example = "ADMIN") val role: UserRole? = null,
    @Schema(description = "Новый ПИН-код пользователя", example = "4321")
    val userPin: String? = null
)
