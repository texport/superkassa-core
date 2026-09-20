package io.github.texport.superkassa.receiptrenderer.impl

import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState
import io.github.texport.superkassa.core.domain.api.model.receipt.PaymentType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptItem
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptPayment
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.receiptrenderer.impl.adapter.DefaultQrCodeGeneratorAdapter
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReceiptRenderersTest {

    private val sampleRequest = ReceiptRequest(
        kkmId = "kkm-1",
        pin = "1111",
        operation = ReceiptOperationType.SELL,
        items = listOf(
            ReceiptItem(
                name = "Товар 1",
                sectionCode = "001",
                price = Money(bills = 100L, coins = 0),
                quantity = 1000L,
                sum = Money(bills = 100L, coins = 0)
            )
        ),
        payments = listOf(
            ReceiptPayment(
                type = PaymentType.CASH,
                sum = Money(bills = 100L, coins = 0)
            )
        ),
        total = Money(bills = 100L, coins = 0),
        idempotencyKey = "key-1"
    )

    private val sampleDoc = FiscalDocumentSnapshot(
        id = "doc-1",
        cashboxId = "kkm-1",
        shiftId = "shift-1",
        docType = "CHECK",
        docNo = 1L,
        shiftNo = 1L,
        createdAt = 1000L,
        totalAmount = 100L,
        currency = "KZT",
        fiscalSign = "FS-12345",
        autonomousSign = "AS-123",
        isAutonomous = false,
        ofdStatus = "DELIVERED",
        deliveredAt = 1000L
    )

    private val sampleKkm = KkmInfo(
        id = "kkm-1",
        createdAt = 0L,
        updatedAt = 0L,
        mode = "ACTIVE",
        state = KkmState.ACTIVE.name
    )

    @Test
    fun testEscPosRenderer() {
        val renderer = EscPosReceiptRenderer()
        val bytes = renderer.renderReceiptBytes(sampleRequest, sampleDoc, sampleKkm)
        assertTrue(bytes.isNotEmpty())
    }

    @Test
    fun testSvgRenderer() {
        val renderer = SvgReceiptRenderer()
        val svg = renderer.renderReceiptSvg(sampleRequest, sampleDoc, sampleKkm)
        assertTrue(svg.contains("<svg"))
        assertTrue(svg.contains("SUPERKASSA FISCAL TICKET"))
    }

    @Test
    fun testQrCodeGeneratorAdapter() {
        val adapter = DefaultQrCodeGeneratorAdapter()
        val dataUri = adapter.generatePngDataUri("https://test.ru", 200)
        assertNotNull(dataUri)
        assertTrue(dataUri.startsWith("data:image/svg+xml;"))
    }
}
