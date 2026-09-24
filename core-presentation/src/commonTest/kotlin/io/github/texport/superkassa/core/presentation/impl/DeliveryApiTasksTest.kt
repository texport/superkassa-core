package io.github.texport.superkassa.core.presentation.impl

import io.github.texport.superkassa.core.domain.api.model.auth.KkmUser
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryFailure
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryOutcome
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryTask
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryTaskStatus
import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.model.settings.CoreMode
import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import io.github.texport.superkassa.core.domain.api.model.settings.StorageSettings
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.integration.DeliveryPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.api.port.internal.PinHasherPort
import io.github.texport.superkassa.core.domain.impl.usecase.auth.MemoryPinAttempts
import io.github.texport.superkassa.core.domain.impl.usecase.auth.PinGuard
import io.github.texport.superkassa.core.presentation.api.model.delivery.ReceiptDeliveryState
import io.github.texport.superkassa.core.string.api.TrilingualMessage
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

/** Доставка чека для журнала: состояние по каналам, повтор и заход фона. */
class DeliveryApiTasksTest {
    private val storage = mockk<StoragePort>(relaxed = true)
    private val pinHasher = mockk<PinHasherPort> { every { hash("1234") } returns "hash-1" }
    private val delivery = mockk<DeliveryPort>()
    private val clock = mockk<ClockPort> { every { now() } returns 100L }
    private val settings = CoreSettings(mode = CoreMode.DESKTOP, storage = StorageSettings(engine = "SQLITE", jdbcUrl = "jdbc:sqlite:k.db"))
    private val api = DeliveryApiImpl(storage, pinHasher, delivery, { settings.delivery }, mockk(), mockk(), PinGuard(MemoryPinAttempts()), clock)
    private val failure = DeliveryFailure("DELIVERY_SMS_NOT_CONFIGURED", TrilingualMessage("не настроен", "бапталмаған", "not configured"))
    private val waiting = DeliveryTask("doc-1/SMS/LINK", "kkm-1", "doc-1", "SMS", "+7701", "LINK", nextAttemptAt = 900L, createdAt = 1L)
    private val document = FiscalDocumentSnapshot(
        id = "doc-1", cashboxId = "kkm-1", shiftId = "shift-1", docType = "CHECK", docNo = 1L, shiftNo = 1L,
        createdAt = 1L, totalAmount = 100L, currency = "KZT", fiscalSign = "fs", autonomousSign = null,
        isAutonomous = false, ofdStatus = "SENT", deliveredAt = 1L, receiptUrl = "https://bfd.kz/r"
    )

    init {
        every { storage.findUserByPin("kkm-1", "hash-1") } returns KkmUser("user-1", "Нурлан", UserRole.CASHIER, createdAt = 1L)
        every { storage.findFiscalDocumentById("doc-1") } returns document
        every { storage.findFiscalDocumentWithReceiptPayload("doc-1") } returns (document to mockk<ReceiptRequest>())
    }

    @Test
    fun `журнал видит ожидание со сроком и отказ с кодом на трёх языках, без получателя`() {
        val failed = waiting.copy(id = "doc-1/EMAIL/PDF", channel = "EMAIL", status = DeliveryTaskStatus.FAILED, failure = failure)
        every { storage.deliveryTasksOf("doc-1") } returns listOf(waiting, failed)

        val rows = api.receiptDeliveries("kkm-1", "doc-1", "1234")

        assertEquals(listOf(ReceiptDeliveryState.PENDING to 900L, ReceiptDeliveryState.FAILED to null), rows.map { it.state to it.nextAttemptAt })
        assertEquals("DELIVERY_SMS_NOT_CONFIGURED" to "бапталмаған", rows[1].failureCode to rows[1].failureMessage?.kk)
        assertEquals(null, rows[0].failureMessage)
    }

    @Test
    fun `повтор отправляет задачу сразу и отвечает её итогом`() {
        val due = waiting.copy(nextAttemptAt = 100L)
        var row = due
        every { storage.deliveryTasksOf("doc-1") } answers { listOf(row) }
        every { storage.claimDeliveryTask(due.id, 100L, any()) } returns true
        every { storage.saveDeliveryTask(any()) } answers { row = firstArg() }
        every { delivery.send(any()) } returns DeliveryOutcome.DELIVERED

        val rows = api.resendReceipt("kkm-1", "doc-1", "1234")
        val pairs = api.retryReceiptDelivery("kkm-1", "doc-1", "1234")

        assertEquals(listOf(ReceiptDeliveryState.DELIVERED), rows.map { it.state })
        assertEquals(listOf("SMS" to true), pairs)
    }

    @Test
    fun `заход фона отправляет задачи, чей срок наступил`() {
        every { storage.dueDeliveryTasks(100L, 5) } returns listOf(waiting.copy(nextAttemptAt = 100L))
        every { storage.claimDeliveryTask(any(), any(), any()) } returns false

        assertEquals(0, api.sendDueDeliveries(5))
    }
}
