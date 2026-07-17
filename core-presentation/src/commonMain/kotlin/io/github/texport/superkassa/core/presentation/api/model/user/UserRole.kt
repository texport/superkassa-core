package io.github.texport.superkassa.core.presentation.api.model.user

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Роли пользователей кассового аппарата ККМ.
 */
@Serializable
@Schema(description = "Роль пользователя")
enum class UserRole {
    /** Администратор кассы с полными правами управления */
    @Schema(description = "Администратор")
    ADMIN,

    /** Кассир-оператор с правами оформления чеков и смен */
    @Schema(description = "Кассир")
    CASHIER
}
