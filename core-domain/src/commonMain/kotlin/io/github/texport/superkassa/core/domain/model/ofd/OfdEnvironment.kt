package io.github.texport.superkassa.core.domain.model.ofd

/**
 * Окружение (стенд/сервер) для взаимодействия с ОФД.
 *
 * @property id Идентификатор окружения в строковом виде.
 */
enum class OfdEnvironment(val id: String) {
    /** Стенд разработки. */
    DEV("DEV"),

    /** Тестовый стенд ОФД. */
    TEST("TEST"),

    /** Боевой (продуктовый) сервер ОФД. */
    PROD("PROD");

    companion object {
        /**
         * Находит окружение по его строковому идентификатору (регистронезависимо).
         */
        fun findEnvironment(envId: String): OfdEnvironment? =
            entries.firstOrNull { it.id.equals(envId, ignoreCase = true) }
    }
}
