package kz.mybrain.superkassa.core.domain.model.receipt

import kotlinx.serialization.Serializable
import kz.mybrain.superkassa.core.domain.model.delivery.DeliveryStatus

/**
 * Результат успешной обработки и регистрации чека.
 *
 * @property documentId Уникальный идентификатор фискального документа чека в БД.
 * @property fiscalSign Фискальный признак (подпись) чека, полученный от ОФД (null при офлайн-оформлении).
 * @property autonomousSign Автономный фискальный признак чека (при офлайн-оформлении).
 * @property deliveryPayload Сгенерированная печатная форма чека (например, в формате ESC_POS/PDF).
 * @property deliveryStatus Текущий статус отправки чека в ОФД/клиенту.
 * @property deliveryError Текст возникшей ошибки при попытке отправки/печати чека.
 */
@Serializable
data class ReceiptResult(
    val documentId: String,
    val fiscalSign: String? = null,
    val autonomousSign: String? = null,
    val deliveryPayload: ByteArray? = null,
    val deliveryStatus: DeliveryStatus = DeliveryStatus.NOT_SENT,
    val deliveryError: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ReceiptResult) return false

        if (documentId != other.documentId) return false
        if (fiscalSign != other.fiscalSign) return false
        if (autonomousSign != other.autonomousSign) return false
        if (deliveryPayload != null) {
            if (other.deliveryPayload == null) return false
            if (!deliveryPayload.contentEquals(other.deliveryPayload)) return false
        } else if (other.deliveryPayload != null) return false
        if (deliveryStatus != other.deliveryStatus) return false
        if (deliveryError != other.deliveryError) return false

        return true
    }

    override fun hashCode(): Int {
        var result = documentId.hashCode()
        result = 31 * result + (fiscalSign?.hashCode() ?: 0)
        result = 31 * result + (autonomousSign?.hashCode() ?: 0)
        result = 31 * result + (deliveryPayload?.contentHashCode() ?: 0)
        result = 31 * result + deliveryStatus.hashCode()
        result = 31 * result + (deliveryError?.hashCode() ?: 0)
        return result
    }
}
