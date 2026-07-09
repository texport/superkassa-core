package io.github.texport.superkassa.core.domain.logging

actual class Logger(private val tag: String) {
    actual fun info(message: String, arg1: Any?, arg2: Any?) {
        println("INFO [$tag]: ${format(message, arg1, arg2)}")
    }
    actual fun info(message: String, arg1: Any?, arg2: Any?, arg3: Any?) {
        println("INFO [$tag]: ${format(message, arg1, arg2, arg3)}")
    }
    actual fun info(message: String, arg1: Any?) {
        println("INFO [$tag]: ${format(message, arg1)}")
    }
    actual fun info(message: String) {
        println("INFO [$tag]: $message")
    }
    actual fun warn(message: String, arg1: Any?, arg2: Any?) {
        println("WARN [$tag]: ${format(message, arg1, arg2)}")
    }
    actual fun warn(message: String, arg1: Any?) {
        println("WARN [$tag]: ${format(message, arg1)}")
    }
    actual fun warn(message: String) {
        println("WARN [$tag]: $message")
    }
    actual fun debug(message: String, arg1: Any?, arg2: Any?) {
        println("DEBUG [$tag]: ${format(message, arg1, arg2)}")
    }
    actual fun debug(message: String, arg1: Any?) {
        println("DEBUG [$tag]: ${format(message, arg1)}")
    }
    actual fun debug(message: String) {
        println("DEBUG [$tag]: $message")
    }
    actual fun trace(message: String, arg1: Any?, arg2: Any?) {
        println("TRACE [$tag]: ${format(message, arg1, arg2)}")
    }
    actual fun trace(message: String, arg1: Any?) {
        println("TRACE [$tag]: ${format(message, arg1)}")
    }
    actual fun trace(message: String) {
        println("TRACE [$tag]: $message")
    }
    actual fun error(message: String, throwable: Throwable?) {
        println("ERROR [$tag]: $message. Exception: ${throwable?.message}")
        throwable?.printStackTrace()
    }
    actual fun error(message: String) {
        println("ERROR [$tag]: $message")
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
