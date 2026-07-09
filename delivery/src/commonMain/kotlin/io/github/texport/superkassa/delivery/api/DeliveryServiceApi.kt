package io.github.texport.superkassa.delivery.api

import io.github.texport.superkassa.delivery.api.model.DeliveryRequest
import io.github.texport.superkassa.delivery.api.model.DeliveryResult
import io.github.texport.superkassa.delivery.api.port.DeliveryPort
import io.github.texport.superkassa.delivery.impl.DeliveryServiceApiImpl

/**
 * Сервис для отправки фискальных документов (чеков, отчетов) через настроенные каналы доставки.
 */
interface DeliveryServiceApi {
    /**
     * Выполняет доставку фискального документа по каналу, указанному в запросе.
     *
     * @param request Запрос на доставку, содержащий данные и параметры отправки.
     * @return Результат выполнения доставки.
     */
    fun deliver(request: DeliveryRequest): DeliveryResult
}

/**
 * Фабричный метод для создания экземпляра сервиса доставки.
 *
 * @param adapters Список поддерживаемых адаптеров каналов связи.
 * @return Экземпляр сервиса доставки.
 */
fun createDeliveryServiceApi(adapters: List<DeliveryPort>): DeliveryServiceApi =
    DeliveryServiceApiImpl(adapters)
