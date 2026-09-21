package io.github.texport.superkassa.receiptrenderer.impl.renderer.report

import io.github.texport.superkassa.core.domain.api.model.kkm.*
import io.github.texport.superkassa.core.domain.api.model.shift.*
import io.github.texport.superkassa.core.domain.api.model.zxreport.ZxReportInput

internal class ZReportRenderer(
    now: () -> Long = { kotlin.time.Clock.System.now().toEpochMilliseconds() }
) : ZxReportCommonRenderer(now) {

    fun render(
        shift: ShiftInfo,
        counters: Map<String, Long>,
        kkm: KkmInfo,
        ofdStatus: String?,
        docNo: String? = null
    ): String {
        return renderZxReportHtml(
            titleKey = TITLE_KEY,
            shift = shift,
            counters = counters,
            isZReport = true,
            kkm = kkm,
            ofdStatus = ofdStatus,
            docNo = docNo
        )
    }

    /** Z-отчёт по готовым сменным итогам: смены этой кассы за ними нет. */
    fun render(
        report: ZxReportInput,
        kkm: KkmInfo,
        ofdStatus: String?,
        docNo: String? = null
    ): String {
        return renderZxReportHtml(
            titleKey = TITLE_KEY,
            report = report,
            isZReport = true,
            kkm = kkm,
            ofdStatus = ofdStatus,
            docNo = docNo
        )
    }

    private companion object {
        const val TITLE_KEY = "z_report"
    }
}
