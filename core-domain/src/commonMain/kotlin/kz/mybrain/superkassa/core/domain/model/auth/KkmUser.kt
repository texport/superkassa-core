package kz.mybrain.superkassa.core.domain.model.auth

/**
 * Пользователь ККМ с правами доступа к кассовым операциям (администратор/кассир).
 *
 * @property id Уникальный идентификатор пользователя.
 * @property name Имя (ФИО) пользователя.
 * @property role Роль пользователя в системе (например, администратор или кассир).
 * @property pin ПИН-код пользователя для авторизации на ККМ (может быть пустым).
 * @property createdAt Временная метка создания пользователя (в миллисекундах).
 */
data class KkmUser(
    val id: String,
    val name: String,
    val role: UserRole,
    val pin: String?,
    val createdAt: Long
)
