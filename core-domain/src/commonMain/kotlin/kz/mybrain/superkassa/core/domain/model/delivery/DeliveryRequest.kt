package kz.mybrain.superkassa.core.domain.model.delivery

/**
 * Запрос на доставку чека клиенту (по email, sms, мессенджерам или на печать).
 *
 * @property kkmId Идентификатор кассы (ККМ), отправившей запрос.
 * @property documentId Идентификатор связанного фискального документа.
 * @property channel Канал отправки (например, EMAIL, SMS, PRINT).
 * @property destination Адрес получателя (почта, номер телефона и т.д.).
 * @property payloadType Тип передаваемых данных (например, LINK — URL-адрес ОФД, PDF/IMAGE/HTML — документ, ESC_POS — для печати).
 * @property payloadUrl URL-ссылка на чек (используется при payloadType=LINK).
 * @property payloadBytes Документ в бинарном представлении (например, PDF/ESC_POS).
 */
data class DeliveryRequest(
    val kkmId: String,
    val documentId: String,
    val channel: String,
    val destination: String? = null,
    val payloadType: String,
    val payloadUrl: String? = null,
    val payloadBytes: ByteArray? = null
) {
    init {
        require(payloadUrl != null || payloadBytes != null) {
            "Either payloadUrl or payloadBytes must be set"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other !is DeliveryRequest) return false

        if (kkmId != other.kkmId) return false
        if (documentId != other.documentId) return false
        if (channel != other.channel) return false
        if (destination != other.destination) return false
        if (payloadType != other.payloadType) return false
        if (payloadUrl != other.payloadUrl) return false
        if (payloadBytes != null) {
            if (other.payloadBytes == null) return false
            if (!payloadBytes.contentEquals(other.payloadBytes)) return false
        } else if (other.payloadBytes != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = kkmId.hashCode()
        result = 31 * result + documentId.hashCode()
        result = 31 * result + channel.hashCode()
        result = 31 * result + (destination?.hashCode() ?: 0)
        result = 31 * result + payloadType.hashCode()
        result = 31 * result + (payloadUrl?.hashCode() ?: 0)
        result = 31 * result + (payloadBytes?.contentHashCode() ?: 0)
        return result
    }
}
