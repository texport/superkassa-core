package io.github.texport.superkassa.core.presentation.api.model.receipt

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Типы печатных фискальных документов ККМ.
 */
@Serializable
@Schema(description = "Тип печатного документа")
enum class PrintDocumentType {
    /** Фискальный чек продажи/возврата/покупки */
    @Schema(description = "Фискальный документ (Чек)")
    DOCUMENT,

    /** Сменный отчет без гашения (X-Отчет) */
    @Schema(description = "X-Отчет")
    X_REPORT,

    /** Документ открытия кассовой смены */
    @Schema(description = "Документ открытия смены")
    OPEN_SHIFT,

    /** Сменный отчет с гашением (Z-Отчет / закрытие смены) */
    @Schema(description = "Z-Отчет")
    CLOSE_SHIFT
}
