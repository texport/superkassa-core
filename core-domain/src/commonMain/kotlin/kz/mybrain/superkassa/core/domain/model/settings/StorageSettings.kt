package kz.mybrain.superkassa.core.domain.model.settings

import kotlinx.serialization.Serializable

/**
 * Настройки хранилища и параметров подключения к базе данных.
 *
 * @property engine Используемая СУБД (например, SQLITE, POSTGRESQL, MYSQL).
 * @property jdbcUrl URL-строка подключения JDBC к базе данных.
 * @property user Имя пользователя для авторизации в БД.
 * @property password Пароль для авторизации в БД.
 */
@Serializable
data class StorageSettings(
    val engine: String,
    val jdbcUrl: String,
    val user: String? = null,
    val password: String? = null
)
