package kz.mybrain.superkassa.core.domain.logging

@Suppress("unused")
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
    actual fun warn(message: String, arg1: Any?, arg2: Any?) {
        println("WARN [$tag]: ${format(message, arg1, arg2)}")
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
