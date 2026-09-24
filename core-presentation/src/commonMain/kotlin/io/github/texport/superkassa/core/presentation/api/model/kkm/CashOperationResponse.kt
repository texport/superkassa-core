package io.github.texport.superkassa.core.presentation.api.model.kkm

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kotlinx.serialization.Serializable

/**
 * Результат выполнения операции с наличными.
 */
@Serializable
@Schema(description = "Результат выполнения операции с наличными")
data class CashOperationResponse(
    @Schema(
        description = "Идентификатор сгенерированного фискального документа",
        example = "doc-uuid"
    ) val documentId: String,
    @Schema(
        description = "Статус доставки документа в ОФД",
        example = "ONLINE_OK"
    ) val deliveryStatus: DeliveryStatus = DeliveryStatus.NOT_SENT,
    @Schema(
        description = "Почему БФД не принял документ и что делать — на каждом языке свой текст"
    ) val deliveryError: TrilingualMessageResponse? = null,
    @Schema(
        description = "Код отказа БФД (ResultTypeEnum CPCR); пусто, если БФД не ответил или принял",
        example = "13"
    ) val bfdResultCode: Int? = null
)
