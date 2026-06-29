package kz.mybrain.superkassa.core.data.adapter

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandResult
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandStatus
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandType
import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.usecase.ofd.SendFiscalCommandUseCase
import kz.mybrain.superkassa.offline_queue.application.model.DispatchResult
import kz.mybrain.superkassa.offline_queue.domain.model.QueueCommand
import kz.mybrain.superkassa.offline_queue.domain.model.QueueCommandType
import kz.mybrain.superkassa.offline_queue.domain.model.QueueLane
import kz.mybrain.superkassa.offline_queue.domain.model.QueueStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class OfdQueueCommandHandlerAdapterTest {

    @Test
    fun testHandleSuccess() {
        val sendFiscalCommand = mockk<SendFiscalCommandUseCase>()
        val storage = mockk<StoragePort>()
        val clock = mockk<ClockPort>()

        every { clock.now() } returns 10000L
        every {
            sendFiscalCommand.execute("c1", OfdCommandType.TICKET, "ref1")
        } returns OfdCommandResult(
            status = OfdCommandStatus.OK,
            fiscalSign = "fs123",
            autonomousSign = null
        )
        every {
            storage.updateReceiptStatus("ref1", "fs123", null, "SENT", 10000L, false)
        } returns true

        val adapter = OfdQueueCommandHandlerAdapter(sendFiscalCommand, storage, clock)

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

        val result = adapter.handle(command)
        assertEquals(QueueStatus.SENT, result.status)

        verify {
            storage.updateReceiptStatus("ref1", "fs123", null, "SENT", 10000L, false)
        }
    }
}
