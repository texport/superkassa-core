package io.github.texport.superkassa.core.domain.api.model.kkm

/**
 * Режим работы ККМ.
 */
enum class KkmMode {
    /** Режим регистрации (первоначальная настройка и фискализация). */
    REGISTRATION,

    /** Режим программирования (настройка параметров работы). */
    PROGRAMMING
}
