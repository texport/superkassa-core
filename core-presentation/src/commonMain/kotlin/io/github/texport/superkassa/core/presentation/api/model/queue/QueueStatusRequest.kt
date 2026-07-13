package io.github.texport.superkassa.core.presentation.api.model.queue

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import io.github.texport.superkassa.core.presentation.api.annotations.NotBlank
import kotlinx.serialization.Serializable

/**
 * Модель запроса для получения статуса оффлайн-очереди.
 */
@Serializable
@Schema(description = "Запрос на получение статуса оффлайн-очереди")
data class QueueStatusRequest(
    @Schema(description = "Идентификатор ККМ", example = "kkm-uuid")
    @field:NotBlank
    val kkmId: String
)
