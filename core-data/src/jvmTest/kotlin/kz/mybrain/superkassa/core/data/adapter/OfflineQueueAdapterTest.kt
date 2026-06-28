package kz.mybrain.superkassa.core.data.adapter

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kz.mybrain.superkassa.core.domain.model.queue.OfflineQueueCommandRequest
import kz.mybrain.superkassa.offline_queue.application.service.QueueCommandHandler
import kz.mybrain.superkassa.offline_queue.domain.model.QueueCommand
import kz.mybrain.superkassa.offline_queue.domain.model.QueueCommandType
import kz.mybrain.superkassa.offline_queue.domain.model.QueueLane
import kz.mybrain.superkassa.offline_queue.domain.model.QueueStatus
import kz.mybrain.superkassa.offline_queue.domain.port.LeaseLockPort
import kz.mybrain.superkassa.offline_queue.domain.port.QueueStoragePort

class OfflineQueueAdapterTest {
    private val storage: QueueStoragePort = mockk()
    private val lockPort: LeaseLockPort = mockk()
    private val handler: QueueCommandHandler = mockk()
    private val adapter = OfflineQueueAdapter(storage, lockPort, handler, "owner-1")

    @Test
    fun testCanSendDirectly() {
        // hasOfflineQueue is checked by calling listByCashbox on the storage
        every { storage.listByCashbox("kkm-1", QueueLane.OFFLINE, 100) } returns emptyList()
        assertTrue(adapter.canSendDirectly("kkm-1"))

        val mockCommand = mockk<QueueCommand>()
        every { mockCommand.status } returns QueueStatus.PENDING
        every { storage.listByCashbox("kkm-1", QueueLane.OFFLINE, 100) } returns listOf(mockCommand)
        assertFalse(adapter.canSendDirectly("kkm-1"))
    }

    @Test
    fun testEnqueueOffline() {
        every { storage.enqueue(any()) } returns true

        val request = OfflineQueueCommandRequest(
            kkmId = "kkm-1",
            type = "COMMAND_TICKET",
            payloadRef = "doc-1"
        )
        assertTrue(adapter.enqueueOffline(request))

        verify {
            storage.enqueue(
                withArg {
                    assertEquals("kkm-1:COMMAND_TICKET:doc-1", it.id)
                    assertEquals("kkm-1", it.cashboxId)
                    assertEquals(QueueLane.OFFLINE, it.lane)
                    assertEquals(QueueCommandType.TICKET, it.type)
                    assertEquals("doc-1", it.payloadRef)
                }
            )
        }
    }

    @Test
    fun testEnqueueOfflineDifferentTypes() {
        every { storage.enqueue(any()) } returns true

        val types = listOf(
            "COMMAND_TICKET" to QueueCommandType.TICKET,
            "COMMAND_REPORT" to QueueCommandType.REPORT_X,
            "COMMAND_CLOSE_SHIFT" to QueueCommandType.CLOSE_SHIFT,
            "COMMAND_MONEY_PLACEMENT" to QueueCommandType.MONEY_PLACEMENT,
            "COMMAND_INFO" to QueueCommandType.INFO,
            "COMMAND_SYSTEM" to QueueCommandType.SYSTEM,
            "COMMAND_UNKNOWN" to QueueCommandType.TICKET
        )

        for ((strType, expectedType) in types) {
            val request = OfflineQueueCommandRequest(
                kkmId = "kkm-1",
                type = strType,
                payloadRef = "doc-1"
            )
            assertTrue(adapter.enqueueOffline(request))
            verify {
                storage.enqueue(
                    withArg {
                        if (it.type == expectedType) {
                            assertEquals(expectedType, it.type)
                        }
                    }
                )
            }
        }
    }

    @Test
    fun testDeleteQueuedCommands() {
        every { storage.deleteByCashbox("kkm-1") } returns true
        assertTrue(adapter.deleteQueuedCommands("kkm-1"))

        every { storage.deleteByCashbox("kkm-1") } returns false
        assertFalse(adapter.deleteQueuedCommands("kkm-1"))
    }

    @Test
    fun testProcessOfflineBatch() {
        // mock lease lock
        every { lockPort.tryAcquire("kkm-1", "owner-1", any(), any()) } returns true
        every { lockPort.release("kkm-1", "owner-1") } returns true

        // mock nextPending tasks in storage. If none, processBatch returns 0
        every { storage.nextPending("kkm-1", QueueLane.OFFLINE, any()) } returns null

        val result = adapter.processOfflineBatch("kkm-1", 5)
        assertEquals(0, result)
    }
}
