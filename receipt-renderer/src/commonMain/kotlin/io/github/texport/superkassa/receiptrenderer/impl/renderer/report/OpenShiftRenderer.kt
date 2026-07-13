package io.github.texport.superkassa.receiptrenderer.impl.renderer.report

import io.github.texport.superkassa.core.domain.api.model.kkm.*
import io.github.texport.superkassa.core.domain.api.model.shift.*

import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.BaseDocumentRenderer

import io.github.texport.superkassa.receiptrenderer.impl.renderer.base.StandardDocumentInput

internal class OpenShiftRenderer : BaseDocumentRenderer() {

    fun render(
        shift: ShiftInfo,
        kkm: KkmInfo,
        ofdStatus: String?,
        docNo: String? = null
    ): String {
        return renderStandardDocument(
            StandardDocumentInput(
                titleKey = "open_shift",
                kkm = kkm,
                createdAt = shift.openedAt,
                shiftNo = shift.shiftNo,
                docNo = docNo,
                ofdStatus = ofdStatus,
                isFiscal = false,
                showOfdStatus = false,
                bodyContent = ""
            )
        )
    }
}
