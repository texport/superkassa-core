package io.github.texport.superkassa.delivery.api.port

import io.github.texport.superkassa.delivery.api.model.DeliveryChannel
import io.github.texport.superkassa.delivery.api.model.DeliveryRequest
import io.github.texport.superkassa.delivery.api.model.DeliveryResult

/**
 * Порт для подключения конкретного транспортного канала доставки (Email, SMS и др.).
 */
interface DeliveryPort {
    /**
     * Канал доставки, обслуживаемый данным адаптером.
     */
    val channel: DeliveryChannel

    /**
     * Отправляет фискальный документ через данный канал.
     *
     * @param request Запрос на доставку.
     * @return Результат отправки.
     */
    fun send(request: DeliveryRequest): DeliveryResult
}
