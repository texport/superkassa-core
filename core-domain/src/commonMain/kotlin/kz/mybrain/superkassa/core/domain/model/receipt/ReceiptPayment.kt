package kz.mybrain.superkassa.core.domain.model.receipt

import kz.mybrain.superkassa.core.domain.model.common.Money

/**
 * Информация об оплате по чеку.
 *
 * @property type Способ оплаты (например, наличные или карта).
 * @property sum Сумма, внесенная данным способом оплаты.
 */
data class ReceiptPayment(
    val type: PaymentType,
    val sum: Money
)
