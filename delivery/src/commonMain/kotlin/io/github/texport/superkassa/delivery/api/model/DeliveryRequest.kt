package io.github.texport.superkassa.delivery.api.model

/**
 * Параметры запроса на доставку фискального документа.
 *
 * @property cashboxId Идентификатор кассы.
 * @property documentId Идентификатор фискального документа.
 * @property channel Выбранный канал доставки.
 * @property destination Адрес назначения (email, номер телефона и др.).
 * @property payloadUrl URL-адрес для загрузки содержимого документа (опционально).
 * @property payloadBytes Двоичные данные содержимого документа (опционально).
 */
data class DeliveryRequest(
    val cashboxId: String,
    val documentId: String,
    val channel: DeliveryChannel,
    val destination: String? = null,
    val payloadUrl: String? = null,
    val payloadBytes: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as DeliveryRequest

        if (cashboxId != other.cashboxId) return false
        if (documentId != other.documentId) return false
        if (channel != other.channel) return false
        if (destination != other.destination) return false
        if (payloadUrl != other.payloadUrl) return false
        if (payloadBytes != null) {
            if (other.payloadBytes == null) return false
            if (!payloadBytes.contentEquals(other.payloadBytes)) return false
        } else if (other.payloadBytes != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = cashboxId.hashCode()
        result = 31 * result + documentId.hashCode()
        result = 31 * result + channel.hashCode()
        result = 31 * result + (destination?.hashCode() ?: 0)
        result = 31 * result + (payloadUrl?.hashCode() ?: 0)
        result = 31 * result + (payloadBytes?.contentHashCode() ?: 0)
        return result
    }
}
