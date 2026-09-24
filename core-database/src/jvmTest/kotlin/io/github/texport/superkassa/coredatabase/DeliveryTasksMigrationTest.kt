package io.github.texport.superkassa.coredatabase

import androidx.sqlite.execSQL
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryFailure
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryTask
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryTaskStatus
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.string.api.TrilingualMessage
import io.github.texport.superkassa.coredatabase.api.RoomStorageFactory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * База версии 13 открывается строгим открытием: шаг 13 → 14 заводит задачи
 * доставки, записанное остаётся. Задачи в файловой базе и в памяти ведут
 * себя одинаково: поставленная второй раз не меняется, занимает её один.
 */
class DeliveryTasksMigrationTest {
    private val file = RoomFile()

    @AfterTest
    fun cleanUp() = file.delete()

    @Test
    fun `база тринадцатой версии получает задачи доставки и хранит их между открытиями`() {
        file.session { }
        file.sql { connection ->
            connection.execSQL(
                "INSERT INTO kkms (id, state, mode, autoCloseShift, autoCashout, createdAt, updatedAt, name) " +
                    "VALUES ('kkm-old', 'ACTIVE', 'REGISTRATION', 0, 0, 1, 1, 'kept')"
            )
            connection.execSQL("DROP TABLE delivery_tasks")
            connection.execSQL("PRAGMA user_version = 13")
        }

        file.session { it.addDeliveryTasks(listOf(task("doc-1"))) }
        val stored = file.session { it.findKkm("kkm-old")?.name to it.deliveryTasksOf("doc-1") }

        assertEquals("kept" to listOf(task("doc-1")), stored)
    }

    @Test
    fun `в файле задачи ставятся, занимаются и уходят вместе с кассой`() = file.session { behaves(it) }

    @Test
    fun `в памяти задачи ведут себя так же`() = behaves(RoomStorageFactory.createDefaultStorage().storagePort)

    private fun behaves(storage: StoragePort) {
        storage.addDeliveryTasks(listOf(task("doc-1"), task("doc-2").copy(nextAttemptAt = 50L)))
        storage.addDeliveryTasks(listOf(task("doc-1").copy(status = DeliveryTaskStatus.FAILED)))
        storage.addDeliveryTasks(emptyList())

        val due = storage.dueDeliveryTasks(now = 20L, limit = 10).map { it.documentId }
        val first = storage.claimDeliveryTask("doc-1/SMS/LINK", now = 20L, leaseUntil = 80L)
        val second = storage.claimDeliveryTask("doc-1/SMS/LINK", now = 20L, leaseUntil = 80L)
        val claimed = storage.deliveryTasksOf("doc-1").single()
        val failure = DeliveryFailure("DELIVERY_PROVIDER_REJECTED", TrilingualMessage("отказ", "бас тарту", "refused"))
        storage.saveDeliveryTask(claimed.copy(status = DeliveryTaskStatus.FAILED, failure = failure, updatedAt = 30L))
        val saved = storage.deliveryTasksOf("doc-1").single()
        storage.deleteKkmCompletely("kkm-1")

        assertEquals(listOf("doc-1"), due)
        assertEquals(true to false, first to second)
        assertEquals(1 to 80L, claimed.attempts to claimed.nextAttemptAt)
        assertEquals(DeliveryTaskStatus.FAILED to failure, saved.status to saved.failure)
        assertEquals(emptyList(), storage.deliveryTasksOf("doc-1") + storage.deliveryTasksOf("doc-2"))
    }

    private fun task(documentId: String) = DeliveryTask(
        id = DeliveryTask.idOf(documentId, "SMS", "LINK"),
        kkmId = "kkm-1",
        documentId = documentId,
        channel = "SMS",
        destination = "+77010000001",
        payloadType = "LINK",
        nextAttemptAt = 10L,
        createdAt = 10L
    )
}
