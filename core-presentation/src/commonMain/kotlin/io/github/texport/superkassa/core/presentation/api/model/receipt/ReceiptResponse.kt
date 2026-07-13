package io.github.texport.superkassa.core.presentation.api.model.receipt

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus
import kotlinx.serialization.Serializable

/**
 * Результат успешной обработки и регистрации чека (Ответ).
 */
@Serializable
@Schema(description = "Результат успешной регистрации чека")
data class ReceiptResponse(
    @Schema(description = "Уникальный идентификатор фискального документа чека в БД", example = "doc-uuid") val documentId: String,
    @Schema(description = "Фискальный признак (подпись) чека от ОФД", example = "829302") val fiscalSign: String? = null,
    @Schema(description = "Автономный фискальный признак чека (при офлайн-оформлении)", example = "920183") val autonomousSign: String? = null,
    @Schema(description = "Сгенерированная печатная форма чека (бинарный payload)", hidden = true) val deliveryPayload: ByteArray? = null,
    @Schema(description = "Текущий статус отправки чека в ОФД/клиенту", example = "ONLINE_OK") val deliveryStatus: DeliveryStatus = DeliveryStatus.NOT_SENT,
    @Schema(description = "Текст возникшей ошибки при попытке отправки/печати чека", example = "Timeout") val deliveryError: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is ReceiptResponse) return false

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
