package io.github.texport.superkassa.core.presentation.api.model.shift

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kotlinx.serialization.Serializable

/**
 * Результат выполнения операции формирования X/Z отчетов.
 */
@Serializable
@Schema(description = "Результат выполнения операции формирования отчета")
data class ReportResponse(
    @Schema(description = "Идентификатор сгенерированного фискального документа", example = "doc-uuid") val documentId:
    String,
    @Schema(description = "Статус доставки отчета в ОФД/клиенту", example = "ONLINE_OK") val deliveryStatus:
    DeliveryStatus = DeliveryStatus.NOT_SENT,
    @Schema(
        description = "Почему БФД не принял отчёт и что делать — на каждом языке свой текст"
    ) val deliveryError: TrilingualMessageResponse? = null,
    @Schema(description = "Бинарное представление сгенерированного отчета", hidden = true) val deliveryPayload:
    ByteArray? = null,
    @Schema(
        description = "Код отказа БФД (ResultTypeEnum CPCR); пусто, если БФД не ответил или принял",
        example = "13"
    ) val bfdResultCode: Int? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ReportResponse) return false

        if (documentId != other.documentId) return false
        if (deliveryStatus != other.deliveryStatus) return false
        if (deliveryError != other.deliveryError) return false
        if (bfdResultCode != other.bfdResultCode) return false
        if (deliveryPayload != null) {
            if (other.deliveryPayload == null) return false
            if (!deliveryPayload.contentEquals(other.deliveryPayload)) return false
        } else if (other.deliveryPayload != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = documentId.hashCode()
        result = 31 * result + deliveryStatus.hashCode()
        result = 31 * result + (deliveryError?.hashCode() ?: 0)
        result = 31 * result + (bfdResultCode ?: 0)
        result = 31 * result + (deliveryPayload?.contentHashCode() ?: 0)
        return result
    }
}
