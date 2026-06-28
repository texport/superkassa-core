package kz.mybrain.superkassa.core.data.adapter

import java.util.Base64
import kz.mybrain.superkassa.core.domain.port.TokenCodecPort
import org.slf4j.LoggerFactory

/**
 * Реализация TokenCodecPort на базе Base64.
 * Настраивается и создается как Spring-бин в superkassa-server.
 */
class Base64TokenCodecAdapter : TokenCodecPort {
    private val logger = LoggerFactory.getLogger(Base64TokenCodecAdapter::class.java)

    /**
     * Кодирует числовой токен ОФД в строку формата Base64 для безопасной передачи и хранения.
     * @param token Числовой токен.
     * @return Закодированная строка Base64.
     */
    override fun encodeToken(token: Long): String {
        return Base64.getEncoder().encodeToString(token.toString().toByteArray())
    }

    /**
     * Декодирует строку Base64 обратно в числовой токен.
     * Возвращает null при неверном формате Base64 или если строка пустая.
     * @param tokenEncryptedBase64 Строка токена в Base64.
     * @return Декодированное число Long или null при ошибке.
     */
    override fun decodeToken(tokenEncryptedBase64: String?): Long? {
        if (tokenEncryptedBase64.isNullOrBlank()) return null
        return try {
            // Декодируем байты Base64
            val decoded = Base64.getDecoder().decode(tokenEncryptedBase64)
            val text = decoded.toString(Charsets.UTF_8)
            // Парсим в Long
            text.toLongOrNull()
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid Base64 format in token: {}", e.message)
            null
        }
    }
}
