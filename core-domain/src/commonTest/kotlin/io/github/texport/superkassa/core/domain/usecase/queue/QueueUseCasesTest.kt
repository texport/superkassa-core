package io.github.texport.superkassa.core.domain.usecase.queue

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.github.texport.superkassa.core.domain.exception.ConflictException
import io.github.texport.superkassa.core.domain.exception.ValidationException
import io.github.texport.superkassa.core.domain.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.model.kkm.KkmMode
import io.github.texport.superkassa.core.domain.model.kkm.KkmState
import io.github.texport.superkassa.core.domain.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.domain.model.queue.QueueDispatchStatus
import io.github.texport.superkassa.core.domain.model.queue.QueueTask
import io.github.texport.superkassa.core.domain.port.ClockPort
import io.github.texport.superkassa.core.domain.port.StoragePort
import io.github.texport.superkassa.core.domain.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.domain.usecase.ofd.SendFiscalCommandUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class QueueUseCasesTest {

    private val authorizeUserUseCase = mockk<AuthorizeUserUseCase>()
    private val storage = mockk<StoragePort>(relaxed = true)
    private val clock = mockk<ClockPort>()
    private val sendFiscalCommand = mockk<SendFiscalCommandUseCase>()

    private val listQueueItems = ListQueueItemsUseCase(storage, authorizeUserUseCase)
    private val processQueueCommand = ProcessQueueCommandUseCase(sendFiscalCommand, storage, clock)
    private val retryFailedQueueItems = RetryFailedQueueItemsUseCase(storage, authorizeUserUseCase)

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

        val mockCommand = QueueTask(
            id = "cmd-1",
            cashboxId = "kkm-1",
            lane = "OFFLINE",
            type = "TICKET",
            payloadRef = "payload-1",
            status = "FAILED",
            attempt = 1,
            nextAttemptAt = 1000L,
            lastError = "RU: Ошибка | KK: Қате | EN: Error",
            createdAt = 1000L
        )
        every { storage.listQueueTasksByCashbox("kkm-1", "OFFLINE", 100, 0) } returns listOf(mockCommand)

        val list = listQueueItems.execute("kkm-1", "1234")
        assertEquals(1, list.size)
        assertEquals("cmd-1", list[0].id)
        assertEquals("RU: Ошибка | KK: Қате | EN: Error", list[0].lastError)
        assertEquals("Ошибка", list[0].errorRu)
        assertEquals("Қате", list[0].errorKk)
        assertEquals("Error", list[0].errorEn)
    }

    @Test
    fun testProcessQueueCommandSuccess() {
        val mockCommand = QueueTask(
            id = "cmd-1",
            cashboxId = "kkm-1",
            lane = "OFFLINE",
            type = "TICKET",
            payloadRef = "payload-1",
            status = "PENDING",
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
        every { storage.findFiscalDocumentById("payload-1") } returns null
        every { clock.now() } returns 2000L

        val res = processQueueCommand.execute(mockCommand)
        assertEquals(QueueDispatchStatus.SENT, res.status)
        verify {
            storage.updateReceiptStatus("payload-1", "fs-123", "as-123", "SENT", 2000L, null)
        }
    }

    @Test
    fun testProcessQueueCommandOfdFailed() {
        val mockCommand = QueueTask(
            id = "cmd-1",
            cashboxId = "kkm-1",
            lane = "OFFLINE",
            type = "TICKET",
            payloadRef = "payload-1",
            status = "PENDING",
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
        assertEquals(QueueDispatchStatus.FAILED, res.status)
        assertEquals("Server error", res.errorMessage)
        assertEquals(62000L, res.retryAt)
        assertEquals("Ошибка отправки в ОФД: Server error", res.errorRu)
        assertEquals("ОФД-ға жіберу қатесі: Server error", res.errorKk)
        assertEquals("OFD delivery failure: Server error", res.errorEn)
    }

    @Test
    fun testProcessQueueCommandOfdTimeout() {
        val mockCommand = QueueTask(
            id = "cmd-1",
            cashboxId = "kkm-1",
            lane = "OFFLINE",
            type = "MONEY_PLACEMENT",
            payloadRef = "payload-1",
            status = "PENDING",
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
        assertEquals(QueueDispatchStatus.FAILED, res.status)
        assertEquals("OFD timeout", res.errorMessage)
        assertEquals(32000L, res.retryAt)
        assertEquals("Тайм-аут ожидания ответа от ОФД", res.errorRu)
        assertEquals("ОФД жауабын күту уақыты бітті", res.errorKk)
        assertEquals("OFD connection timeout", res.errorEn)
    }

    @Test
    fun testProcessQueueCommandVariousTypes() {
        every { clock.now() } returns 1000L
        val types = listOf(
            "REPORT_X",
            "REPORT_Z",
            "CLOSE_SHIFT",
            "INFO",
            "SYSTEM"
        )
        types.forEach { type ->
            val cmd = QueueTask(
                id = "cmd-1",
                cashboxId = "kkm-1",
                lane = "OFFLINE",
                type = type,
                payloadRef = "payload-1",
                status = "PENDING",
                attempt = 1,
                nextAttemptAt = null,
                lastError = null,
                createdAt = 1000L
            )
            val ofdResult = OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0)
            every { sendFiscalCommand.execute("kkm-1", any(), "payload-1") } returns ofdResult
            val res = processQueueCommand.execute(cmd)
            assertEquals(QueueDispatchStatus.SENT, res.status)
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
        every { storage.listQueueTasksByCashbox("kkm-1", "OFFLINE", 100, 0) } returns emptyList()

        val count = retryFailedQueueItems.execute("kkm-1", "1234")
        assertEquals(0, count)
    }

    @Test
    fun testRetryFailedQueueItemsSuccessWithItems() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkmProg // mode PROGRAMMING
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findOpenShift("kkm-1") } returns null

        val failedCmd = QueueTask(
            id = "cmd-1",
            cashboxId = "kkm-1",
            lane = "OFFLINE",
            type = "TICKET",
            payloadRef = "payload-1",
            status = "FAILED",
            attempt = 2,
            nextAttemptAt = 1000L,
            lastError = "error",
            createdAt = 1000L
        )
        val pendingCmd = QueueTask(
            id = "cmd-2",
            cashboxId = "kkm-1",
            lane = "OFFLINE",
            type = "TICKET",
            payloadRef = "payload-2",
            status = "PENDING",
            attempt = 1,
            nextAttemptAt = null,
            lastError = null,
            createdAt = 1000L
        )
        every { storage.listQueueTasksByCashbox("kkm-1", "OFFLINE", 100, 0) } returns listOf(failedCmd, pendingCmd)
        every { storage.updateQueueTaskStatus("cmd-1", "PENDING", 2, null, null) } returns true

        val count = retryFailedQueueItems.execute("kkm-1", "1234")
        assertEquals(1, count)
    }
}
