package io.github.texport.superkassa.core.domain.api.model.auth

/**
 * Пользователь ККМ с правами доступа к кассовым операциям (администратор/кассир).
 *
 * Пина здесь нет намеренно: узел хранит только его хеш, а знать пин
 * чужого пользователя не должен никто, включая администратора.
 *
 * @property id Уникальный идентификатор пользователя.
 * @property name Имя (ФИО) пользователя.
 * @property role Роль пользователя в системе (например, администратор или кассир).
 * @property createdAt Временная метка создания пользователя (в миллисекундах).
 */
data class KkmUser(
    val id: String,
    val name: String,
    val role: UserRole,
    val createdAt: Long
)
