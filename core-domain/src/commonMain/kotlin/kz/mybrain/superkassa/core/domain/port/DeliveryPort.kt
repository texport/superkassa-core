package kz.mybrain.superkassa.core.domain.port

import kz.mybrain.superkassa.core.domain.model.delivery.DeliveryRequest

/**
 * Порт отправки и доставки фискальных документов (чеков, отчётов) клиентам.
 * Отвечает за коммуникацию по внешним каналам связи (SMS, Email и др.).
 */
interface DeliveryPort {

    /**
     * Выполняет доставку фискального документа по указанному адресу/номеру телефона.
     *
     * @param request параметры запроса на доставку [DeliveryRequest], содержащие контактные данные и контент.
     * @return `true`, если доставка инициирована успешно; `false` в противном случае.
     */
    fun deliver(request: DeliveryRequest): Boolean
}
