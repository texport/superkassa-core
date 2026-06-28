package kz.mybrain.superkassa.core.domain.usecase.queue

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kz.mybrain.superkassa.core.domain.exception.ConflictException
import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kz.mybrain.superkassa.core.domain.model.kkm.KkmMode
import kz.mybrain.superkassa.core.domain.model.kkm.KkmState
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandResult
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandStatus
import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.usecase.auth.AuthorizeUserUseCase
import kz.mybrain.superkassa.core.domain.usecase.ofd.SendFiscalCommandUseCase
import kz.mybrain.superkassa.offline_queue.domain.model.QueueCommand
import kz.mybrain.superkassa.offline_queue.domain.model.QueueCommandType
import kz.mybrain.superkassa.offline_queue.domain.model.QueueLane
import kz.mybrain.superkassa.offline_queue.domain.model.QueueStatus
import kz.mybrain.superkassa.offline_queue.domain.port.QueueStoragePort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class QueueUseCasesTest {

    private val queueStorage = mockk<QueueStoragePort>()
    private val authorizeUserUseCase = mockk<AuthorizeUserUseCase>()
    private val storage = mockk<StoragePort>(relaxed = true)
    private val clock = mockk<ClockPort>()
    private val sendFiscalCommand = mockk<SendFiscalCommandUseCase>()

    private val listQueueItems = ListQueueItemsUseCase(queueStorage, authorizeUserUseCase)
    private val processQueueCommand = ProcessQueueCommandUseCase(sendFiscalCommand, storage, clock)
    private val retryFailedQueueItems = RetryFailedQueueItemsUseCase(storage, queueStorage, authorizeUserUseCase)

    private val kkmActive = KkmInfo(id = "kkm-1", createdAt = 0, updatedAt = 0, mode = "ACTIVE", state = "ACTIVE")
    private val kkmProg = KkmInfo(id = "kkm-1", createdAt = 0, updatedAt = 0, mode = KkmMode.PROGRAMMING.name, state = KkmState.PROGRAMMING.name)

    init {
        every { storage.inTransaction<Any>(any()) } answers {
            val block = firstArg<() -> Any>()
            block()
        }
        every { storage.inTransaction<Int>(any()) } answers {
            val block = firstArg<() -> Int>()
            block()
        }
    }

    @Test
    fun testListQueueItemsSuccess() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkmActive
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()

        val mockCommand = QueueCommand(
            id = "cmd-1",
            cashboxId = "kkm-1",
            lane = QueueLane.OFFLINE,
            type = QueueCommandType.TICKET,
            payloadRef = "payload-1",
            status = QueueStatus.FAILED,
            attempt = 1,
            nextAttemptAt = 1000L,
            lastError = "error",
            createdAt = 1000L
        )
        every { queueStorage.listByCashbox("kkm-1", QueueLane.OFFLINE, 100, 0) } returns listOf(mockCommand)

        val list = listQueueItems.execute("kkm-1", "1234")
        assertEquals(1, list.size)
        assertEquals("cmd-1", list[0].id)
    }

    @Test
    fun testProcessQueueCommandSuccess() {
        val mockCommand = QueueCommand(
            id = "cmd-1",
            cashboxId = "kkm-1",
            lane = QueueLane.OFFLINE,
            type = QueueCommandType.TICKET,
            payloadRef = "payload-1",
            status = QueueStatus.PENDING,
            attempt = 1,
            nextAttemptAt = null,
            lastError = null,
            createdAt = 1000L
        )
        val ofdResult = OfdCommandResult(
            status = OfdCommandStatus.OK,
            fiscalSign = "fs-123",
            autonomousSign = "as-123",
            resultCode = 0
        )
        every { sendFiscalCommand.execute("kkm-1", any(), "payload-1") } returns ofdResult
        every { clock.now() } returns 2000L

        val res = processQueueCommand.execute(mockCommand)
        assertEquals(QueueStatus.SENT, res.status)
        verify {
            storage.updateReceiptStatus("payload-1", "fs-123", "as-123", "SENT", 2000L, false)
        }
    }

    @Test
    fun testProcessQueueCommandOfdFailed() {
        val mockCommand = QueueCommand(
            id = "cmd-1",
            cashboxId = "kkm-1",
            lane = QueueLane.OFFLINE,
            type = QueueCommandType.TICKET,
            payloadRef = "payload-1",
            status = QueueStatus.PENDING,
            attempt = 1,
            nextAttemptAt = null,
            lastError = null,
            createdAt = 1000L
        )
        val ofdResult = OfdCommandResult(
            status = OfdCommandStatus.FAILED,
            errorMessage = "Server error",
            resultCode = 500
        )
        every { sendFiscalCommand.execute("kkm-1", any(), "payload-1") } returns ofdResult
        every { clock.now() } returns 2000L

        val res = processQueueCommand.execute(mockCommand)
        assertEquals(QueueStatus.FAILED, res.status)
        assertEquals("Server error", res.errorMessage)
        assertEquals(62000L, res.retryAt)
    }

    @Test
    fun testProcessQueueCommandOfdTimeout() {
        val mockCommand = QueueCommand(
            id = "cmd-1",
            cashboxId = "kkm-1",
            lane = QueueLane.OFFLINE,
            type = QueueCommandType.MONEY_PLACEMENT,
            payloadRef = "payload-1",
            status = QueueStatus.PENDING,
            attempt = 1,
            nextAttemptAt = null,
            lastError = null,
            createdAt = 1000L
        )
        val ofdResult = OfdCommandResult(
            status = OfdCommandStatus.TIMEOUT,
            resultCode = 408
        )
        every { sendFiscalCommand.execute("kkm-1", any(), "payload-1") } returns ofdResult
        every { clock.now() } returns 2000L

        val res = processQueueCommand.execute(mockCommand)
        assertEquals(QueueStatus.FAILED, res.status)
        assertEquals("OFD timeout", res.errorMessage)
        assertEquals(32000L, res.retryAt)
    }

    @Test
    fun testProcessQueueCommandVariousTypes() {
        every { clock.now() } returns 1000L
        val types = listOf(
            QueueCommandType.REPORT_X,
            QueueCommandType.REPORT_Z,
            QueueCommandType.CLOSE_SHIFT,
            QueueCommandType.INFO,
            QueueCommandType.SYSTEM
        )
        types.forEach { type ->
            val cmd = QueueCommand(
                id = "cmd-1",
                cashboxId = "kkm-1",
                lane = QueueLane.OFFLINE,
                type = type,
                payloadRef = "payload-1",
                status = QueueStatus.PENDING,
                attempt = 1,
                nextAttemptAt = null,
                lastError = null,
                createdAt = 1000L
            )
            val ofdResult = OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0)
            every { sendFiscalCommand.execute("kkm-1", any(), "payload-1") } returns ofdResult
            val res = processQueueCommand.execute(cmd)
            assertEquals(QueueStatus.SENT, res.status)
        }
    }

    @Test
    fun testRetryFailedQueueItemsValidationFailed() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkmActive // mode ACTIVE
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()

        assertFailsWith<ValidationException> {
            retryFailedQueueItems.execute("kkm-1", "1234")
        }
    }

    @Test
    fun testRetryFailedQueueItemsConflictShiftOpen() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkmProg // mode PROGRAMMING
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findOpenShift("kkm-1") } returns mockk()

        assertFailsWith<ConflictException> {
            retryFailedQueueItems.execute("kkm-1", "1234")
        }
    }

    @Test
    fun testRetryFailedQueueItemsSuccessEmpty() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkmProg // mode PROGRAMMING
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findOpenShift("kkm-1") } returns null
        every { queueStorage.listByCashbox("kkm-1", QueueLane.OFFLINE, 100, 0) } returns emptyList()

        val count = retryFailedQueueItems.execute("kkm-1", "1234")
        assertEquals(0, count)
    }

    @Test
    fun testRetryFailedQueueItemsSuccessWithItems() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkmProg // mode PROGRAMMING
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findOpenShift("kkm-1") } returns null

        val failedCmd = QueueCommand(
            id = "cmd-1",
            cashboxId = "kkm-1",
            lane = QueueLane.OFFLINE,
            type = QueueCommandType.TICKET,
            payloadRef = "payload-1",
            status = QueueStatus.FAILED,
            attempt = 2,
            nextAttemptAt = 1000L,
            lastError = "error",
            createdAt = 1000L
        )
        val pendingCmd = QueueCommand(
            id = "cmd-2",
            cashboxId = "kkm-1",
            lane = QueueLane.OFFLINE,
            type = QueueCommandType.TICKET,
            payloadRef = "payload-2",
            status = QueueStatus.PENDING,
            attempt = 1,
            nextAttemptAt = null,
            lastError = null,
            createdAt = 1000L
        )
        every { queueStorage.listByCashbox("kkm-1", QueueLane.OFFLINE, 100, 0) } returns listOf(failedCmd, pendingCmd)
        every { queueStorage.updateStatus("cmd-1", QueueStatus.PENDING, 2, null, null) } returns true

        val count = retryFailedQueueItems.execute("kkm-1", "1234")
        assertEquals(1, count)
    }
}
