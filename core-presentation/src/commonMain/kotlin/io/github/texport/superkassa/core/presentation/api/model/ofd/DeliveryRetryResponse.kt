package io.github.texport.superkassa.core.presentation.api.model.ofd

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

@Serializable
@Schema(description = "Результаты повторной отправки документов ОФД по различным каналам")
data class DeliveryRetryResponse(
    @Schema(description = "Список результатов повторной отправки по каналам")
    val results: List<DeliveryRetryItemResponse>
)
