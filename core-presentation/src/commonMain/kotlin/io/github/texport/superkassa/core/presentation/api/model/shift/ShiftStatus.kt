package io.github.texport.superkassa.core.presentation.api.model.shift

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Статусы кассовой смены.
 */
@Serializable
@Schema(description = "Статус смены")
enum class ShiftStatus {
    /** Кассовая смена открыта */
    @Schema(description = "Открыта")
    OPEN,

    /** Кассовая смена закрыта */
    @Schema(description = "Закрыта")
    CLOSED
}
