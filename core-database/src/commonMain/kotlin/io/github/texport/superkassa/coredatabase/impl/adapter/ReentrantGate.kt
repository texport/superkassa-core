package io.github.texport.superkassa.coredatabase.impl.adapter

/**
 * Замок, который тот же поток может взять повторно.
 *
 * Транзакции ядра вкладываются друг в друга: чек внутри своей транзакции
 * зовёт помощника, открывающего свою. Невозвратный замок на этом встал бы.
 */
internal expect class ReentrantGate() {
    fun lock()
    fun unlock()
}
