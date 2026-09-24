package io.github.texport.superkassa.core.domain.impl.usecase.receipt

import io.github.texport.superkassa.core.domain.api.exception.ConflictException
import io.github.texport.superkassa.core.domain.api.exception.NotFoundException
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryFailure
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryTaskStatus
import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.model.settings.DeliveryChannelSettings
import io.github.texport.superkassa.core.domain.api.model.settings.DeliverySettings
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.impl.helper.ReceiptDeliveryHelper
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.delivery.GetReceiptDeliveriesUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.delivery.MemoryDeliveryTasks
import io.github.texport.superkassa.core.domain.impl.usecase.delivery.ReceiptDeliveryPlan
import io.github.texport.superkassa.core.domain.impl.usecase.delivery.SendDeliveryTasksUseCase
import io.github.texport.superkassa.core.string.api.TrilingualMessage
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Чек ставит задачи доставки, повтор идёт через них же, журнал их показывает. */
class ReceiptDeliveryTasksTest {
    private val tasks = MemoryDeliveryTasks()
    private val storage = tasks.storage()
    private val clock = mockk<ClockPort> { every { now() } returns 50L }
    private val helper = mockk<ReceiptDeliveryHelper>(relaxed = true)
    private val sender = mockk<SendDeliveryTasksUseCase> {
        every { sendDocument(any()) } answers { tasks.rows.filter { it.documentId == firstArg<String>() } }
    }
    private val auth = mockk<AuthorizeUserUseCase>(relaxed = true)
    private val plan = ReceiptDeliveryPlan { DeliverySettings(channels = listOf(DeliveryChannelSettings("SMS", destination = "+7701"))) }
    private val document = FiscalDocumentSnapshot(
        id = "doc-1", cashboxId = "kkm-1", shiftId = "shift-1", docType = "CHECK", docNo = 1L, shiftNo = 1L,
        createdAt = 1L, totalAmount = 100L, currency = "KZT", fiscalSign = "fs", autonomousSign = null,
        isAutonomous = false, ofdStatus = "SENT", deliveredAt = 1L
    )
    private val receipt = mockk<ReceiptRequest>()

    init {
        every { storage.findFiscalDocumentWithReceiptPayload("doc-1") } returns (document to receipt)
        every { storage.findFiscalDocumentById("doc-1") } returns document
    }

    @Test
    fun `принятый чек ставит задачи и не доставляет в потоке пробития`() {
        DeliverReceiptUseCase(helper, storage, plan, clock).execute("kkm-1", "doc-1", receipt, document, null, null)

        assertEquals(listOf("doc-1/SMS/PDF" to 50L), tasks.rows.map { it.id to it.nextAttemptAt })
        verify(exactly = 0) { helper.deliverReceipt(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `сбой записи задач не выдаёт себя за отказ чека`() {
        every { storage.addDeliveryTasks(any()) } throws IllegalStateException("disk is full")

        DeliverReceiptUseCase(helper, storage, plan, clock).execute("kkm-1", "doc-1", receipt, document, null, null)

        verify(exactly = 0) { helper.deliverReceipt(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `повтор ставит недостающее и заново открывает окончательно не удавшееся`() {
        DeliverReceiptUseCase(helper, storage, plan, clock).execute("kkm-1", "doc-1", receipt, document, null, null)
        val failure = DeliveryFailure("DELIVERY_PROVIDER_REJECTED", TrilingualMessage.mono("нет"))
        tasks.rows[0] = tasks.rows[0].copy(status = DeliveryTaskStatus.FAILED, attempts = 5, failure = failure)

        val result = retry().execute("kkm-1", "doc-1", "1234")

        assertEquals(listOf("SMS" to false), result)
        assertEquals(DeliveryTaskStatus.PENDING to 0, tasks.rows.single().let { it.status to it.attempts })
    }

    @Test
    fun `повтор без каналов - отказ «не настроено»`() {
        val retry = RetryReceiptDeliveryUseCase(storage, auth, helper, ReceiptDeliveryPlan { null }, sender, clock)

        val error = assertFailsWith<ConflictException> { retry.resend("kkm-1", "doc-1", "1234") }

        assertEquals("DELIVERY_NOT_CONFIGURED", error.code)
    }

    @Test
    fun `журнал видит задачи только своего документа`() {
        DeliverReceiptUseCase(helper, storage, plan, clock).execute("kkm-1", "doc-1", receipt, document, null, null)
        every { storage.findFiscalDocumentById("doc-2") } returns document.copy(id = "doc-2", cashboxId = "kkm-2")
        val deliveries = GetReceiptDeliveriesUseCase(storage, auth)

        assertEquals(listOf("SMS"), deliveries.execute("kkm-1", "doc-1", "1234").map { it.channel })
        assertFailsWith<NotFoundException> { deliveries.execute("kkm-1", "doc-2", "1234") }
        assertFailsWith<NotFoundException> { deliveries.execute("kkm-1", "doc-3", "1234") }
    }

    private fun retry() = RetryReceiptDeliveryUseCase(storage, auth, helper, plan, sender, clock)
}
