package io.github.texport.superkassa.coredatabase.impl.adapter

import io.github.texport.superkassa.coredatabase.impl.dao.QueueCommandDao
import io.github.texport.superkassa.coredatabase.impl.entity.QueueCommandEntity
import io.github.texport.superkassa.offlinequeue.api.model.QueueCommand
import io.github.texport.superkassa.offlinequeue.api.model.QueueLane
import io.github.texport.superkassa.offlinequeue.api.model.QueueStatus
import kotlinx.coroutines.runBlocking

/**
 * Очередь досылки документов кассы: команды и аренда обработки.
 *
 * Ведёт себя как очередь узла. Команда ставится один раз: повторная
 * постановка того же документа её не трогает. Захват обработчиком
 * отмечает время и не сбрасывает число попыток. Аренда кассы у одного
 * обработчика: пока она не истекла, второй её не получит.
 */
internal class RoomQueue(private val dao: QueueCommandDao) {
    private val gate = ReentrantGate()
    private val leases = mutableMapOf<String, Lease>()

    fun enqueue(command: QueueCommand): Boolean = runBlocking {
        dao.insert(QueueCommandEntity.fromDomain(command)) != NOT_INSERTED
    }

    fun byStatus(cashboxId: String, lane: QueueLane, statuses: Set<QueueStatus>): List<QueueCommand> = runBlocking {
        dao.getByStatus(cashboxId, lane.name, statuses.map { it.name }).map { it.toDomain() }
    }

    fun updateStatus(id: String, status: QueueStatus, attempt: Int, error: QueueError, nextAttemptAt: Long?): Boolean =
        runBlocking { dao.updateStatus(id, status.name, attempt, error.text, error.code, nextAttemptAt) == 1 }

    fun markInProgress(id: String, now: Long): Boolean = runBlocking { dao.markInProgress(id, now) == 1 }

    fun list(cashboxId: String, lane: QueueLane, limit: Int, offset: Int): List<QueueCommand> = runBlocking {
        dao.listByCashbox(cashboxId, lane.name, limit, offset).map { it.toDomain() }
    }

    fun deleteByCashbox(cashboxId: String): Boolean = runBlocking {
        dao.deleteByCashbox(cashboxId)
        true
    }

    /** Аренда достаётся, только если её нет или она истекла — даже прежнему владельцу. */
    fun acquire(cashboxId: String, ownerId: String, leaseUntil: Long, acquiredAt: Long): Boolean = guarded {
        val current = leases[cashboxId]
        if (current != null && current.until >= acquiredAt) return@guarded false
        leases[cashboxId] = Lease(ownerId, leaseUntil)
        true
    }

    fun renew(cashboxId: String, ownerId: String, leaseUntil: Long, now: Long): Boolean = guarded {
        val current = leases[cashboxId]
        if (current?.owner != ownerId || current.until < now) return@guarded false
        leases[cashboxId] = Lease(ownerId, leaseUntil)
        true
    }

    fun release(cashboxId: String, ownerId: String): Boolean = guarded {
        if (leases[cashboxId]?.owner != ownerId) return@guarded false
        leases.remove(cashboxId)
        true
    }

    private fun <T> guarded(block: () -> T): T {
        gate.lock()
        try {
            return block()
        } finally {
            gate.unlock()
        }
    }

    private data class Lease(val owner: String, val until: Long)

    private companion object {
        const val NOT_INSERTED = -1L
    }
}
