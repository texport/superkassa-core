package io.github.texport.superkassa.core.presentation.impl.mapper

import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.model.report.ReportResult
import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus
import io.github.texport.superkassa.core.presentation.api.model.shift.*

object ShiftMapper {

    fun toResponse(shift: ShiftInfo): ShiftResponse = ShiftResponse(
        id = shift.id,
        kkmId = shift.kkmId,
        shiftNo = shift.shiftNo,
        status = ShiftStatus.valueOf(shift.status.name),
        openedAt = shift.openedAt,
        closedAt = shift.closedAt,
        openDocumentId = shift.openDocumentId,
        closeDocumentId = shift.closeDocumentId
    )

    fun toResponse(report: ReportResult): ReportResponse = ReportResponse(
        documentId = report.documentId,
        deliveryStatus = DeliveryStatus.valueOf(report.deliveryStatus.name),
        deliveryError = report.deliveryError,
        deliveryPayload = report.deliveryPayload
    )
}
