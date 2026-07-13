package io.github.texport.superkassa.core.presentation.api.model.user

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import io.github.texport.superkassa.core.presentation.api.annotations.NotBlank
import io.github.texport.superkassa.core.presentation.api.annotations.Size
import kotlinx.serialization.Serializable

/**
 * Запрос на создание пользователя ККМ (кассира или администратора).
 */
@Serializable
@Schema(description = "Запрос на создание пользователя")
data class UserCreateRequest(
    @Schema(description = "Имя нового пользователя", example = "Иван Иванов")
    @field:NotBlank(message = "Name is required")
    val name: String,
    @Schema(description = "Роль пользователя (ADMIN или CASHIER)", example = "CASHIER")
    val role: UserRole,
    @Schema(description = "ПИН-код нового пользователя", example = "1234")
    @field:NotBlank(message = "User PIN is required")
    @field:Size(min = 4, max = 10, message = "PIN length must be between 4 and 10")
    val userPin: String
)
