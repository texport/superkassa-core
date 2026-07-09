package io.github.texport.superkassa.delivery.api.model

/**
 * Поддерживаемые каналы доставки фискальных документов.
 */
enum class DeliveryChannel {
    PRINT,
    SMS,
    EMAIL,
    TELEGRAM,
    WHATSAPP
}
