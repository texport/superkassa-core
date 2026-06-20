package kz.mybrain.superkassa.core.domain.port

import kz.mybrain.superkassa.core.domain.model.FiscalDocumentSnapshot
import kz.mybrain.superkassa.core.domain.model.ReceiptRequest
import kz.mybrain.superkassa.core.domain.model.ShiftInfo

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
    fun renderHtml(receipt: ReceiptRequest, doc: FiscalDocumentSnapshot): String

    fun renderXReportHtml(shift: ShiftInfo, counters: Map<String, Long>): String

    fun renderOpenShiftHtml(shift: ShiftInfo): String

    fun renderCloseShiftHtml(shift: ShiftInfo, counters: Map<String, Long>): String

    fun renderCashOperationHtml(doc: FiscalDocumentSnapshot): String
}
