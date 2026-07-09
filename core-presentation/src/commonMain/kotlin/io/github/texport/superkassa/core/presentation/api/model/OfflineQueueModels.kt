package io.github.texport.superkassa.core.presentation.api.model

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import io.github.texport.superkassa.core.presentation.api.annotations.NotBlank
import kotlinx.serialization.Serializable

/**
 * Модель статуса оффлайн-очереди.
 *
 * @property hasPendingItems true, если в очереди есть неотправленные задачи.
 * @property pendingCount Общее количество неотправленных задач.
 */
@Serializable
@Schema(description = "Модель статуса оффлайн-очереди")
data class QueueStatusResponse(
    @Schema(description = "Наличие неотправленных задач", example = "true")
    val hasPendingItems: Boolean,
    @Schema(description = "Количество неотправленных задач", example = "2")
    val pendingCount: Int
)

/**
 * Модель запроса для получения статуса оффлайн-очереди.
 *
 * @property kkmId Уникальный идентификатор ККМ.
 */
@Serializable
@Schema(description = "Запрос на получение статуса оффлайн-очереди")
data class QueueStatusRequest(
    @Schema(description = "Идентификатор ККМ", example = "kkm-uuid")
    @field:NotBlank
    val kkmId: String
)
