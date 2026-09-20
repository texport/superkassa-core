package io.github.texport.superkassa.core.domain.api.model.ofd

/**
 * Конечная точка (хост и порт) для подключения к серверам ОФД.
 *
 * @property host IP-адрес или доменное имя сервера ОФД.
 * @property port Сетевой порт подключения.
 * @property checkDomain Домен для проверки статуса отправленных чеков потребителем;
 * `null` у адреса, заданного вручную, — такому ОФД домен неизвестен.
 */
data class OfdEndpoint(
    val host: String,
    val port: Int,
    val checkDomain: String? = null
)
