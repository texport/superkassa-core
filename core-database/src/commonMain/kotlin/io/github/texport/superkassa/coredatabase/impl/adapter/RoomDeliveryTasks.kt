package io.github.texport.superkassa.coredatabase.impl.adapter

import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryTask
import io.github.texport.superkassa.core.domain.api.port.integration.DeliveryTaskStore
import io.github.texport.superkassa.coredatabase.impl.dao.DeliveryTaskDao
import io.github.texport.superkassa.coredatabase.impl.entity.DeliveryTaskEntity
import kotlinx.coroutines.runBlocking

/**
 * Задачи доставки чека покупателю в базе кассы.
 *
 * Занятие задачи — одно условное изменение записи: фон и ручной повтор,
 * взявшиеся за неё одновременно, не отправят чек дважды.
 */
internal class RoomDeliveryTasks(private val dao: DeliveryTaskDao) : DeliveryTaskStore {

    override fun addDeliveryTasks(tasks: List<DeliveryTask>) = runBlocking {
        if (tasks.isNotEmpty()) dao.insert(tasks.map(DeliveryTaskEntity::fromDomain))
    }

    override fun dueDeliveryTasks(now: Long, limit: Int): List<DeliveryTask> = runBlocking {
        dao.due(now, limit).map { it.toDomain() }
    }

    override fun claimDeliveryTask(id: String, now: Long, leaseUntil: Long): Boolean = runBlocking {
        dao.claim(id, now, leaseUntil) > 0
    }

    override fun saveDeliveryTask(task: DeliveryTask) = runBlocking {
        dao.update(DeliveryTaskEntity.fromDomain(task))
    }

    override fun deliveryTasksOf(documentId: String): List<DeliveryTask> = runBlocking {
        dao.byDocument(documentId).map { it.toDomain() }
    }

    /** Касса уходит целиком — вместе с задачами доставки её чеков. */
    fun deleteByKkm(kkmId: String) = runBlocking { dao.deleteByKkm(kkmId) }
}
