package kz.mybrain.superkassa.core.domain.helper.zxreport

import kz.mybrain.superkassa.core.domain.model.zxreport.MoneyPlacementAggregate
import kz.mybrain.superkassa.core.domain.model.common.CounterKeyFormats
import kz.mybrain.superkassa.core.domain.model.common.format

/**
 * Объект-построитель блока `moneyPlacements` для Zx-отчета (Z-отчет / X-отчет).
 *
 * Агрегирует данные по операциям внесения (Deposit) и изъятия (Withdrawal) наличных денег
 * на основе соответствующих счетчиков за смену.
 */
object ZxMoneyPlacementsBlockBuilder {

    /**
     * Список поддерживаемых операций размещения денег:
     * - `MONEY_PLACEMENT_DEPOSIT` — внесение денег в кассу;
     * - `MONEY_PLACEMENT_WITHDRAWAL` — изъятие денег из кассы.
     */
    private val operations = listOf(
        "MONEY_PLACEMENT_DEPOSIT",
        "MONEY_PLACEMENT_WITHDRAWAL"
    )

    /**
     * Формирует список агрегированных данных по операциям с наличными.
     *
     * @param counters Карта счетчиков смены (scope = SHIFT), содержащая статистику операций.
     * @return Список объектов [MoneyPlacementAggregate] для каждой операции.
     */
    fun build(counters: Map<String, Long>): List<MoneyPlacementAggregate> {
        val result = mutableListOf<MoneyPlacementAggregate>()

        for (op in operations) {
            val totalCount = counters[CounterKeyFormats.MONEY_PLACEMENT_TOTAL_COUNT.format(op)] ?: 0L
            val count = counters[CounterKeyFormats.MONEY_PLACEMENT_COUNT.format(op)] ?: 0L
            val sum = counters[CounterKeyFormats.MONEY_PLACEMENT_SUM.format(op)] ?: 0L
            val offlineCount = counters[CounterKeyFormats.MONEY_PLACEMENT_OFFLINE_COUNT.format(op)] ?: 0L

            result += MoneyPlacementAggregate(
                operation = op,
                operationsTotalCount = totalCount,
                operationsCount = count,
                operationsSumBills = sum,
                offlineCount = offlineCount
            )
        }

        return result
    }
}
