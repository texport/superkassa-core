package io.github.texport.superkassa.testing.impl.bfd

import kz.kazakhtelecom.proto.v203.CommandTypeEnum
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Сбои связи, заказанные проверкой: разовые — по очереди, по одному
 * на запрос; отказ командам типа и обрыв связи — пока их не снимут.
 */
internal class BfdFaults {
    private val once = ConcurrentLinkedQueue<Fault>()
    private val rejections = ConcurrentHashMap<CommandTypeEnum, Int>()

    @Volatile
    var connected = true

    fun addOnce(fault: Fault) {
        once += fault
    }

    fun reject(command: CommandTypeEnum, code: Int) {
        rejections[command] = code
    }

    fun acceptAll() = rejections.clear()

    /** Сбой для очередного запроса команды [command] или `null`, если запрос проходит. */
    fun next(command: CommandTypeEnum): Fault? = when {
        !connected -> Fault.Unreachable
        else -> once.poll() ?: rejections[command]?.let { Fault.Refused(it) }
    }

    /** Сбой одного запроса. */
    sealed interface Fault {
        /** Запрос до БФД не дошёл. */
        data object Unreachable : Fault

        /** Запрос учтён, ответ до кассы не дошёл; держится, пока касса не пришлёт другой запрос. */
        data class Lost(val waitForAnotherMillis: Long) : Fault

        /** Отказ с кодом, ничего не учтено. */
        data class Refused(val code: Int) : Fault
    }
}
