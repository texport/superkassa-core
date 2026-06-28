package kz.mybrain.superkassa.core.domain.model.kkm

/**
 * Режим работы ККМ.
 */
enum class KkmMode {
    /** Режим регистрации (первоначальная настройка и фискализация). */
    REGISTRATION,
    /** Режим программирования (настройка параметров работы). */
    PROGRAMMING
}
