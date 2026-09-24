package io.github.texport.superkassa.core.presentation.impl

import io.github.texport.superkassa.core.domain.impl.usecase.auth.MemoryPinAttempts
import io.github.texport.superkassa.core.domain.impl.usecase.auth.PinGuard
import io.mockk.every
import io.mockk.mockk
import io.github.texport.superkassa.core.domain.api.model.auth.KkmUser
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.api.port.internal.PinHasherPort
import io.github.texport.superkassa.core.domain.api.port.integration.DeliveryPort
import io.github.texport.superkassa.core.domain.api.port.integration.DocumentConvertPort
import io.github.texport.superkassa.core.domain.api.port.internal.ReceiptRenderPort
import io.github.texport.superkassa.core.presentation.api.DeliveryApi
import io.github.texport.superkassa.core.domain.api.exception.ConflictException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeliveryApiImplTest {

    private val storage = mockk<StoragePort>(relaxed = true)
    private val pinHasher = mockk<PinHasherPort>(relaxed = true)
    private val delivery = mockk<DeliveryPort>(relaxed = true)
    private val coreSettings = mockk<CoreSettings>(relaxed = true)
    private val documentConvertPort = mockk<DocumentConvertPort>(relaxed = true)
    private val receiptRenderPort = mockk<ReceiptRenderPort>(relaxed = true)

    private val deliveryApi: DeliveryApi = DeliveryApiImpl(
        storage = storage,
        pinHasher = pinHasher,
        delivery = delivery,
        coreSettings = coreSettings,
        documentConvertPort = documentConvertPort,
        receiptRenderPort = receiptRenderPort,
        pinGuard = PinGuard(MemoryPinAttempts()),
        clock = mockk(relaxed = true)
    )

    @Test
    fun testRetryReceiptDeliveryWithoutChannelsIsRefused() {
        val testUser = KkmUser(
            id = "user-1",
            name = "Cashier 1",
            role = UserRole.CASHIER,
            createdAt = 1000L
        )
        every { pinHasher.hash("1234") } returns "hash-1"
        every { storage.findUserByPin("kkm-1", "hash-1") } returns testUser

        val mockSnapshot = FiscalDocumentSnapshot(
            id = "doc-1",
            cashboxId = "kkm-1",
            shiftId = "shift-1",
            docType = "CHECK",
            docNo = 1L,
            shiftNo = 1L,
            createdAt = 1000L,
            totalAmount = 100L,
            currency = "KZT",
            fiscalSign = "fs",
            autonomousSign = "as",
            isAutonomous = false,
            ofdStatus = "DELIVERED",
            deliveredAt = 1000L
        )
        every { storage.findFiscalDocumentWithReceiptPayload("doc-1") } returns (mockSnapshot to mockk<ReceiptRequest>())
        every { delivery.deliver(any()) } returns true

        // Каналов доставки не настроено: отправлять чек некуда. Прежде
        // повтор отвечал успехом с пустым списком результатов, и кассир
        // считал чек отправленным.
        val error = assertFailsWith<ConflictException> {
            deliveryApi.retryReceiptDelivery("kkm-1", "doc-1", "1234")
        }
        assertEquals("DELIVERY_NOT_CONFIGURED", error.code)
    }

    @Test
    fun testRetryReceiptDeliveryOfNonFiscalDocumentIsRefused() {
        val testUser = KkmUser(
            id = "user-1",
            name = "Cashier 1",
            role = UserRole.CASHIER,
            createdAt = 1000L
        )
        every { pinHasher.hash("1234") } returns "hash-1"
        every { storage.findUserByPin("kkm-1", "hash-1") } returns testUser

        // Чек, который ОФД отверг: ни фискального признака, ни автономного.
        val rejected = FiscalDocumentSnapshot(
            id = "doc-2",
            cashboxId = "kkm-1",
            shiftId = "shift-1",
            docType = "SALE",
            docNo = null,
            shiftNo = 1L,
            createdAt = 1000L,
            totalAmount = 100L,
            currency = "KZT",
            fiscalSign = null,
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = "FAILED",
            ofdErrorCode = 13,
            deliveredAt = null
        )
        every { storage.findFiscalDocumentWithReceiptPayload("doc-2") } returns (rejected to mockk<ReceiptRequest>())

        val error = assertFailsWith<ConflictException> {
            deliveryApi.retryReceiptDelivery("kkm-1", "doc-2", "1234")
        }
        assertEquals("DOCUMENT_NOT_FISCALIZED", error.code)
    }
}
