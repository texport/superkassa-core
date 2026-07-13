package io.github.texport.superkassa.core.presentation.api.model.kkm

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Слепок значений счетчиков ККМ.
 */
@Serializable
@Schema(description = "Слепок значений счетчиков ККМ")
data class CounterSnapshotResponse(
    @Schema(description = "Область видимости счетчика (GLOBAL или SHIFT)", example = "GLOBAL") val scope: String,
    @Schema(description = "Идентификатор смены (null для глобальных счетчиков)", example = "shift-uuid") val shiftId: String? = null,
    @Schema(description = "Уникальный ключ счетчика", example = "SELL_CASH") val key: String,
    @Schema(description = "Числовое значение счетчика", example = "125000") val value: Long,
    @Schema(description = "Время последнего обновления (epoch ms)", example = "1700000000000") val updatedAt: Long
)
