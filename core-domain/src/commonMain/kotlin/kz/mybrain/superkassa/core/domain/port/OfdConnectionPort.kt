package kz.mybrain.superkassa.core.domain.port

/**
 * Порт сетевого соединения для обмена данными с ОФД.
 */
interface OfdConnectionPort {
    /**
     * Устанавливает соединение с хостом/портом, отправляет байты запроса и считывает байты ответа.
     *
     * @param host Адрес сервера ОФД.
     * @param port Порт сервера ОФД.
     * @param requestData Массив байт запроса.
     * @param timeoutMs Таймаут в миллисекундах.
     * @return Массив байт ответа или null в случае ошибки связи.
     */
    fun sendAndReceive(host: String, port: Int, requestData: ByteArray, timeoutMs: Long): ByteArray?
}
