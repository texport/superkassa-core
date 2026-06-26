package kz.mybrain.superkassa.core.domain.port

import kz.mybrain.superkassa.core.domain.model.FiscalDocumentSnapshot
import kz.mybrain.superkassa.core.domain.model.ReceiptRequest
import kz.mybrain.superkassa.core.domain.model.ShiftInfo
import kz.mybrain.superkassa.core.domain.model.ReceiptBranding
import kz.mybrain.superkassa.core.domain.model.KkmInfo
import kz.mybrain.superkassa.core.domain.model.ReceiptLayoutType

/**
 * Порт рендеринга чека и отчетов в HTML (для печати, PDF, доставки).
 */
interface ReceiptRenderPort {
    /**
     * Рендерит чек в HTML.
     * @param receipt Данные чека (позиции, оплата, итог).
     * @param doc Фискальный документ (номер, подпись, дата).
     * @return HTML-строка чека.
     */
    fun renderHtml(
        receipt: ReceiptRequest,
        doc: FiscalDocumentSnapshot,
        kkm: KkmInfo,
        layoutType: ReceiptLayoutType? = null
    ): String

    fun renderXReportHtml(
        shift: ShiftInfo,
        counters: Map<String, Long>,
        kkm: KkmInfo,
        ofdStatus: String?,
        layoutType: ReceiptLayoutType? = null
    ): String

    fun renderOpenShiftHtml(
        shift: ShiftInfo,
        kkm: KkmInfo,
        ofdStatus: String?,
        docNo: String? = null,
        layoutType: ReceiptLayoutType? = null
    ): String

    fun renderCloseShiftHtml(
        shift: ShiftInfo,
        counters: Map<String, Long>,
        kkm: KkmInfo,
        ofdStatus: String?,
        docNo: String? = null,
        layoutType: ReceiptLayoutType? = null
    ): String

    fun renderCashOperationHtml(
        doc: FiscalDocumentSnapshot,
        kkm: KkmInfo,
        layoutType: ReceiptLayoutType? = null
    ): String

    fun renderPreviewHtml(
        branding: ReceiptBranding,
        layoutType: ReceiptLayoutType? = null
    ): String
}
