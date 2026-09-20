package io.github.texport.superkassa.core.presentation.api.model.user

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Данные пользователя ККМ, возвращаемые API (Ответ).
 *
 * Пина в ответе нет: список пользователей доступен администратору,
 * и по нему нельзя было бы узнать чужие учётные данные.
 */
@Serializable
@Schema(description = "Данные пользователя")
data class UserResponse(
    @Schema(description = "ID пользователя", example = "user-123") val userId: String,
    @Schema(description = "Имя пользователя", example = "Иван Иванов") val name: String,
    @Schema(description = "Роль пользователя", example = "CASHIER") val role: UserRole
)
