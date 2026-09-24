package io.github.texport.superkassa.coredatabase.impl.dao

import io.github.texport.superkassa.coredatabase.impl.entity.DeliveryTaskEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Задачи доставки в памяти — для хранилища без файла: отвечают так же, как запросы Room. */
internal class InMemoryDeliveryTaskDao : DeliveryTaskDao {
    private val rows = mutableListOf<DeliveryTaskEntity>()
    private val lock = Mutex()

    override suspend fun insert(entities: List<DeliveryTaskEntity>) = lock.withLock {
        entities.filter { new -> rows.none { it.id == new.id } }.forEach { rows += it }
    }

    override suspend fun due(now: Long, limit: Int): List<DeliveryTaskEntity> = lock.withLock {
        rows.filter { it.status == PENDING && it.nextAttemptAt <= now }
            .sortedWith(compareBy({ it.nextAttemptAt }, { it.id }))
            .take(limit)
    }

    override suspend fun claim(id: String, now: Long, leaseUntil: Long): Int = lock.withLock {
        val index = rows.indexOfFirst { it.id == id && it.status == PENDING && it.nextAttemptAt <= now }
        if (index < 0) return@withLock 0
        rows[index] = rows[index].let { it.copy(attempts = it.attempts + 1, nextAttemptAt = leaseUntil, updatedAt = now) }
        1
    }

    override suspend fun update(entity: DeliveryTaskEntity) = lock.withLock {
        val index = rows.indexOfFirst { it.id == entity.id }
        if (index >= 0) rows[index] = entity
    }

    override suspend fun byDocument(documentId: String): List<DeliveryTaskEntity> = lock.withLock {
        rows.filter { it.documentId == documentId }.sortedWith(compareBy({ it.createdAt }, { it.id }))
    }

    override suspend fun deleteByKkm(kkmId: String) = lock.withLock {
        rows.removeAll { it.kkmId == kkmId }
        Unit
    }

    private companion object {
        const val PENDING = "PENDING"
    }
}
