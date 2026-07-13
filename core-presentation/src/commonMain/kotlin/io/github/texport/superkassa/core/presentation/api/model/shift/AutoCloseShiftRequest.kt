package io.github.texport.superkassa.core.presentation.api.model.shift

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Запрос на автозакрытие смены.
 */
@Serializable
@Schema(description = "Запрос на обновление настройки автозакрытия смены")
data class AutoCloseShiftRequest(
    @Schema(description = "Включить/выключить автоматическое закрытие смены", example = "true")
    val autoCloseShift: Boolean
)
