package io.github.texport.superkassa.core.domain.impl.logging

import platform.Foundation.NSLog

actual class Logger(private val tag: String) {
    actual fun info(message: String, arg1: Any?, arg2: Any?) = log(LogLevel.INFO, format(message, arg1, arg2))
    actual fun info(message: String, arg1: Any?, arg2: Any?, arg3: Any?) = log(
        LogLevel.INFO,
        format(message, arg1, arg2, arg3)
    )
    actual fun info(message: String, arg1: Any?) = log(LogLevel.INFO, format(message, arg1))
    actual fun info(message: String) = log(LogLevel.INFO, message)

    actual fun warn(message: String, arg1: Any?, arg2: Any?) = log(LogLevel.WARN, format(message, arg1, arg2))
    actual fun warn(message: String, arg1: Any?) = log(LogLevel.WARN, format(message, arg1))
    actual fun warn(message: String) = log(LogLevel.WARN, message)

    actual fun debug(message: String, arg1: Any?, arg2: Any?) = log(LogLevel.DEBUG, format(message, arg1, arg2))
    actual fun debug(message: String, arg1: Any?) = log(LogLevel.DEBUG, format(message, arg1))
    actual fun debug(message: String) = log(LogLevel.DEBUG, message)

    actual fun trace(message: String, arg1: Any?, arg2: Any?) = log(LogLevel.TRACE, format(message, arg1, arg2))
    actual fun trace(message: String, arg1: Any?) = log(LogLevel.TRACE, format(message, arg1))
    actual fun trace(message: String) = log(LogLevel.TRACE, message)

    actual fun error(message: String, throwable: Throwable?) {
        val fullMessage = if (throwable != null) "$message. Exception: ${throwable.message}" else message
        log(LogLevel.ERROR, fullMessage)
        throwable?.printStackTrace()
    }
    actual fun error(message: String) = log(LogLevel.ERROR, message)

    private fun log(level: LogLevel, message: String) {
        if (!LoggerConfig.isEnabled(level)) return
        val formatted = "${level.name} [$tag]: $message"
        NSLog("%s", formatted)
        println(formatted)
        LoggerConfig.listener?.onLog(level.name, tag, message)
    }

    private fun format(message: String, vararg args: Any?): String {
        var result = message
        for (arg in args) {
            result = result.replaceFirst("{}", arg?.toString() ?: "null")
        }
        return result
    }
}

actual fun getLogger(clazz: kotlin.reflect.KClass<*>): Logger {
    return Logger(clazz.simpleName ?: "UnknownClass")
}
