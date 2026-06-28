package kz.mybrain.superkassa.core.domain.model.settings

import kotlinx.serialization.Serializable

/**
 * Настройки каналов отправки и маршрутизации сообщений.
 *
 * @property channel Имя канала доставки (например, EMAIL, SMS).
 * @property enabled Флаг активности данного канала доставки.
 * @property payloadType Тип передаваемых данных (по умолчанию "DOCUMENT").
 * @property documentFormat Формат документа для отправки (например, "PDF", "HTML").
 * @property destination Адрес назначения (номер телефона, email и т.д.).
 */
@Serializable
data class DeliveryChannelSettings(
    val channel: String,
    val enabled: Boolean = true,
    val payloadType: String = "DOCUMENT",
    val documentFormat: String = "PDF",
    val destination: String? = null
)
