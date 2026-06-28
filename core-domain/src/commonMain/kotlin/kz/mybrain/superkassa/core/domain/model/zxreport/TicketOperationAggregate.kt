package kz.mybrain.superkassa.core.domain.model.zxreport

import kotlinx.serialization.Serializable

/**
 * Агрегированные показатели фискальных операций по типам документов чеков за смену.
 *
 * @property operation Тип фискальной операции (например, продажа, возврат).
 * @property ticketsTotalCount Общее количество оформленных чеков данного типа.
 * @property ticketsCount Количество успешно подтвержденных чеков данного типа.
 * @property ticketsSumBills Сумма по чекам данного типа (в целых тенге).
 * @property payments Список агрегированных сумм оплат по разным типам платежей.
 * @property offlineCount Количество чеков данного типа, оформленных в офлайн-режиме.
 * @property discountSumBills Накопленная сумма предоставленных скидок (в целых тенге).
 * @property markupSumBills Накопленная сумма начисленных наценок (в целых тенге).
 * @property changeSumBills Накопленная сумма выданной сдачи (в целых тенге).
 */
@Serializable
data class TicketOperationAggregate(
    val operation: String,
    val ticketsTotalCount: Long,
    val ticketsCount: Long,
    val ticketsSumBills: Long,
    val payments: List<TicketPaymentAggregate>,
    val offlineCount: Long,
    val discountSumBills: Long,
    val markupSumBills: Long,
    val changeSumBills: Long
)
