package kz.mybrain.superkassa.core.data.adapter

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kz.mybrain.superkassa.core.domain.model.queue.QueueTask
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.offline_queue.domain.model.QueueCommand
import kz.mybrain.superkassa.offline_queue.domain.model.QueueCommandType
import kz.mybrain.superkassa.offline_queue.domain.model.QueueLane
import kz.mybrain.superkassa.offline_queue.domain.model.QueueStatus

class StorageBackedQueueStorageAdapterTest {
    private val storage: StoragePort = mockk()
    private val adapter = StorageBackedQueueStorageAdapter(storage)

    @Test
    fun testEnqueue() {
        val command = QueueCommand(
            id = "cmd-1",
            cashboxId = "kkm-1",
            lane = QueueLane.OFFLINE,
            type = QueueCommandType.TICKET,
            payloadRef = "doc-1",
            createdAt = 100L,
            status = QueueStatus.PENDING,
            attempt = 1,
            nextAttemptAt = 200L,
            lastError = "error"
        )

        every { storage.enqueueQueueTask(any()) } returns true

        assertTrue(adapter.enqueue(command))

        verify {
            storage.enqueueQueueTask(
                withArg {
                    assertEquals("cmd-1", it.id)
                    assertEquals("kkm-1", it.cashboxId)
                    assertEquals("OFFLINE", it.lane)
                    assertEquals("TICKET", it.type)
                    assertEquals("doc-1", it.payloadRef)
                    assertEquals(100L, it.createdAt)
                    assertEquals("PENDING", it.status)
                    assertEquals(1, it.attempt)
                    assertEquals(200L, it.nextAttemptAt)
                    assertEquals("error", it.lastError)
                }
            )
        }
    }

    @Test
    fun testNextPending() {
        val task = QueueTask(
            id = "cmd-1",
            cashboxId = "kkm-1",
            lane = "OFFLINE",
            type = "TICKET",
            payloadRef = "doc-1",
            createdAt = 100L,
            status = "PENDING",
            attempt = 1,
            nextAttemptAt = 200L,
            lastError = "error"
        )

        every { storage.nextPendingQueueTask("kkm-1", "OFFLINE", 150L) } returns task
        val command = adapter.nextPending("kkm-1", QueueLane.OFFLINE, 150L)
        assertNotNull(command)
        assertEquals("cmd-1", command.id)
        assertEquals(QueueLane.OFFLINE, command.lane)
        assertEquals(QueueCommandType.TICKET, command.type)
        assertEquals(QueueStatus.PENDING, command.status)

        every { storage.nextPendingQueueTask("kkm-1", "OFFLINE", 150L) } returns null
        assertNull(adapter.nextPending("kkm-1", QueueLane.OFFLINE, 150L))
    }

    @Test
    fun testUpdateStatus() {
        every { storage.updateQueueTaskStatus("cmd-1", "SENT", 2, "no error", 300L) } returns true
        assertTrue(adapter.updateStatus("cmd-1", QueueStatus.SENT, 2, "no error", 300L))

        every { storage.updateQueueTaskStatus("cmd-1", "SENT", 2, "no error", 300L) } returns false
        assertFalse(adapter.updateStatus("cmd-1", QueueStatus.SENT, 2, "no error", 300L))
    }

    @Test
    fun testMarkInProgress() {
        every { storage.markQueueTaskInProgress("cmd-1", 150L) } returns true
        assertTrue(adapter.markInProgress("cmd-1", 150L))

        every { storage.markQueueTaskInProgress("cmd-1", 150L) } returns false
        assertFalse(adapter.markInProgress("cmd-1", 150L))
    }

    @Test
    fun testListByCashbox() {
        val task = QueueTask(
            id = "cmd-1",
            cashboxId = "kkm-1",
            lane = "OFFLINE",
            type = "TICKET",
            payloadRef = "doc-1",
            createdAt = 100L,
            status = "PENDING",
            attempt = 1,
            nextAttemptAt = null,
            lastError = null
        )

        every { storage.listQueueTasksByCashbox("kkm-1", "OFFLINE", 10, 0) } returns listOf(task)

        val list = adapter.listByCashbox("kkm-1", QueueLane.OFFLINE, 10, 0)
        assertEquals(1, list.size)
        assertEquals("cmd-1", list[0].id)

        every { storage.listQueueTasksByCashbox("kkm-1", "OFFLINE", 10, 0) } returns emptyList()
        assertTrue(adapter.listByCashbox("kkm-1", QueueLane.OFFLINE, 10, 0).isEmpty())
    }

    @Test
    fun testDeleteByCashbox() {
        every { storage.deleteQueueTasksByCashbox("kkm-1") } returns true
        assertTrue(adapter.deleteByCashbox("kkm-1"))

        every { storage.deleteQueueTasksByCashbox("kkm-1") } returns false
        assertFalse(adapter.deleteByCashbox("kkm-1"))
    }
}
