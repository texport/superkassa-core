package io.github.texport.superkassa.core.domain.api.model.zxreport

/**
 * Агрегированные суммы платежей по типам оплат за смену.
 *
 * @property payment Тип оплаты (например, CASH, CARD).
 * @property sumBills Накопленная сумма оплат данным типом платежа (в целых тенге).
 * @property count Количество платежей данным типом оплаты.
 */
data class TicketPaymentAggregate(
    val payment: String,
    val sumBills: Long,
    val count: Long
)
