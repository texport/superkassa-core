package io.github.texport.superkassa.offlinequeue.impl

import kotlin.reflect.KClass

/**
 * Ожидаемый класс логгера для различных платформ (expect).
 */
internal expect class Logger {
    fun trace(message: String, vararg args: Any?)
    fun debug(message: String, vararg args: Any?)
    fun info(message: String, vararg args: Any?)
    fun warn(message: String, vararg args: Any?)
    fun error(message: String, throwable: Throwable?)
}

/**
 * Ожидаемая фабричная функция для создания логгера (expect).
 */
internal expect fun getLogger(clazz: KClass<*>): Logger
