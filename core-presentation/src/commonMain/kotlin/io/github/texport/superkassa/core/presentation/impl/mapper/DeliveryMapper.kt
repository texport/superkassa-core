package io.github.texport.superkassa.core.presentation.impl.mapper

import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryTask
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryTaskStatus
import io.github.texport.superkassa.core.presentation.api.model.delivery.ReceiptDeliveryResponse
import io.github.texport.superkassa.core.presentation.api.model.delivery.ReceiptDeliveryState
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse

/** Задача доставки — строкой журнала, без получателя. */
internal fun DeliveryTask.toResponse(): ReceiptDeliveryResponse = ReceiptDeliveryResponse(
    channel = channel,
    payloadType = payloadType,
    state = when (status) {
        DeliveryTaskStatus.PENDING -> ReceiptDeliveryState.PENDING
        DeliveryTaskStatus.DELIVERED -> ReceiptDeliveryState.DELIVERED
        DeliveryTaskStatus.FAILED -> ReceiptDeliveryState.FAILED
    },
    attempts = attempts,
    nextAttemptAt = nextAttemptAt.takeIf { status == DeliveryTaskStatus.PENDING },
    failureCode = failure?.code,
    failureMessage = failure?.message?.let(TrilingualMessageResponse::from),
    updatedAt = updatedAt
)
