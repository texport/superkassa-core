package io.github.texport.superkassa.core.domain.api.port.integration

import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryFailure
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryOutcome
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryRequest
import io.github.texport.superkassa.core.string.api.CoreStrings

/**
 * Порт отправки и доставки фискальных документов (чеков, отчётов) клиентам.
 * Отвечает за коммуникацию по внешним каналам связи (SMS, Email и др.).
 *
 * Вызовы блокирующие: ждут ответа канала. Ядро зовёт их из фона досылки
 * задач доставки, а не из потока пробития чека.
 */
interface DeliveryPort {

    /**
     * Выполняет доставку фискального документа по указанному адресу/номеру телефона.
     *
     * @param request параметры запроса на доставку [DeliveryRequest], содержащие контактные данные и контент.
     * @return `true`, если доставка инициирована успешно; `false` в противном случае.
     */
    fun deliver(request: DeliveryRequest): Boolean

    /**
     * Доставляет документ и называет причину отказа.
     *
     * По умолчанию — [deliver] без причины: такой отказ получает код
     * [UNEXPLAINED_FAILURE] и повторяется. Порт, которому причина известна,
     * переопределяет этот метод — её увидят кассир и владелец.
     *
     * @param request что и куда доставить.
     * @return итог отправки с кодом и текстом отказа.
     */
    fun send(request: DeliveryRequest): DeliveryOutcome =
        if (deliver(request)) {
            DeliveryOutcome.DELIVERED
        } else {
            val message = CoreStrings.deliveryFailedWithoutReason(request.channel)
            DeliveryOutcome.failed(DeliveryFailure(UNEXPLAINED_FAILURE, message), retryable = true)
        }

    companion object {
        /** Канал отказал, не назвав причины. */
        const val UNEXPLAINED_FAILURE: String = "DELIVERY_FAILED"
    }
}
