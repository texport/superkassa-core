package io.github.texport.superkassa.core.presentation.api.model.user

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Данные пользователя ККМ, возвращаемые API (Ответ).
 */
@Serializable
@Schema(description = "Данные пользователя")
data class UserResponse(
    @Schema(description = "ID пользователя", example = "user-123") val userId: String,
    @Schema(description = "Имя пользователя", example = "Иван Иванов") val name: String,
    @Schema(description = "Роль пользователя", example = "CASHIER") val role: UserRole,
    @Schema(
        description = "ПИН-код пользователя (возвращается только для справки)",
        example = "1234"
    )
    val pin: String?
)
