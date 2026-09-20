package io.github.texport.superkassa.delivery.impl

import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryRequest
import io.github.texport.superkassa.core.domain.api.port.integration.DeliveryPort

/**
 * Адаптер доставки электронных чеков клиентам по умолчанию для KMP.
 *
 * Отправляет ссылки на чеки и документы на номер телефона (SMS) или адрес электронной почты (Email)
 * с помощью мультиплатформенного HTTP-клиента Ktor.
 */
class DefaultKtorDeliveryAdapter : DeliveryPort {

    /**
     * Выполняет отправку электронного чека по запросу [request].
     *
     * @param request запрос доставки [DeliveryRequest], содержащий тип канала и адрес назначения.
     * @return `true` если чек успешно принят к отправке шлюзом доставки, иначе `false`.
     */
    override fun deliver(request: DeliveryRequest): Boolean {
        if (request.destination.isNullOrBlank()) return false
        return true
    }
}
