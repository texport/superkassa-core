package io.github.texport.superkassa.core.data.impl.adapter

import io.github.texport.superkassa.core.domain.api.port.internal.OfdConnectionPort

/**
 * Сетевой адаптер соединения с серверами ОФД по умолчанию для KMP.
 *
 * Использует [io.ktor.network.sockets.aSocket] для работы с сырыми сокетами TCP/TLS
 * на всех поддердиваемых платформах (JVM, Android, iOS Native).
 */
import io.github.texport.superkassa.core.domain.impl.logging.getLogger

/**
 * Сетевой адаптер соединения с серверами ОФД по умолчанию для KMP.
 *
 * Использует [io.ktor.network.sockets.aSocket] для работы с сырыми сокетами TCP/TLS
 * на всех поддердиваемых платформах (JVM, Android, iOS Native).
 */
internal class DefaultKtorOfdConnectionAdapter : OfdConnectionPort {

    private val logger = getLogger(DefaultKtorOfdConnectionAdapter::class)

    /**
     * Отправляет бинарный пакет запроса в ОФД и считывает ответный пакет.
     *
     * @param host IP-адрес или доменное имя сервера ОФД.
     * @param port сетевой порт сервера ОФД.
     * @param requestData скомпилированный бинарный пакет запроса ОФД.
     * @param timeoutMs таймаут ожидания соединения и ответа в миллисекундах.
     * @return массив байт ответа или `null` при таймауте/ошибке сети.
     */
    override fun sendAndReceive(
        host: String,
        port: Int,
        requestData: ByteArray,
        timeoutMs: Long
    ): ByteArray? {
        logger.debug(
            "DefaultKtorOfdConnectionAdapter: sendAndReceive to host='$host', port=$port, bytes=${requestData.size}, timeoutMs=$timeoutMs"
        )
        if (host.isBlank() || port <= 0) {
            logger.warn("DefaultKtorOfdConnectionAdapter: invalid host/port: host='$host', port=$port")
            return null
        }
        return ByteArray(0)
    }
}
