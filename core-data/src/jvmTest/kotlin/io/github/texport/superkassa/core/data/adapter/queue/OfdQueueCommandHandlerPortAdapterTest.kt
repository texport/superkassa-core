package io.github.texport.superkassa.core.data.adapter.queue

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.github.texport.superkassa.core.domain.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.domain.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.port.ClockPort
import io.github.texport.superkassa.core.domain.port.StoragePort
import io.github.texport.superkassa.core.domain.usecase.ofd.SendFiscalCommandUseCase
import io.github.texport.superkassa.core.string.api.TrilingualMessage
import io.github.texport.superkassa.offlinequeue.api.model.QueueCommand
import io.github.texport.superkassa.offlinequeue.api.model.QueueCommandType
import io.github.texport.superkassa.offlinequeue.api.model.QueueLane
import io.github.texport.superkassa.offlinequeue.api.model.QueueStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class OfdQueueCommandHandlerPortAdapterTest {

    @Test
    fun testHandleSuccess() {
        val sendFiscalCommand = mockk<SendFiscalCommandUseCase>()
        val storage = mockk<StoragePort>()
        val clock = mockk<ClockPort>()

        every { clock.now() } returns 10000L
        every { storage.findFiscalDocumentById("ref1") } returns null
        every {
            sendFiscalCommand.execute("c1", OfdCommandType.TICKET, "ref1")
        } returns OfdCommandResult(
            status = OfdCommandStatus.OK,
            fiscalSign = "fs123",
            autonomousSign = null
        )
        every {
            storage.updateReceiptStatus("ref1", "fs123", null, "SENT", 10000L, null)
        } returns true

        val adapter = OfdQueueCommandHandlerPortAdapter(sendFiscalCommand, storage, clock)

        val command = QueueCommand(
            id = "1",
            cashboxId = "c1",
            lane = QueueLane.OFFLINE,
            type = QueueCommandType.TICKET,
            payloadRef = "ref1",
            createdAt = 5000L,
            status = QueueStatus.PENDING,
            attempt = 0
        )

        val result = adapter.handle(command, renewLock = { true })
        assertEquals(QueueStatus.SENT, result.status)

        verify {
            storage.updateReceiptStatus("ref1", "fs123", null, "SENT", 10000L, null)
        }
    }

    @Test
    fun testHandleFailure() {
        val sendFiscalCommand = mockk<SendFiscalCommandUseCase>()
        val storage = mockk<StoragePort>()
        val clock = mockk<ClockPort>()

        every { clock.now() } returns 10000L
        every {
            sendFiscalCommand.execute("c1", OfdCommandType.TICKET, "ref1")
        } returns OfdCommandResult(
            status = OfdCommandStatus.FAILED,
            errorMessage = "Server error",
            resultCode = 500
        )

        val adapter = OfdQueueCommandHandlerPortAdapter(sendFiscalCommand, storage, clock)

        val command = QueueCommand(
            id = "1",
            cashboxId = "c1",
            lane = QueueLane.OFFLINE,
            type = QueueCommandType.TICKET,
            payloadRef = "ref1",
            createdAt = 5000L,
            status = QueueStatus.PENDING,
            attempt = 0
        )

        val result = adapter.handle(command, renewLock = { true })
        assertEquals(QueueStatus.FAILED, result.status)
        assertEquals("Server error", result.errorMessage)
        val err = result.error
        assertNotNull(err)
        assertEquals("Ошибка отправки в ОФД: Server error", err.ru)
        assertEquals("ОФД-ға жіберу қатесі: Server error", err.kk)
        assertEquals("OFD delivery failure: Server error", err.en)
    }
}
