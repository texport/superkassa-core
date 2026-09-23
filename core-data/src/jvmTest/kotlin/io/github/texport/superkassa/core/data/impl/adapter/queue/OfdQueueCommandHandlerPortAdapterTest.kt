package io.github.texport.superkassa.core.data.impl.adapter.queue

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.usecase.ofd.SendFiscalCommandUseCase
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
        val storage = mockk<StoragePort>(relaxed = true)
        val clock = mockk<ClockPort>()

        every { clock.now() } returns 10000L
        // Документ по ссылке обязан находиться: без него узел не знает,
        // чей это ответ, и статус доставки ставить некуда.
        every { storage.findFiscalDocumentById("ref1") } returns mockk(relaxed = true)
        every {
            sendFiscalCommand.execute("c1", OfdCommandType.TICKET, "ref1")
        } returns OfdCommandResult(
            status = OfdCommandStatus.OK,
            resultCode = 0,
            fiscalSign = "fs123",
            autonomousSign = null
        )
        every {
            storage.updateReceiptStatus("ref1", "fs123", any(), "SENT", null, 10000L, any())
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
            storage.updateReceiptStatus("ref1", "fs123", any(), "SENT", null, 10000L, any())
        }
    }

    @Test
    fun testHandleFailure() {
        val sendFiscalCommand = mockk<SendFiscalCommandUseCase>()
        val storage = mockk<StoragePort>(relaxed = true)
        val clock = mockk<ClockPort>()

        every { clock.now() } returns 10000L
        every {
            sendFiscalCommand.execute("c1", OfdCommandType.TICKET, "ref1")
        } returns OfdCommandResult(
            status = OfdCommandStatus.FAILED,
            errorMessage = "Server error"
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
        // Обмена не было: запрос не собрался и до ОФД не дошёл. Задача
        // остаётся в очереди — причина бывает в состоянии кассы, а не
        // в документе; отбраковка теряла фискальный документ молча.
        assertEquals(QueueStatus.FAILED, result.status)
        assertEquals("Server error", result.errorMessage)
        val err = result.error
        assertNotNull(err)
        assertEquals("Ошибка отправки в БФД: Server error", err.ru)
        assertEquals("БФД-ға жіберу қатесі: Server error", err.kk)
        assertEquals("BFD delivery failure: Server error", err.en)
    }
}
