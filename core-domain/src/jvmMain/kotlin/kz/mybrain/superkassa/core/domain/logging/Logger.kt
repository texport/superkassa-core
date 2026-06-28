package kz.mybrain.superkassa.core.domain.logging

import org.slf4j.LoggerFactory

@Suppress("unused")
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
    actual fun warn(message: String, arg1: Any?, arg2: Any?) {
        slf4jLogger.warn(message, arg1, arg2)
    }
}

actual fun getLogger(clazz: kotlin.reflect.KClass<*>): Logger {
    return Logger(LoggerFactory.getLogger(clazz.java))
}
