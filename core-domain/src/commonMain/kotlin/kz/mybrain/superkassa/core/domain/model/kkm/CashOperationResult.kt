package kz.mybrain.superkassa.core.domain.model.kkm

import kz.mybrain.superkassa.core.domain.model.delivery.DeliveryStatus

/**
 * Результат выполнения операции с наличными (внесение/изъятие).
 *
 * @property documentId Идентификатор сгенерированного фискального документа.
 * @property deliveryStatus Статус доставки документа в ОФД.
 * @property deliveryError Текст ошибки доставки, если отправка в ОФД не удалась.
 */
data class CashOperationResult(
    val documentId: String,
    val deliveryStatus: DeliveryStatus = DeliveryStatus.NOT_SENT,
    val deliveryError: String? = null
)
