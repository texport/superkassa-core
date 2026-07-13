package io.github.texport.superkassa.core.presentation.api.model.kkm

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Запрос на обновление настроек ККМ.
 */
@Serializable
@Schema(description = "Запрос на обновление настроек ККМ")
data class KkmSettingsUpdateRequest(
    @Schema(description = "Флаг автоматического закрытия смены (при истечении 24 часов)", example = "true")
    val autoCloseShift: Boolean? = null
)
