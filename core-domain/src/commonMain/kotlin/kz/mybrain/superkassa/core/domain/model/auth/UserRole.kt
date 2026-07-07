package kz.mybrain.superkassa.core.domain.model.auth

/**
 * Роли пользователей ККМ (Кассир, Администратор).
 */
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
