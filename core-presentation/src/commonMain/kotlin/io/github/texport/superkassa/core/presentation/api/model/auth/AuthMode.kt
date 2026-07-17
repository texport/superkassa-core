package io.github.texport.superkassa.core.presentation.api.model.auth

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Режимы авторизации пользователей кассы.
 */
@Serializable
@Schema(description = "Режим авторизации")
enum class AuthMode {
    /**
     * Без авторизации.
     */
    @Schema(description = "Без авторизации")
    NONE,

    /**
     * Авторизация с использованием Bearer токена.
     */
    @Schema(description = "Bearer токен")
    BEARER
}
