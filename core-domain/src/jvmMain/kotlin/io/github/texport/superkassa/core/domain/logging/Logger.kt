package io.github.texport.superkassa.core.domain.logging

import org.slf4j.LoggerFactory

actual class Logger(private val slf4jLogger: org.slf4j.Logger) {
    actual fun info(message: String, arg1: Any?, arg2: Any?) {
        slf4jLogger.info(message, arg1, arg2)
    }
    actual fun info(message: String, arg1: Any?, arg2: Any?, arg3: Any?) {
        slf4jLogger.info(message, arg1, arg2, arg3)
    }
    actual fun info(message: String, arg1: Any?) {
        slf4jLogger.info(message, arg1)
    }
    actual fun info(message: String) {
        slf4jLogger.info(message)
    }
    actual fun warn(message: String, arg1: Any?, arg2: Any?) {
        slf4jLogger.warn(message, arg1, arg2)
    }
    actual fun warn(message: String, arg1: Any?) {
        slf4jLogger.warn(message, arg1)
    }
    actual fun warn(message: String) {
        slf4jLogger.warn(message)
    }
    actual fun debug(message: String, arg1: Any?, arg2: Any?) {
        slf4jLogger.debug(message, arg1, arg2)
    }
    actual fun debug(message: String, arg1: Any?) {
        slf4jLogger.debug(message, arg1)
    }
    actual fun debug(message: String) {
        slf4jLogger.debug(message)
    }
    actual fun trace(message: String, arg1: Any?, arg2: Any?) {
        slf4jLogger.trace(message, arg1, arg2)
    }
    actual fun trace(message: String, arg1: Any?) {
        slf4jLogger.trace(message, arg1)
    }
    actual fun trace(message: String) {
        slf4jLogger.trace(message)
    }
    actual fun error(message: String, throwable: Throwable?) {
        slf4jLogger.error(message, throwable)
    }
    actual fun error(message: String) {
        slf4jLogger.error(message)
    }
}

actual fun getLogger(clazz: kotlin.reflect.KClass<*>): Logger {
    return Logger(LoggerFactory.getLogger(clazz.java))
}
