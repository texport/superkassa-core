package io.github.texport.superkassa.coredatabase.impl.adapter

import io.github.texport.superkassa.coredatabase.impl.dao.IdempotencyDao
import io.github.texport.superkassa.coredatabase.impl.entity.IdempotencyEntity
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock

/**
 * Один писатель на базу и ключи повтора фискальных операций.
 *
 * Транзакция ядра здесь — очередь: пока одна операция кассы идёт, вторая
 * ждёт, как ждала бы блокировки строки кассы в базе узла. Отката записей
 * Room через синхронный порт хранилища не даёт, поэтому откат здесь
 * частичный: забываются только ключи повтора, заведённые сорвавшейся
 * операцией, — так повтор после отказа выполняется заново, как после
 * отката транзакции узла, а не отвечает «уже сделано».
 */
internal class RoomWriter(private val dao: IdempotencyDao) {
    private val gate = ReentrantGate()
    private var depth = 0
    private val opened = mutableListOf<Pair<String, String>>()

    fun begin() {
        gate.lock()
        depth++
    }

    fun commit() {
        finish(forgetOpened = false)
    }

    fun rollback() {
        finish(forgetOpened = true)
    }

    fun insertKey(kkmId: String, key: String, operation: String): Boolean = runBlocking {
        val entity = IdempotencyEntity(kkmId, key, operation, null, Clock.System.now().toEpochMilliseconds())
        val created = dao.insert(entity) != -1L
        if (created && depth > 0) opened += kkmId to key
        created
    }

    fun responseOf(kkmId: String, key: String): String? = runBlocking { dao.find(kkmId, key)?.responseRef }

    fun complete(kkmId: String, key: String, responseRef: String?): Boolean = runBlocking {
        dao.complete(kkmId, key, responseRef) > 0
    }

    private fun finish(forgetOpened: Boolean) {
        try {
            depth--
            if (depth == 0) {
                if (forgetOpened) runBlocking { opened.forEach { (kkm, key) -> dao.forgetUnfinished(kkm, key) } }
                opened.clear()
            }
        } finally {
            gate.unlock()
        }
    }
}
