package io.github.texport.superkassa.core.domain.impl.usecase.delivery

import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryFailure
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryOutcome
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryRequest
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryRetryPolicy
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryTask
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryTaskStatus
import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.model.settings.PrintDeliverySettings
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.integration.DeliveryPort
import io.github.texport.superkassa.core.domain.api.port.integration.DocumentConvertPort
import io.github.texport.superkassa.core.domain.api.port.internal.ReceiptRenderPort
import io.github.texport.superkassa.core.string.api.TrilingualMessage
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

/** Отправка задач доставки: занятие, итог, повтор по паузе и окончательный отказ. */
class SendDeliveryTasksUseCaseTest {
    private val tasks = MemoryDeliveryTasks()
    private val storage = tasks.storage()
    private val sent = mutableListOf<DeliveryRequest>()
    private var answer: (DeliveryRequest) -> DeliveryOutcome = { DeliveryOutcome.DELIVERED }
    private val channel = object : DeliveryPort {
        override fun deliver(request: DeliveryRequest) = error("send is used")
        override fun send(request: DeliveryRequest): DeliveryOutcome = answer(request).also { sent += request }
    }
    private var now = 1_000L
    private val clock = mockk<ClockPort> { every { now() } answers { now } }
    private val convert = mockk<DocumentConvertPort>()
    private val render = mockk<ReceiptRenderPort> { every { renderHtml(any(), any(), any()) } returns "<p>чек</p>" }
    private val policy = DeliveryRetryPolicy(attempts = 2, firstPause = 10.seconds, lease = 60.seconds)
    private fun sender(print: PrintDeliverySettings? = null) =
        SendDeliveryTasksUseCase(storage, channel, DeliveryRequests(storage, { print }, convert, render), clock, policy)

    init {
        every { storage.findFiscalDocumentWithReceiptPayload("doc-1") } returns (document("https://bfd.kz/r/1") to mockk<ReceiptRequest>())
    }

    @Test
    fun `принятая каналом задача доставлена, второй раз не уходит`() {
        tasks.rows += task("LINK")

        assertEquals(1, sender().sendDue(limit = 10))
        assertEquals(0, sender().sendDue(limit = 10))
        assertEquals(DeliveryTaskStatus.DELIVERED to 1, tasks.rows.single().let { it.status to it.attempts })
        assertEquals(listOf("https://bfd.kz/r/1"), sent.map { it.payloadUrl })
    }

    @Test
    fun `отказ ждёт паузы с причиной, после предела попыток - окончательный`() {
        tasks.rows += task("HTML")
        answer = { DeliveryOutcome.failed(DeliveryFailure("DELIVERY_PROVIDER_REJECTED", TrilingualMessage.mono("нет")), retryable = true) }

        sender().sendDue(limit = 10)
        val waiting = tasks.rows.single()
        now += 9_999L
        val early = sender().sendDue(limit = 10)
        now += 1L
        sender().sendDue(limit = 10)

        assertEquals(Triple(DeliveryTaskStatus.PENDING, 11_000L, "DELIVERY_PROVIDER_REJECTED"), Triple(waiting.status, waiting.nextAttemptAt, waiting.failure?.code))
        assertEquals(0, early)
        assertEquals(DeliveryTaskStatus.FAILED to 2, tasks.rows.single().let { it.status to it.attempts })
        assertEquals("<p>чек</p>", sent.first().payloadBytes?.decodeToString())
    }

    @Test
    fun `отказ, который повтором не лечится, окончательный сразу`() {
        tasks.rows += task("HTML")
        answer = { DeliveryOutcome.failed(DeliveryFailure("DELIVERY_SMS_NOT_CONFIGURED", TrilingualMessage.mono("нет")), retryable = false) }

        sender().sendDue(limit = 10)

        assertEquals(DeliveryTaskStatus.FAILED to 1, tasks.rows.single().let { it.status to it.attempts })
    }

    @Test
    fun `нет документа или ссылки - окончательный отказ без канала`() {
        tasks.rows += task("LINK").copy(id = "doc-2/SMS/LINK", documentId = "doc-2")
        tasks.rows += task("LINK")
        every { storage.findFiscalDocumentWithReceiptPayload("doc-2") } returns null
        every { storage.findFiscalDocumentWithReceiptPayload("doc-1") } returns (document(null) to mockk<ReceiptRequest>())

        sender().sendDue(limit = 10)

        assertEquals(
            listOf(DeliveryRequests.DOCUMENT_MISSING, DeliveryRequests.LINK_MISSING),
            tasks.rows.map { it.failure?.code }
        )
        assertEquals(emptyList(), sent)
    }

    @Test
    fun `сбой рисования и падение канала - отказ с повтором, а не падение фона`() {
        tasks.rows += task("PDF")
        tasks.rows += task("ESC_POS").copy(id = "doc-1/PRINT/ESC_POS", channel = "PRINT")
        every { convert.htmlToPdf(any()) } throws IllegalStateException("no renderer")
        every { convert.htmlToEscPos(any(), 80) } returns byteArrayOf(1)
        answer = { throw IllegalArgumentException("socket") }

        sender(PrintDeliverySettings(paperWidthMm = 80)).sendDue(limit = 10)

        assertEquals(
            listOf(SendDeliveryTasksUseCase.PREPARATION_FAILED, SendDeliveryTasksUseCase.CHANNEL_FAILED),
            tasks.rows.map { it.failure?.code }
        )
        assertEquals(listOf(DeliveryTaskStatus.PENDING, DeliveryTaskStatus.PENDING), tasks.rows.map { it.status })
    }

    @Test
    fun `задачи документа отправляются по требованию, занятую другим не трогают`() {
        tasks.rows += task("HTML")
        tasks.rows += task("IMAGE").copy(id = "doc-1/SMS/IMAGE", nextAttemptAt = now + 1)
        every { convert.htmlToImage(any()) } returns byteArrayOf(2)
        every { convert.htmlToEscPos(any(), 58) } returns byteArrayOf(3)

        val after = sender(PrintDeliverySettings(paperWidthMm = 70)).sendDocument("doc-1")

        assertEquals(listOf(DeliveryTaskStatus.DELIVERED, DeliveryTaskStatus.PENDING), after.map { it.status })
        assertEquals(1, sent.size)
    }

    private fun task(payloadType: String) = DeliveryTask(
        id = "doc-1/SMS/$payloadType",
        kkmId = "kkm-1",
        documentId = "doc-1",
        channel = "SMS",
        destination = "+77010000001",
        payloadType = payloadType,
        nextAttemptAt = now,
        createdAt = now
    )

    private fun document(url: String?) = FiscalDocumentSnapshot(
        id = "doc-1", cashboxId = "kkm-1", shiftId = "shift-1", docType = "CHECK", docNo = 1L, shiftNo = 1L,
        createdAt = 1L, totalAmount = 100L, currency = "KZT", fiscalSign = "fs", autonomousSign = null,
        isAutonomous = false, ofdStatus = "SENT", deliveredAt = 1L, receiptUrl = url
    )
}
