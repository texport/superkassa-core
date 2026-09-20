package io.github.texport.superkassa.receiptrenderer.impl.renderer.report

import io.github.texport.superkassa.core.domain.api.model.kkm.*
import io.github.texport.superkassa.core.domain.api.model.shift.*
import io.github.texport.superkassa.core.domain.api.model.zxreport.ZxReportInput

internal class XReportRenderer : ZxReportCommonRenderer() {

    fun render(
        shift: ShiftInfo,
        counters: Map<String, Long>,
        kkm: KkmInfo,
        ofdStatus: String?
    ): String {
        return renderZxReportHtml(
            titleKey = TITLE_KEY,
            shift = shift,
            counters = counters,
            isZReport = false,
            kkm = kkm,
            ofdStatus = ofdStatus
        )
    }

    /** X-отчёт по готовым сменным итогам: смены этой кассы за ними нет. */
    fun render(
        report: ZxReportInput,
        kkm: KkmInfo,
        ofdStatus: String?,
        docNo: String? = null
    ): String {
        return renderZxReportHtml(
            titleKey = TITLE_KEY,
            report = report,
            isZReport = false,
            kkm = kkm,
            ofdStatus = ofdStatus,
            docNo = docNo
        )
    }

    private companion object {
        const val TITLE_KEY = "x_report"
    }
}
