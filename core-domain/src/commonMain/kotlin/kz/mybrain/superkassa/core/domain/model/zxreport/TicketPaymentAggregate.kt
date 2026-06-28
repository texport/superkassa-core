package kz.mybrain.superkassa.core.domain.model.zxreport

import kotlinx.serialization.Serializable

/**
 * Агрегированные суммы платежей по типам оплат за смену.
 *
 * @property payment Тип оплаты (например, CASH, CARD).
 * @property sumBills Накопленная сумма оплат данным типом платежа (в целых тенге).
 * @property count Количество платежей данным типом оплаты.
 */
@Serializable
data class TicketPaymentAggregate(
    val payment: String,
    val sumBills: Long,
    val count: Long
)
