package io.github.texport.superkassa.core.data.impl.adapter.receipt

import io.mockk.mockk
import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdServiceInfo
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptBranding
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptLayoutType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftStatus
import io.github.texport.superkassa.core.domain.api.port.integration.QrCodeGeneratorPort
import kotlin.test.Test
import kotlin.test.assertTrue

class ReceiptRenderAdapterTest {

    private val qrCodeGenerator = mockk<QrCodeGeneratorPort>(relaxed = true)
    private val adapter = ReceiptRenderAdapter(qrCodeGenerator)

    private val defaultBranding = ReceiptBranding(
        paperWidthMm = 80,
        themeColor = "indigo"
    )

    private val defaultKkm = KkmInfo(
        id = "kkm-1",
        createdAt = 1774567890000L,
        updatedAt = 1774567890000L,
        mode = "PRODUCTION",
        state = "READY",
        registrationNumber = "RN-123456",
        factoryNumber = "FN-987654",
        branding = defaultBranding,
        ofdServiceInfo = OfdServiceInfo(
            orgTitle = "Test Org",
            orgInn = "123456789012",
            orgAddress = "Test Address",
            orgAddressKz = "Test Address Kz",
            orgOkved = "62010",
            geoLatitude = 432389,
            geoLongitude = 768897,
            geoSource = "GPS"
        )
    )

    @Test
    fun testRenderHtml() {
        val receipt = ReceiptRequest(
            kkmId = "kkm-1",
            pin = "0000",
            operation = io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType.SELL,
            items = emptyList(),
            payments = emptyList(),
            total = io.github.texport.superkassa.core.domain.api.model.common.Money(0, 0),
            taken = io.github.texport.superkassa.core.domain.api.model.common.Money(0, 0),
            change = io.github.texport.superkassa.core.domain.api.model.common.Money(0, 0),
            idempotencyKey = "key-1",
            taxRegime = io.github.texport.superkassa.core.domain.api.model.common.TaxRegime.VAT_PAYER,
            defaultVatGroup = io.github.texport.superkassa.core.domain.api.model.common.VatGroup.VAT_16
        )
        val doc = FiscalDocumentSnapshot(
            id = "doc-1",
            cashboxId = "kkm-1",
            shiftId = "shift-1",
            docType = "CHECK",
            docNo = 1,
            shiftNo = 1,
            createdAt = 1774567890000L,
            totalAmount = 0L,
            currency = "KZT",
            fiscalSign = "FS-123",
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = "DELIVERED",
            deliveredAt = null,
            receiptUrl = "https://example.com/receipt/1",
            registrationNumber = "RN-123456",
            taxpayerName = "Test Org",
            taxpayerBin = "123456789012",
            taxpayerAddress = "Test Address",
            factoryNumber = "FN-987654",
            ofdProvider = "TEST_PROVIDER"
        )
        val html = adapter.renderHtml(receipt, doc, defaultKkm, ReceiptLayoutType.TAPE_80MM)
        assertTrue(html.isNotEmpty())
    }

    @Test
    fun testRenderXReportHtml() {
        val shift = ShiftInfo(
            id = "shift-1",
            kkmId = "kkm-1",
            shiftNo = 1,
            openedAt = 1774567890000L,
            closedAt = null,
            status = ShiftStatus.OPEN
        )
        val html = adapter.renderXReportHtml(shift, emptyMap(), defaultKkm, "DELIVERED", ReceiptLayoutType.TAPE_80MM)
        assertTrue(html.isNotEmpty())
    }

    @Test
    fun testRenderOpenShiftHtml() {
        val shift = ShiftInfo(
            id = "shift-1",
            kkmId = "kkm-1",
            shiftNo = 1,
            openedAt = 1774567890000L,
            closedAt = null,
            status = ShiftStatus.OPEN
        )
        val html = adapter.renderOpenShiftHtml(shift, defaultKkm, "DELIVERED", "123", ReceiptLayoutType.TAPE_80MM)
        assertTrue(html.isNotEmpty())
    }

    @Test
    fun testRenderCloseShiftHtml() {
        val shift = ShiftInfo(
            id = "shift-1",
            kkmId = "kkm-1",
            shiftNo = 1,
            openedAt = 1774567890000L,
            closedAt = 1774567990000L,
            status = ShiftStatus.CLOSED
        )
        val html = adapter.renderCloseShiftHtml(shift, emptyMap(), defaultKkm, "DELIVERED", "1234", ReceiptLayoutType.TAPE_80MM)
        assertTrue(html.isNotEmpty())
    }

    @Test
    fun testRenderCashOperationHtml() {
        val doc = FiscalDocumentSnapshot(
            id = "doc-1",
            cashboxId = "kkm-1",
            shiftId = "shift-1",
            docType = "CASH_IN",
            docNo = 1,
            shiftNo = 1,
            createdAt = 1774567890000L,
            totalAmount = 1000L,
            currency = "KZT",
            fiscalSign = "FS-123",
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = "DELIVERED",
            deliveredAt = null,
            receiptUrl = "https://example.com/receipt/1",
            registrationNumber = "RN-123456",
            taxpayerName = "Test Org",
            taxpayerBin = "123456789012",
            taxpayerAddress = "Test Address",
            factoryNumber = "FN-987654",
            ofdProvider = "TEST_PROVIDER"
        )
        val html = adapter.renderCashOperationHtml(doc, defaultKkm, ReceiptLayoutType.TAPE_80MM)
        assertTrue(html.isNotEmpty())
    }

    @Test
    fun testRenderPreviewHtml() {
        val html = adapter.renderPreviewHtml(defaultBranding, ReceiptLayoutType.TAPE_80MM)
        assertTrue(html.isNotEmpty())
    }
}
