package io.github.texport.superkassa.core.presentation.api.model.queue

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Модель элемента очереди для отображения в интерфейсе.
 */
@Serializable
@Schema(description = "Элемент офлайн-очереди фискализации")
data class QueueItemResponse(
    @Schema(description = "Уникальный ID задачи в очереди", example = "task-uuid-abc-123")
    val id: String,
    @Schema(description = "Идентификатор полосы/очереди ККМ", example = "lane-kkm-1")
    val lane: String,
    @Schema(description = "Тип фискальной операции (SELL, BUY и др.)", example = "SELL")
    val type: String,
    @Schema(description = "Статус обработки задачи (PENDING, PROCESSING, SUCCESS, FAILED)", example = "PENDING")
    val status: String,
    @Schema(description = "Порядковый номер попытки отправки", example = "2")
    val attempt: Int,
    @Schema(description = "Время следующей попытки отправки (Unix timestamp в миллисекундах)", example = "1718000000000")
    val nextAttemptAt: Long?,
    @Schema(description = "Текст последней технической ошибки", example = "Timeout error")
    val lastError: String?,
    @Schema(description = "Текст ошибки на русском языке", example = "Превышено время ожидания ответа")
    val errorRu: String?,
    @Schema(description = "Текст ошибки на казахском языке", example = "Күту уақыты асып кетті")
    val errorKk: String?,
    @Schema(description = "Текст ошибки на английском языке", example = "Response timeout exceeded")
    val errorEn: String?
)
