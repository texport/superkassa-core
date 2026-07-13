package io.github.texport.superkassa.core.presentation.api.model.queue

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Модель статуса оффлайн-очереди.
 */
@Serializable
@Schema(description = "Модель статуса оффлайн-очереди")
data class QueueStatusResponse(
    @Schema(description = "Наличие неотправленных задач", example = "true")
    val hasPendingItems: Boolean,
    @Schema(description = "Количество неотправленных задач", example = "2")
    val pendingCount: Int
)
