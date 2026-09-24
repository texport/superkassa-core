package io.github.texport.superkassa.core.presentation.api.model.delivery

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kotlinx.serialization.Serializable

/**
 * Доставка чека покупателю по одному каналу — строка журнала кассира.
 *
 * Получатель здесь не отдаётся: это персональные данные покупателя,
 * а журнал показывает, куда и почему не дошло, по каналу и причине.
 */
@Serializable
@Schema(description = "Доставка чека покупателю по одному каналу")
data class ReceiptDeliveryResponse(
    @Schema(description = "Канал доставки", example = "SMS")
    val channel: String,
    @Schema(description = "Что отправляется: LINK, PDF, IMAGE, HTML или ESC_POS", example = "LINK")
    val payloadType: String,
    @Schema(description = "Где доставка сейчас")
    val state: ReceiptDeliveryState,
    @Schema(description = "Сколько раз чек уже отправлялся по этому каналу", example = "1")
    val attempts: Int,
    @Schema(
        description = "Не раньше какого времени будет следующая попытка, мс эпохи; только у ожидающей",
        example = "1767225600000"
    )
    val nextAttemptAt: Long?,
    @Schema(description = "Код последнего отказа", example = "DELIVERY_SMS_NOT_CONFIGURED")
    val failureCode: String?,
    @Schema(description = "Причина последнего отказа и что сделать")
    val failureMessage: TrilingualMessageResponse?,
    @Schema(description = "Когда доставка менялась последний раз, мс эпохи", example = "1767225600000")
    val updatedAt: Long
)

/** Где доставка чека по каналу сейчас. */
@Serializable
@Schema(description = "Состояние доставки чека по каналу")
enum class ReceiptDeliveryState {
    /** Ждёт отправки или повтора после отказа: касса дошлёт сама. */
    @Schema(description = "Ждёт отправки")
    PENDING,

    /** Канал принял чек. */
    @Schema(description = "Доставлен")
    DELIVERED,

    /** Не доставлен окончательно: причина — в коде и тексте отказа; повторить можно вручную. */
    @Schema(description = "Не удалось")
    FAILED
}
