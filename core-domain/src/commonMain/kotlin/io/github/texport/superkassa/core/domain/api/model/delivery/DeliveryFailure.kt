package io.github.texport.superkassa.core.domain.api.model.delivery

import io.github.texport.superkassa.core.string.api.TrilingualMessage

/**
 * Почему доставка не удалась — то, что видят кассир и владелец.
 *
 * @property code постоянный код отказа, например `DELIVERY_SMS_NOT_CONFIGURED`:
 *   по нему приложение различает причины, не разбирая текст.
 * @property message причина и что сделать, на трёх языках.
 */
data class DeliveryFailure(
    val code: String,
    val message: TrilingualMessage
)

/**
 * Итог одной отправки по каналу.
 *
 * @property delivered канал принял чек.
 * @property failure причина отказа; у принятого — `null`.
 * @property retryable есть ли смысл повторять: у ненастроенного канала
 *   и у чека без получателя повтор даст тот же отказ.
 */
data class DeliveryOutcome(
    val delivered: Boolean,
    val failure: DeliveryFailure? = null,
    val retryable: Boolean = true
) {
    companion object {
        /** Канал принял чек. */
        val DELIVERED: DeliveryOutcome = DeliveryOutcome(delivered = true)

        /** Отказ с причиной [failure]; [retryable] — стоит ли повторять. */
        fun failed(failure: DeliveryFailure, retryable: Boolean): DeliveryOutcome =
            DeliveryOutcome(delivered = false, failure = failure, retryable = retryable)
    }
}
