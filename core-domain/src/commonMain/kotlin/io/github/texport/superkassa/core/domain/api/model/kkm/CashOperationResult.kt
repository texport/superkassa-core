package io.github.texport.superkassa.core.domain.api.model.kkm

import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryStatus
import io.github.texport.superkassa.core.string.api.TrilingualMessage

/**
 * Результат выполнения операции с наличными (внесение/изъятие).
 *
 * @property documentId Идентификатор сгенерированного фискального документа.
 * @property deliveryStatus Статус доставки документа в ОФД.
 * @property deliveryError Почему БФД не принял документ и что делать — на каждом языке свой текст;
 *   `null`, если отказа не было.
 * @property bfdResultCode Код отказа БФД (ResultTypeEnum CPCR); `null`, если БФД не ответил или принял.
 */
data class CashOperationResult(
    val documentId: String,
    val deliveryStatus: DeliveryStatus = DeliveryStatus.NOT_SENT,
    val deliveryError: TrilingualMessage? = null,
    val bfdResultCode: Int? = null
)
