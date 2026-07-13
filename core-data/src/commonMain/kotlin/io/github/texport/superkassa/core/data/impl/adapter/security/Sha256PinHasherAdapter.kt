package io.github.texport.superkassa.core.data.impl.adapter.security

import io.github.texport.superkassa.core.data.impl.util.Sha256
import io.github.texport.superkassa.core.domain.api.port.internal.PinHasherPort

/**
 * Реализация PinHasherPort с использованием SHA-256.
 * Настраивается и создается как Spring-бин в superkassa-server.
 */
internal class Sha256PinHasherAdapter : PinHasherPort {
    /**
     * Хеширует PIN-код кассира с помощью алгоритма SHA-256.
     * Используется для безопасной проверки подлинности без хранения оригинального пароля.
     * @param pin Исходный PIN-код.
     * @return Шестнадцатеричная (hex) строка хеша.
     */
    override fun hash(pin: String): String {
        return Sha256.hash(pin)
    }
}
