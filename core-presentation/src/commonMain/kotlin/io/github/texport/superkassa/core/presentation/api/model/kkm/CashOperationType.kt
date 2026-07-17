package io.github.texport.superkassa.core.presentation.api.model.kkm

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Тип кассовой операции с наличными деньгами (внесение/изъятие).
 */
@Serializable
@Schema(description = "Тип операции с наличными")
enum class CashOperationType {
    /** Внесение наличных денег */
    @Schema(description = "Внесение наличных")
    CASH_IN,

    /** Изъятие наличных денег */
    @Schema(description = "Изъятие наличных")
    CASH_OUT
}
