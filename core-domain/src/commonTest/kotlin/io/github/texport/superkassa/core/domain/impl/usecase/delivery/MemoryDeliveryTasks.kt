package io.github.texport.superkassa.core.domain.impl.usecase.delivery

import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryTask
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryTaskStatus
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.mockk.every
import io.mockk.mockk

/** Задачи доставки в памяти — так же, как их держит база кассы. */
class MemoryDeliveryTasks {
    val rows: MutableList<DeliveryTask> = mutableListOf()

    /** Хранилище, у которого задачи доставки — эти, а прочее — заглушка [base]. */
    fun storage(base: StoragePort = mockk(relaxed = true)): StoragePort = base.also { storage ->
        every { storage.addDeliveryTasks(any()) } answers {
            firstArg<List<DeliveryTask>>().filter { new -> rows.none { it.id == new.id } }.forEach { rows += it }
        }
        every { storage.dueDeliveryTasks(any(), any()) } answers {
            rows.filter { it.status == DeliveryTaskStatus.PENDING && it.nextAttemptAt <= firstArg<Long>() }.take(secondArg())
        }
        every { storage.claimDeliveryTask(any(), any(), any()) } answers { claim(firstArg(), secondArg(), thirdArg()) }
        every { storage.saveDeliveryTask(any()) } answers {
            val task = firstArg<DeliveryTask>()
            rows[rows.indexOfFirst { it.id == task.id }] = task
        }
        every { storage.deliveryTasksOf(any()) } answers { rows.filter { it.documentId == firstArg<String>() } }
    }

    private fun claim(id: String, now: Long, leaseUntil: Long): Boolean {
        val index = rows.indexOfFirst { it.id == id && it.status == DeliveryTaskStatus.PENDING && it.nextAttemptAt <= now }
        if (index < 0) return false
        rows[index] = rows[index].let { it.copy(attempts = it.attempts + 1, nextAttemptAt = leaseUntil) }
        return true
    }
}
