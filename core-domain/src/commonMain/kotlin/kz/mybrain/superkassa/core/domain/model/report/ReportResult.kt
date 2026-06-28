package kz.mybrain.superkassa.core.domain.model.report

import kotlinx.serialization.Serializable
import kz.mybrain.superkassa.core.domain.model.delivery.DeliveryStatus

/**
 * Результат выполнения операции формирования X/Z отчетов.
 *
 * @property documentId Идентификатор сгенерированного фискального документа.
 * @property deliveryStatus Статус доставки отчета в ОФД/клиенту.
 * @property deliveryError Текст ошибки доставки, если отправка завершилась неудачно.
 * @property deliveryPayload Бинарное представление сгенерированного отчета (например, PDF/ESC_POS).
 */
@Serializable
data class ReportResult(
    val documentId: String,
    val deliveryStatus: DeliveryStatus = DeliveryStatus.NOT_SENT,
    val deliveryError: String? = null,
    val deliveryPayload: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ReportResult) return false

        if (documentId != other.documentId) return false
        if (deliveryStatus != other.deliveryStatus) return false
        if (deliveryError != other.deliveryError) return false
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
        result = 31 * result + (deliveryPayload?.contentHashCode() ?: 0)
        return result
    }
}
