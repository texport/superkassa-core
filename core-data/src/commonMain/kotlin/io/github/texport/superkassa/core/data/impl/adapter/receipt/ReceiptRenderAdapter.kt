package io.github.texport.superkassa.core.data.impl.adapter.receipt

import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptBranding
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptLayoutType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.port.integration.QrCodeGeneratorPort
import io.github.texport.superkassa.core.domain.api.port.internal.ReceiptRenderPort
import io.github.texport.superkassa.receiptrenderer.api.ReceiptRendererApi
import io.github.texport.superkassa.receiptrenderer.api.createReceiptRendererApi

/**
 * Адаптер для рендеринга чеков и отчетов, оборачивающий библиотеку receipt-renderer.
 *
 * Размещается в data-слое согласно принципам чистой архитектуры.
 */
internal class ReceiptRenderAdapter(
    qrCodeGenerator: QrCodeGeneratorPort
) : ReceiptRenderPort {

    private val renderer: ReceiptRendererApi = createReceiptRendererApi(qrCodeGenerator)

    override fun renderHtml(
        receipt: ReceiptRequest,
        doc: FiscalDocumentSnapshot,
        kkm: KkmInfo,
        layoutType: ReceiptLayoutType?
    ): String = renderer.renderHtml(receipt, doc, kkm, layoutType)

    override fun renderXReportHtml(
        shift: ShiftInfo,
        counters: Map<String, Long>,
        kkm: KkmInfo,
        ofdStatus: String?,
        layoutType: ReceiptLayoutType?
    ): String = renderer.renderXReportHtml(shift, counters, kkm, ofdStatus, layoutType)

    override fun renderOpenShiftHtml(
        shift: ShiftInfo,
        kkm: KkmInfo,
        ofdStatus: String?,
        docNo: String?,
        layoutType: ReceiptLayoutType?
    ): String = renderer.renderOpenShiftHtml(shift, kkm, ofdStatus, docNo, layoutType)

    override fun renderCloseShiftHtml(
        shift: ShiftInfo,
        counters: Map<String, Long>,
        kkm: KkmInfo,
        ofdStatus: String?,
        docNo: String?,
        layoutType: ReceiptLayoutType?
    ): String = renderer.renderCloseShiftHtml(shift, counters, kkm, ofdStatus, docNo, layoutType)

    override fun renderCashOperationHtml(
        doc: FiscalDocumentSnapshot,
        kkm: KkmInfo,
        layoutType: ReceiptLayoutType?
    ): String = renderer.renderCashOperationHtml(doc, kkm, layoutType)

    override fun renderPreviewHtml(
        branding: ReceiptBranding,
        layoutType: ReceiptLayoutType?
    ): String = renderer.renderPreviewHtml(branding, layoutType)
}
