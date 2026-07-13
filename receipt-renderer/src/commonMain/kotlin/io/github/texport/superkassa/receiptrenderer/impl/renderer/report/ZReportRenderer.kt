package io.github.texport.superkassa.receiptrenderer.impl.renderer.report

import io.github.texport.superkassa.core.domain.api.model.kkm.*
import io.github.texport.superkassa.core.domain.api.model.shift.*

internal class ZReportRenderer : ZxReportCommonRenderer() {

    fun render(
        shift: ShiftInfo,
        counters: Map<String, Long>,
        kkm: KkmInfo,
        ofdStatus: String?,
        docNo: String? = null
    ): String {
        return renderZxReportHtml(
            titleKey = "z_report",
            shift = shift,
            counters = counters,
            isZReport = true,
            kkm = kkm,
            ofdStatus = ofdStatus,
            docNo = docNo
        )
    }
}
