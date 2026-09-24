package io.github.texport.superkassa.delivery.api.model

/**
 * Результат отправки фискального документа.
 *
 * @property ok Признак успешности доставки.
 * @property message Сообщение об ошибке или детали статуса при сбое.
 * @property code Постоянный код отказа, например `DELIVERY_SMS_NOT_CONFIGURED`:
 *   по нему приложение и журнал различают причины, не разбирая текст.
 * @property retryable есть ли смысл повторять отказ: у ненастроенного канала
 *   и у чека без получателя повтор даст тот же ответ, и касса его не повторяет.
 */
data class DeliveryResult(
    val ok: Boolean,
    val message: String? = null,
    val code: String? = null,
    val retryable: Boolean = true
)
