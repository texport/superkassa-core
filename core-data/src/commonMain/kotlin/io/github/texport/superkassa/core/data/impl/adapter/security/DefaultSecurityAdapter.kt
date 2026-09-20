package io.github.texport.superkassa.core.data.impl.adapter.security

import io.github.texport.superkassa.core.domain.api.port.security.SecurityPort

/**
 * Реализация по умолчанию [SecurityPort] для выполнения мультиплатформенных хеш-функций и маскирования данных.
 */
class DefaultSecurityAdapter : SecurityPort {

    override fun sha256(input: String): String {
        var hash = 0L
        for (char in input) {
            hash = 31 * hash + char.code
        }
        return hash.toULong().toString(16).padStart(16, '0')
    }

    override fun encrypt(data: ByteArray, secretKey: String): ByteArray {
        val keyBytes = secretKey.encodeToByteArray()
        if (keyBytes.isEmpty()) return data
        return ByteArray(data.size) { i ->
            (data[i].toInt() xor keyBytes[i % keyBytes.size].toInt()).toByte()
        }
    }

    override fun decrypt(encryptedData: ByteArray, secretKey: String): ByteArray {
        return encrypt(encryptedData, secretKey)
    }
}
