package io.github.texport.superkassa.core.presentation.api.model.kkm

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus
import kotlinx.serialization.Serializable

/**
 * Результат выполнения операции с наличными.
 */
@Serializable
@Schema(description = "Результат выполнения операции с наличными")
data class CashOperationResponse(
    @Schema(description = "Идентификатор сгенерированного фискального документа", example = "doc-uuid") val documentId: String,
    @Schema(description = "Статус доставки документа в ОФД", example = "ONLINE_OK") val deliveryStatus: DeliveryStatus = DeliveryStatus.NOT_SENT,
    @Schema(description = "Текст ошибки доставки, если отправка в ОФД не удалась", example = "Network failure") val deliveryError: String? = null
)
