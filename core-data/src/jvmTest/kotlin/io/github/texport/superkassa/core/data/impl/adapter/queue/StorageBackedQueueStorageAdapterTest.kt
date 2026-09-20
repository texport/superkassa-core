package io.github.texport.superkassa.core.data.impl.adapter.queue

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import io.github.texport.superkassa.core.domain.api.model.queue.QueueTask
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.offlinequeue.api.model.QueueCommand
import io.github.texport.superkassa.offlinequeue.api.model.QueueCommandType
import io.github.texport.superkassa.offlinequeue.api.model.QueueLane
import io.github.texport.superkassa.offlinequeue.api.model.QueueStatus

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
    fun testGetCommandsByStatus() {
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

        every { storage.getQueueTasksByStatus("kkm-1", "OFFLINE", setOf("PENDING")) } returns listOf(task)
        val commands = adapter.getCommandsByStatus("kkm-1", QueueLane.OFFLINE, setOf(QueueStatus.PENDING))
        assertEquals(1, commands.size)
        val command = commands[0]
        assertEquals("cmd-1", command.id)
        assertEquals(QueueLane.OFFLINE, command.lane)
        assertEquals(QueueCommandType.TICKET, command.type)
        assertEquals(QueueStatus.PENDING, command.status)

        every { storage.getQueueTasksByStatus("kkm-1", "OFFLINE", setOf("PENDING")) } returns emptyList()
        assertTrue(adapter.getCommandsByStatus("kkm-1", QueueLane.OFFLINE, setOf(QueueStatus.PENDING)).isEmpty())
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
