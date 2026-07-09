package io.github.texport.superkassa.receiptrenderer.impl.renderer.report

import io.github.texport.superkassa.core.domain.model.kkm.*
import io.github.texport.superkassa.core.domain.model.shift.*

internal class XReportRenderer : ZxReportCommonRenderer() {

    fun render(
        shift: ShiftInfo,
        counters: Map<String, Long>,
        kkm: KkmInfo,
        ofdStatus: String?
    ): String {
        return renderZxReportHtml(
            titleKey = "x_report",
            shift = shift,
            counters = counters,
            isZReport = false,
            kkm = kkm,
            ofdStatus = ofdStatus
        )
    }
}
