package io.github.texport.superkassa.delivery.impl

import kotlin.reflect.KClass

/**
 * Реализация логгера для iOS через NSLog/println.
 */
internal actual class Logger(private val tag: String) {
    actual fun trace(message: String, vararg args: Any?) {
        println("TRACE [$tag]: ${format(message, *args)}")
    }
    actual fun debug(message: String, vararg args: Any?) {
        println("DEBUG [$tag]: ${format(message, *args)}")
    }
    actual fun info(message: String, vararg args: Any?) {
        println("INFO [$tag]: ${format(message, *args)}")
    }
    actual fun warn(message: String, vararg args: Any?) {
        println("WARN [$tag]: ${format(message, *args)}")
    }
    actual fun error(message: String, throwable: Throwable?) {
        println("ERROR [$tag]: $message")
        throwable?.printStackTrace()
    }

    private fun format(message: String, vararg args: Any?): String {
        var result = message
        for (arg in args) {
            result = result.replaceFirst("{}", arg?.toString() ?: "null")
        }
        return result
    }
}

/**
 * Реализация фабричного метода получения логгера для iOS.
 */
internal actual fun getLogger(clazz: KClass<*>): Logger {
    return Logger(clazz.simpleName ?: "UnknownClass")
}
