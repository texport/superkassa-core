package io.github.texport.superkassa.core.data.adapter.security

import io.github.texport.superkassa.core.data.util.Base64Coder
import io.github.texport.superkassa.core.domain.logging.getLogger
import io.github.texport.superkassa.core.domain.port.TokenCodecPort

/**
 * Реализация TokenCodecPort на базе Base64.
 * Настраивается и создается как Spring-бин в superkassa-server.
 */
class Base64TokenCodecAdapter : TokenCodecPort {
    private val logger = getLogger(Base64TokenCodecAdapter::class)

    /**
     * Кодирует числовой токен ОФД в строку формата Base64 для безопасной передачи и хранения.
     * @param token Числовой токен.
     * @return Закодированная строка Base64.
     */
    override fun encodeToken(token: Long): String {
        return Base64Coder.encode(token.toString().encodeToByteArray())
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
            val decoded = Base64Coder.decode(tokenEncryptedBase64)
            val text = decoded.decodeToString()
            // Парсим в Long
            text.toLongOrNull()
        } catch (e: IllegalArgumentException) {
            logger.warn("Invalid Base64 format in token: {}", e.message)
            null
        }
    }
}
