package io.github.texport.superkassa.core.presentation.api.model.shift

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Запрос на закрытие смены (Z-отчет).
 */
@Serializable
data class CloseShiftRequest(
    @kotlinx.serialization.Transient
    @Schema(description = "Не используется. Передано для обратной совместимости.", example = "deprecated")
    private val _unused: String? = null
)
