package io.github.texport.superkassa.core.domain.api.model.report

import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryStatus
import io.github.texport.superkassa.core.string.api.TrilingualMessage

/**
 * Результат выполнения операции формирования X/Z отчетов.
 *
 * @property documentId Идентификатор сгенерированного фискального документа.
 * @property deliveryStatus Статус доставки отчета в ОФД/клиенту.
 * @property deliveryError Почему БФД не принял отчёт и что делать — на каждом языке свой текст;
 *   `null`, если отказа не было.
 * @property deliveryPayload Бинарное представление сгенерированного отчета (например, PDF/ESC_POS).
 * @property bfdResultCode Код отказа БФД (ResultTypeEnum CPCR); `null`, если БФД не ответил или принял.
 */
data class ReportResult(
    val documentId: String,
    val deliveryStatus: DeliveryStatus = DeliveryStatus.NOT_SENT,
    val deliveryError: TrilingualMessage? = null,
    val deliveryPayload: ByteArray? = null,
    val bfdResultCode: Int? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ReportResult) return false

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
