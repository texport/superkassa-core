package io.github.texport.superkassa.receiptrenderer.impl

import io.github.texport.superkassa.receiptrenderer.api.ReceiptRendererApi
import io.github.texport.superkassa.receiptrenderer.impl.renderer.operation.CashOperationRenderer
import io.github.texport.superkassa.receiptrenderer.impl.renderer.report.OpenShiftRenderer
import io.github.texport.superkassa.receiptrenderer.impl.renderer.report.XReportRenderer
import io.github.texport.superkassa.receiptrenderer.impl.renderer.report.ZReportRenderer
import io.github.texport.superkassa.receiptrenderer.impl.renderer.ticket.SaleReceiptRenderer
import io.github.texport.superkassa.core.domain.model.kkm.*
import io.github.texport.superkassa.core.domain.model.ofd.*
import io.github.texport.superkassa.core.domain.model.shift.*
import io.github.texport.superkassa.core.domain.model.common.*
import io.github.texport.superkassa.core.domain.model.receipt.*
import io.github.texport.superkassa.core.domain.port.QrCodeGeneratorPort
import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.DocumentConstants

/**
 * Внутренняя реализация движка рендеринга чеков.
 */
internal class ReceiptRendererApiImpl(
    qrCodeGenerator: QrCodeGeneratorPort
) : ReceiptRendererApi {

    private val saleRenderer = SaleReceiptRenderer(qrCodeGenerator)
    private val xReportRenderer = XReportRenderer()
    private val zReportRenderer = ZReportRenderer()
    private val openShiftRenderer = OpenShiftRenderer()
    private val cashOperationRenderer = CashOperationRenderer()

    private fun ReceiptLayoutType.toPaperWidthMm(): Int = when (this) {
        ReceiptLayoutType.TAPE_80MM -> DocumentConstants.TAPE_WIDTH_80_MM
        ReceiptLayoutType.TAPE_58MM -> DocumentConstants.TAPE_WIDTH_58_MM
        ReceiptLayoutType.FULLSCREEN -> DocumentConstants.FULLSCREEN_WIDTH
    }

    private fun overrideKkmLayout(kkm: KkmInfo, layoutType: ReceiptLayoutType?): KkmInfo {
        if (layoutType == null) return kkm
        return kkm.copy(branding = kkm.branding.copy(paperWidthMm = layoutType.toPaperWidthMm()))
    }

    private fun overrideBrandingLayout(branding: ReceiptBranding, layoutType: ReceiptLayoutType?): ReceiptBranding {
        if (layoutType == null) return branding
        return branding.copy(paperWidthMm = layoutType.toPaperWidthMm())
    }

    override fun renderHtml(
        receipt: ReceiptRequest,
        doc: FiscalDocumentSnapshot,
        kkm: KkmInfo,
        layoutType: ReceiptLayoutType?
    ): String {
        return saleRenderer.render(receipt, doc, overrideKkmLayout(kkm, layoutType))
    }

    override fun renderXReportHtml(
        shift: ShiftInfo,
        counters: Map<String, Long>,
        kkm: KkmInfo,
        ofdStatus: String?,
        layoutType: ReceiptLayoutType?
    ): String {
        return xReportRenderer.render(shift, counters, overrideKkmLayout(kkm, layoutType), ofdStatus)
    }

    override fun renderOpenShiftHtml(
        shift: ShiftInfo,
        kkm: KkmInfo,
        ofdStatus: String?,
        docNo: String?,
        layoutType: ReceiptLayoutType?
    ): String {
        return openShiftRenderer.render(shift, overrideKkmLayout(kkm, layoutType), ofdStatus, docNo)
    }

    override fun renderCloseShiftHtml(
        shift: ShiftInfo,
        counters: Map<String, Long>,
        kkm: KkmInfo,
        ofdStatus: String?,
        docNo: String?,
        layoutType: ReceiptLayoutType?
    ): String {
        return zReportRenderer.render(shift, counters, overrideKkmLayout(kkm, layoutType), ofdStatus, docNo)
    }

    override fun renderCashOperationHtml(
        doc: FiscalDocumentSnapshot,
        kkm: KkmInfo,
        layoutType: ReceiptLayoutType?
    ): String {
        return cashOperationRenderer.render(doc, overrideKkmLayout(kkm, layoutType))
    }

    override fun renderPreviewHtml(branding: ReceiptBranding, layoutType: ReceiptLayoutType?): String {
        val mockItems = listOf(
            ReceiptItem(
                name = "Товар Предпросмотра / Алдын ала қарау тауары",
                sectionCode = "001",
                quantity = 1L,
                price = Money(150, 0),
                sum = Money(150, 0),
                measureUnitCode = UnitOfMeasurement.PIECE.code,
                vatGroup = VatGroup.VAT_16
            ),
            ReceiptItem(
                name = "Услуга Демонстрации / Демонстрациялық қызмет",
                sectionCode = "001",
                quantity = 2L,
                price = Money(250, 0),
                sum = Money(500, 0),
                measureUnitCode = UnitOfMeasurement.PIECE.code,
                vatGroup = VatGroup.VAT_16
            )
        )
        val mockPayments = listOf(
            ReceiptPayment(
                type = PaymentType.CASH,
                sum = Money(300, 0)
            ),
            ReceiptPayment(
                type = PaymentType.CARD,
                sum = Money(350, 0)
            )
        )
        val mockReceipt = ReceiptRequest(
            kkmId = "KKM-PREVIEW",
            pin = "0000",
            operation = ReceiptOperationType.SELL,
            items = mockItems,
            payments = mockPayments,
            total = Money(650, 0),
            taken = Money(650, 0),
            change = Money(0, 0),
            idempotencyKey = "mock-preview-id",
            taxRegime = TaxRegime.VAT_PAYER,
            defaultVatGroup = VatGroup.VAT_16
        )
        val mockDoc = FiscalDocumentSnapshot(
            id = "mock-preview-id",
            cashboxId = "KKM-PREVIEW",
            shiftId = "shift-7",
            docType = "CHECK",
            docNo = 12345,
            shiftNo = 7,
            createdAt = kotlin.time.Clock.System.now().toEpochMilliseconds(),
            totalAmount = 65000L,
            currency = "KZT",
            fiscalSign = "987654321012",
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = "DELIVERED",
            deliveredAt = null,
            receiptUrl = "https://consumer.oofd.kz/r/12345?r=NZ7700123456&i=123456789012&d=1774567890&s=650.00&f=987654321012",
            registrationNumber = "NZ7700123456",
            taxpayerName = "ТОО ДЕМО-БРЕНДИНГ / DEMO-BRANDING",
            taxpayerBin = "123456789012",
            taxpayerAddress = "г. Алматы, пр. Абая, 123 / Алматы қ., Абай даңғылы, 123",
            factoryNumber = "SW7700987654",
            ofdProvider = "KAZAKHTELECOM"
        )
        val mockKkm = KkmInfo(
            id = "KKM-PREVIEW",
            createdAt = 1774567890000L,
            updatedAt = 1774567890000L,
            mode = "PRODUCTION",
            state = "READY",
            registrationNumber = "NZ7700123456",
            factoryNumber = "SW7700987654",
            branding = overrideBrandingLayout(branding, layoutType),
            ofdServiceInfo = OfdServiceInfo(
                orgTitle = "ТОО ДЕМО-БРЕНДИНГ / DEMO-BRANDING",
                orgInn = "123456789012",
                orgAddress = "г. Алматы, пр. Абая, 123",
                orgAddressKz = "Алматы қ., Абай даңғылы, 123",
                orgOkved = "62010",
                geoLatitude = 432389,
                geoLongitude = 768897,
                geoSource = "GPS"
            )
        )
        return saleRenderer.render(mockReceipt, mockDoc, mockKkm)
    }
}
