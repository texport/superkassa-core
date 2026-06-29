package kz.mybrain.superkassa.core.domain.helper.zxreport

import kz.mybrain.superkassa.core.domain.model.zxreport.TicketOperationAggregate
import kz.mybrain.superkassa.core.domain.model.zxreport.TicketPaymentAggregate
import kz.mybrain.superkassa.core.domain.model.common.CounterKeyFormats
import kz.mybrain.superkassa.core.domain.model.common.format

/**
 * Объект-построитель блока операций по чекам (ticketOperations) для Zx-отчета.
 *
 * Рассчитывает статистику по каждому виду кассовых операций (продажа, возврат продажи и т.д.):
 * - Общее количество чеков;
 * - Количество успешно отправленных чеков;
 * - Общую сумму по чекам;
 * - Количество офлайн-чеков;
 * - Суммы скидок, наценок и сдачи;
 * - Распределение по видам платежей (наличные, карты, мобильные и др.).
 */
object ZxTicketOperationsBlockBuilder {

    /**
     * Формирует список агрегированных данных по операциям с чеками.
     *
     * @param counters Карта счетчиков смены.
     * @return Список объектов [TicketOperationAggregate] для каждой кассовой операции.
     */
    fun resolveTicketOperations(counters: Map<String, Long>): List<TicketOperationAggregate> {
        val result = mutableListOf<TicketOperationAggregate>()
        for (op in operationsList()) {
            val totalCount = counters[CounterKeyFormats.TICKET_TOTAL_COUNT.format(op)] ?: 0L
            val count = counters[CounterKeyFormats.TICKET_COUNT.format(op)] ?: 0L
            val sum = counters[CounterKeyFormats.TICKET_SUM.format(op)] ?: 0L
            val offlineCount = counters[CounterKeyFormats.TICKET_OFFLINE_COUNT.format(op)] ?: 0L
            val discountSum = counters[CounterKeyFormats.TICKET_DISCOUNT_SUM.format(op)] ?: 0L
            val markupSum = counters[CounterKeyFormats.TICKET_MARKUP_SUM.format(op)] ?: 0L
            val changeSum = counters[CounterKeyFormats.TICKET_CHANGE_SUM.format(op)] ?: 0L

            val payments = paymentTypes().map { payKey ->
                val sumKey = CounterKeyFormats.PAYMENT_SUM.format(op, payKey)
                val countKey = CounterKeyFormats.PAYMENT_COUNT.format(op, payKey)
                val paySum = counters[sumKey] ?: 0L
                val payCount = counters[countKey] ?: 0L
                TicketPaymentAggregate(payment = payKey, sumBills = paySum, count = payCount)
            }

            result += TicketOperationAggregate(
                operation = op,
                ticketsTotalCount = totalCount,
                ticketsCount = count,
                ticketsSumBills = sum,
                payments = payments,
                offlineCount = offlineCount,
                discountSumBills = discountSum,
                markupSumBills = markupSum,
                changeSumBills = changeSum
            )
        }
        return result
    }

    /**
     * Возвращает список кодов кассовых операций для агрегации чеков.
     */
    private fun operationsList(): List<String> =
        listOf("OPERATION_SELL", "OPERATION_SELL_RETURN", "OPERATION_BUY", "OPERATION_BUY_RETURN")

    /**
     * Возвращает список поддерживаемых типов оплаты.
     */
    private fun paymentTypes(): List<String> =
        listOf("PAYMENT_CASH", "PAYMENT_CARD", "PAYMENT_CREDIT", "PAYMENT_TARE", "PAYMENT_MOBILE", "PAYMENT_ELECTRONIC")
}
