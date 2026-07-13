package io.github.texport.superkassa.core.presentation.api.model.ofd

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

@Serializable
@Schema(description = "Результат повторной отправки документов по конкретному каналу доставки")
data class DeliveryRetryItemResponse(
    @Schema(description = "Название канала доставки (например, ofd, email, sms)", example = "ofd")
    val channel: String,
    @Schema(description = "Флаг успешности отправки", example = "true")
    val success: Boolean
)
