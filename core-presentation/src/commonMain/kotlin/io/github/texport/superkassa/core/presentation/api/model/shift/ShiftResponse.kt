package io.github.texport.superkassa.core.presentation.api.model.shift

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Информация о кассовой смене (Ответ).
 */
@Serializable
@Schema(description = "Информация о кассовой смене")
data class ShiftResponse(
    @Schema(description = "ID смены", example = "shift-uuid") val id: String,
    @Schema(description = "ID ККМ", example = "kkm-uuid") val kkmId: String,
    @Schema(description = "Номер смены", example = "10") val shiftNo: Long,
    @Schema(description = "Текущий статус смены (OPEN/CLOSED)", example = "OPEN") val status: ShiftStatus,
    @Schema(description = "Время открытия (epoch ms)", example = "1700000000000") val openedAt: Long,
    @Schema(description = "Время закрытия (epoch ms)", example = "1700000000000") val closedAt: Long? = null,
    @Schema(description = "ID документа открытия", example = "doc-open-uuid") val openDocumentId: String? = null,
    @Schema(description = "ID документа закрытия", example = "doc-close-uuid") val closeDocumentId: String? = null
)
