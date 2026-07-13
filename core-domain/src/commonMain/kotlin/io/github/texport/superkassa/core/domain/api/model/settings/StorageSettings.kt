package io.github.texport.superkassa.core.domain.api.model.settings

/**
 * Настройки хранилища и параметров подключения к базе данных.
 *
 * @property engine Используемая СУБД (например, SQLITE, POSTGRESQL, MYSQL).
 * @property jdbcUrl URL-строка подключения JDBC к базе данных.
 * @property user Имя пользователя для авторизации в БД.
 * @property password Пароль для авторизации в БД.
 */
data class StorageSettings(
    val engine: String,
    val jdbcUrl: String,
    val user: String? = null,
    val password: String? = null
)
