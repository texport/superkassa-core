package io.github.texport.superkassa.core.domain.impl.usecase.queue

import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.domain.api.model.queue.QueueDispatchStatus
import io.github.texport.superkassa.core.domain.api.model.queue.QueueTask
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.model.settings.DeliveryChannelSettings
import io.github.texport.superkassa.core.domain.api.model.settings.DeliverySettings
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.impl.helper.ReceiptDeliveryHelper
import io.github.texport.superkassa.core.domain.impl.usecase.delivery.MemoryDeliveryTasks
import io.github.texport.superkassa.core.domain.impl.usecase.delivery.ReceiptDeliveryPlan
import io.github.texport.superkassa.core.domain.impl.usecase.ofd.SendFiscalCommandUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.receipt.DeliverReceiptUseCase
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

/** Чек, который БФД принял из очереди, уходит покупателю так же, как пробитый онлайн. */
class QueuedReceiptDeliveryTest {
    private val tasks = MemoryDeliveryTasks()
    private val storage = tasks.storage()
    private val clock = mockk<ClockPort> { every { now() } returns 50L }
    private val send = mockk<SendFiscalCommandUseCase>()
    private val plan = ReceiptDeliveryPlan {
        DeliverySettings(channels = listOf(DeliveryChannelSettings("SMS", payloadType = "BOTH", destination = "+7701")))
    }
    private val deliver = DeliverReceiptUseCase(mockk<ReceiptDeliveryHelper>(), storage, plan, clock)
    private val process = ProcessQueueCommandUseCase(send, storage, clock, deliver)
    private val document = FiscalDocumentSnapshot(
        id = "doc-1", cashboxId = "kkm-1", shiftId = "shift-1", docType = "CHECK", docNo = 1L, shiftNo = 1L,
        createdAt = 1L, totalAmount = 100L, currency = "KZT", fiscalSign = null, autonomousSign = "as",
        isAutonomous = true, ofdStatus = "PENDING", deliveredAt = null
    )

    init {
        every { storage.findFiscalDocumentWithReceiptPayload("doc-1") } returns (document to mockk<ReceiptRequest>())
        every { storage.findFiscalDocumentById("doc-1") } returns document
    }

    @Test
    fun `досланный чек ставит доставку со ссылкой БФД, повтор досылки её не удваивает`() {
        accept("TICKET")
        accept("TICKET")

        assertEquals(listOf("doc-1/SMS/LINK", "doc-1/SMS/PDF"), tasks.rows.map { it.id })
    }

    @Test
    fun `досланные деньги и отчёты покупателю ничего не ставят`() {
        accept("MONEY_PLACEMENT")

        assertEquals(emptyList(), tasks.rows)
    }

    @Test
    fun `чек без сохранённого запроса доставки не ставит`() {
        every { storage.findFiscalDocumentWithReceiptPayload("doc-1") } returns null

        accept("TICKET")

        assertEquals(emptyList(), tasks.rows)
    }

    private fun accept(type: String) {
        every { send.execute("kkm-1", any(), "doc-1") } returns
            OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0, receiptUrl = "https://bfd.kz/r/1")
        val task = QueueTask(
            id = "q-1", cashboxId = "kkm-1", lane = "OFFLINE", type = type, payloadRef = "doc-1",
            status = "PENDING", attempt = 1, nextAttemptAt = null, lastError = null, createdAt = 1L
        )
        assertEquals(QueueDispatchStatus.SENT, process.execute(task).status)
    }
}
