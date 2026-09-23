package io.github.texport.superkassa.delivery.impl

import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryRequest
import io.github.texport.superkassa.core.domain.api.port.integration.DeliveryPort

/**
 * Доставка по умолчанию там, где каналов доставки не настроено.
 *
 * Ничего не отправляет и поэтому всегда отвечает отказом. Прежде она
 * отвечала успехом на любой запрос с получателем: чек записывался
 * доставленным покупателю, которому не уходило ничего.
 */
class DefaultKtorDeliveryAdapter : DeliveryPort {

    /**
     * Отказ: отправлять нечем.
     *
     * @param request запрос доставки.
     * @return всегда `false`.
     */
    override fun deliver(request: DeliveryRequest): Boolean = false
}
