package io.github.texport.superkassa.core.presentation.impl

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
import kotlin.test.Test
import kotlin.test.assertTrue

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
        receiptRenderPort = receiptRenderPort
    )

    @Test
    fun testRetryReceiptDelivery() {
        val testUser = KkmUser(
            id = "user-1",
            name = "Cashier 1",
            role = UserRole.CASHIER,
            pin = "hash-1",
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

        val results = deliveryApi.retryReceiptDelivery("kkm-1", "doc-1", "1234")
        assertTrue(results.isEmpty())
    }
}
