package io.github.texport.superkassa.core.domain.api.port.security

/**
 * Порт безопасности и криптографических операций ядра ККМ.
 */
interface SecurityPort {

    /**
     * Вычисляет SHA-256 хеш строки (например, для проверки PIN-кодов и токенов).
     *
     * @param input Исходная строка.
     * @return Шестнадцатеричный (Hex) хеш строки.
     */
    fun sha256(input: String): String

    /**
     * Шифрует чувствительные данные (например, фискальные секреты) с использованием ключа.
     *
     * @param data Данные в виде массива байт.
     * @param secretKey Секретный ключ шифрования.
     * @return Зашифрованный массив байт.
     */
    fun encrypt(data: ByteArray, secretKey: String): ByteArray

    /**
     * Расшифровывает данные.
     *
     * @param encryptedData Зашифрованный массив байт.
     * @param secretKey Секретный ключ расшифровки.
     * @return Исходный массив байт.
     */
    fun decrypt(encryptedData: ByteArray, secretKey: String): ByteArray
}
