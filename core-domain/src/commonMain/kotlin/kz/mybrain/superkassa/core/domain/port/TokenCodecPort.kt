package kz.mybrain.superkassa.core.domain.port

/**
 * Порт для кодирования, декодирования и валидации токенов авторизации ОФД.
 * Соблюдает принцип инверсии зависимостей (Dependency Inversion Principle, DIP) из SOLID.
 */
interface TokenCodecPort {

    /**
     * Кодирует числовой токен ОФД в строковое представление (например, шифрует в Base64) для безопасного хранения.
     *
     * @param token исходный числовой токен.
     * @return закодированная/зашифрованная строка.
     */
    fun encodeToken(token: Long): String

    /**
     * Декодирует токен из зашифрованного строкового представления обратно в число.
     *
     * @param tokenEncryptedBase64 закодированный токен ККМ в Base64.
     * @return исходный токен в формате [Long] или `null`, если переданная строка пуста или некорректна.
     */
    fun decodeToken(tokenEncryptedBase64: String?): Long?

    /**
     * Выполняет парсинг и валидацию токена ОФД из текстовой строки ввода.
     *
     * @param token текстовое представление токена ОФД.
     * @return разобранный токен в виде числа [Long].
     * @throws kz.mybrain.superkassa.core.domain.exception.ValidationException если переданная строка не является валидным числом.
     */
    fun parseToken(token: String): Long {
        return token.toLongOrNull()
            ?: throw kz.mybrain.superkassa.core.domain.exception.ValidationException(
                kz.mybrain.superkassa.core.domain.exception.ErrorMessages.ofdTokenInvalid(token),
                "OFD_TOKEN_INVALID"
            )
    }
}
