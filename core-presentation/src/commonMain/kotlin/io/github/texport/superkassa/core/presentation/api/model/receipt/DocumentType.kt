package io.github.texport.superkassa.core.presentation.api.model.receipt

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Типы кассовых документов.
 */
@Serializable
@Schema(description = "Тип кассового документа")
enum class DocumentType {
    /** Открытие смены */
    @Schema(description = "Открытие смены")
    SHIFT_OPEN,

    /** Закрытие смены */
    @Schema(description = "Закрытие смены")
    SHIFT_CLOSE,

    /** Продажа (фискальный чек) */
    @Schema(description = "Продажа")
    SALE,

    /** Возврат продажи */
    @Schema(description = "Возврат продажи")
    RETURN,

    /** Покупка */
    @Schema(description = "Покупка")
    BUY,

    /** Возврат покупки */
    @Schema(description = "Возврат покупки")
    BUY_RETURN,

    /** Внесение наличных денег */
    @Schema(description = "Внесение наличных")
    CASH_IN,

    /** Изъятие наличных денег */
    @Schema(description = "Изъятие наличных")
    CASH_OUT,

    /** X-отчет */
    @Schema(description = "X-Отчет")
    X_REPORT
}
