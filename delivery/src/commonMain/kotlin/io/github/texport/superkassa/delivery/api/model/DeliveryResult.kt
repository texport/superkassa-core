package io.github.texport.superkassa.delivery.api.model

/**
 * Результат отправки фискального документа.
 *
 * @property ok Признак успешности доставки.
 * @property message Сообщение об ошибке или детали статуса при сбое.
 */
data class DeliveryResult(
    val ok: Boolean,
    val message: String? = null
)
