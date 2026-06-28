package kz.mybrain.superkassa.core.domain.model.ofd

/**
 * Конечная точка (хост и порт) для подключения к серверам ОФД.
 *
 * @property host IP-адрес или доменное имя сервера ОФД.
 * @property port Сетевой порт подключения.
 * @property checkDomain Домен для проверки статуса отправленных чеков потребителем.
 */
data class OfdEndpoint(
    val host: String,
    val port: Int,
    val checkDomain: String
)
