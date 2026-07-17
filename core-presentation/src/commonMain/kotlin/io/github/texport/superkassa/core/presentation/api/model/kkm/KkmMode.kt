package io.github.texport.superkassa.core.presentation.api.model.kkm

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Режим работы ККМ (онлайн или автономный).
 */
@Serializable
@Schema(description = "Режим работы ККМ")
enum class KkmMode {
    /** Онлайн-режим с прямой отправкой в ОФД */
    @Schema(description = "Онлайн")
    ONLINE,

    /** Автономный (офлайн) режим работы */
    @Schema(description = "Автономный (Офлайн)")
    OFFLINE
}
