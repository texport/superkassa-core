package io.github.texport.superkassa.core.presentation.api.model.receipt

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Типы фискальных операций чеков.
 */
@Serializable
@Schema(description = "Тип фискальной операции чека")
enum class ReceiptOperationType {
    /** Продажа */
    @Schema(description = "Продажа")
    SELL,

    /** Возврат продажи */
    @Schema(description = "Возврат продажи")
    SELL_RETURN,

    /** Покупка */
    @Schema(description = "Покупка")
    BUY,

    /** Возврат покупки */
    @Schema(description = "Возврат покупки")
    BUY_RETURN
}
