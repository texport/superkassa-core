package io.github.texport.superkassa.core.presentation.api.model.kkm

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Поддерживаемые языки печатных форм документов (чеков).
 */
@Serializable
@Schema(description = "Язык чека")
enum class ReceiptLanguage {
    /** Русский язык */
    @Schema(description = "Русский")
    RU,

    /** Казахский язык */
    @Schema(description = "Казахский")
    KK,

    /** Смешанный (двуязычный) */
    @Schema(description = "Смешанный (Двуязычный)")
    MIXED
}
