package kz.mybrain.superkassa.core.domain.logging

@Suppress("unused")
expect class Logger {
    fun info(message: String, arg1: Any?, arg2: Any?)
    fun info(message: String, arg1: Any?, arg2: Any?, arg3: Any?)
    fun info(message: String, arg1: Any?)
    fun warn(message: String, arg1: Any?, arg2: Any?)
}

expect fun getLogger(clazz: kotlin.reflect.KClass<*>): Logger
