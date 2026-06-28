package kz.mybrain.superkassa.core.domain.model.auth

import kotlinx.serialization.Serializable

/**
 * Роли пользователей ККМ (Кассир, Администратор).
 */
@Serializable
enum class UserRole {
    /**
     * Администратор ККМ с полными правами доступа.
     */
    ADMIN,

    /**
     * Кассир ККМ с ограниченными правами доступа к кассовым операциям.
     */
    CASHIER
}
