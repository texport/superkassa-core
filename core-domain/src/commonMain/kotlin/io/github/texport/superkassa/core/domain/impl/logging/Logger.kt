package io.github.texport.superkassa.core.domain.impl.logging

expect class Logger {
    fun info(message: String, arg1: Any?, arg2: Any?)
    fun info(message: String, arg1: Any?, arg2: Any?, arg3: Any?)
    fun info(message: String, arg1: Any?)
    fun info(message: String)
    fun warn(message: String, arg1: Any?, arg2: Any?)
    fun warn(message: String, arg1: Any?)
    fun warn(message: String)
    fun debug(message: String, arg1: Any?, arg2: Any?)
    fun debug(message: String, arg1: Any?)
    fun debug(message: String)
    fun trace(message: String, arg1: Any?, arg2: Any?)
    fun trace(message: String, arg1: Any?)
    fun trace(message: String)
    fun error(message: String, throwable: Throwable?)
    fun error(message: String)
}

expect fun getLogger(clazz: kotlin.reflect.KClass<*>): Logger
