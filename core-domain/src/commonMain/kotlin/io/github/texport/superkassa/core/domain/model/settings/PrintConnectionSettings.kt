package io.github.texport.superkassa.core.domain.model.settings

/**
 * Настройки подключения к чековому принтеру.
 *
 * @property type Тип подключения (например, "NETWORK", "USB").
 * @property host Сетевой адрес принтера (IP-адрес или имя хоста).
 * @property port Сетевой порт принтера (по умолчанию 9100).
 */
data class PrintConnectionSettings(
    val type: String = "NETWORK",
    val host: String? = null,
    val port: Int? = 9100
)
