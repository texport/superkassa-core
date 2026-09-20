package io.github.texport.superkassa.core.domain.api.model.zxreport

/**
 * Агрегированные показатели фискальных операций по типам документов чеков за смену.
 *
 * @property operation Тип фискальной операции (например, продажа, возврат).
 * @property ticketsTotalCount Общее количество оформленных чеков данного типа.
 * @property ticketsCount Количество успешно подтвержденных чеков данного типа.
 * @property ticketsSumTiyn Сумма по чекам данного типа (в целых тенге).
 * @property payments Список агрегированных сумм оплат по разным типам платежей.
 * @property offlineCount Количество чеков данного типа, оформленных в офлайн-режиме.
 * @property discountSumTiyn Накопленная сумма предоставленных скидок (в целых тенге).
 * @property markupSumTiyn Накопленная сумма начисленных наценок (в целых тенге).
 * @property changeSumTiyn Накопленная сумма выданной сдачи (в целых тенге).
 */
data class TicketOperationAggregate(
    val operation: String,
    val ticketsTotalCount: Long,
    val ticketsCount: Long,
    val ticketsSumTiyn: Long,
    val payments: List<TicketPaymentAggregate>,
    val offlineCount: Long,
    val discountSumTiyn: Long,
    val markupSumTiyn: Long,
    val changeSumTiyn: Long
)
