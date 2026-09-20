package io.github.texport.superkassa.core.domain.impl.logging

/**
 * Уровни логирования для библиотеки superkassa-core.
 */
enum class LogLevel(val priority: Int) {
    TRACE(0),
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4),
    NONE(5)
}

interface LogListener {
    fun onLog(levelName: String, tag: String, message: String)
}

/**
 * Глобальная конфигурация логгера библиотеки superkassa-core.
 */
object LoggerConfig {
    /**
     * Минимальный активный уровень логирования.
     * Сообщения с приоритетом ниже указанного не выводятся логгером.
     */
    var minLogLevel: LogLevel = LogLevel.DEBUG

    var listener: LogListener? = null

    /**
     * Проверяет, включен ли указанный уровень логирования.
     */
    fun isEnabled(level: LogLevel): Boolean {
        return level.priority >= minLogLevel.priority
    }
}
