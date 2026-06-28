package kz.mybrain.superkassa.core.data.adapter

import java.security.MessageDigest
import kz.mybrain.superkassa.core.domain.port.PinHasherPort

/**
 * Реализация PinHasherPort с использованием SHA-256.
 * Настраивается и создается как Spring-бин в superkassa-server.
 */
class Sha256PinHasherAdapter : PinHasherPort {
    /**
     * Хеширует PIN-код кассира с помощью алгоритма SHA-256.
     * Используется для безопасной проверки подлинности без хранения оригинального пароля.
     * @param pin Исходный PIN-код.
     * @return Шестнадцатеричная (hex) строка хеша.
     */
    override fun hash(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
