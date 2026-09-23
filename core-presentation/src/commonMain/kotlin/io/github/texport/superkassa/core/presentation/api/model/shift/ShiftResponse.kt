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
    @Schema(description = "ID документа закрытия", example = "doc-close-uuid") val closeDocumentId: String? = null,
    @Schema(
        description = "Предел смены в сутки (epoch ms): после него касса отказывает в продаже, возврате, " +
            "внесении и изъятии, пока смену не закроют. Сутки от первого платёжного документа смены; " +
            "пусто — платёжных документов ещё нет",
        example = "1700086400000"
    ) val dayLimitAt: Long? = null,
    @Schema(
        description = "Предел пройден по часам кассы: операции получат отказ SHIFT_LONGER_THAN_DAY",
        example = "false"
    ) val dayLimitExceeded: Boolean = false
)
