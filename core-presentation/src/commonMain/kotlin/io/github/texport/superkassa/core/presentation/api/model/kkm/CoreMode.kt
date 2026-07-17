package io.github.texport.superkassa.core.presentation.api.model.kkm

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Режим работы ядра (локальный/серверный).
 */
@Serializable
@Schema(description = "Режим работы ядра")
enum class CoreMode {
    /** Локальный (десктопный) режим работы */
    @Schema(description = "Локальный (Десктоп)")
    DESKTOP,

    /** Серверный режим работы (кластер) */
    @Schema(description = "Серверный (Кластер)")
    SERVER
}
