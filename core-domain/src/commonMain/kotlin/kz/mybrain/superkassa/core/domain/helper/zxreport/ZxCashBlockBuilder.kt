package kz.mybrain.superkassa.core.domain.helper.zxreport

import kz.mybrain.superkassa.core.domain.model.common.CounterKeyFormats
import kz.mybrain.superkassa.core.domain.model.common.format

/**
 * Объект-построитель блока наличных (cash) для Zx-отчета.
 *
 * Отвечает за вычисление:
 * - Текущей суммы наличных в кассе (`cashSum`);
 * - Накопительных (необнуляемых) сумм на начало смены (`startShiftNonNullableSums`);
 * - Накопительных (необнуляемых) сумм на конец смены (`nonNullableSums`).
 */
object ZxCashBlockBuilder {

    /**
     * Вычисляет текущую сумму наличных в кассе.
     *
     * Сначала пытается получить значение по явному счетчику [CounterKeyFormats.CASH_SUM].
     * Если счетчик отсутствует, использует резервную логику: сумму продаж по операции продажи (OPERATION_SELL).
     *
     * @param counters Карта счетчиков смены.
     * @return Сумма наличных денег в кассе в минимальных денежных единицах (тиын/копейки).
     */
    fun resolveCashSum(counters: Map<String, Long>): Long {
        // Предпочитаем явный счетчик CASH_SUM, если он есть.
        val explicitCash = counters[CounterKeyFormats.CASH_SUM]
        if (explicitCash != null) {
            return explicitCash
        }

        // Фолбэк на старую логику: сумма продаж по операции SELL.
        val sellKey = CounterKeyFormats.OPERATION_SUM.format("OPERATION_SELL")
        return counters[sellKey] ?: 0L
    }

    /**
     * Вычисляет конечные накопительные (необнуляемые) суммы по всем типам операций.
     *
     * Пытается прочитать готовое значение накопительного итога [CounterKeyFormats.NON_NULLABLE_SUM].
     * Если счетчик отсутствует, рассчитывает его как сумму на начало смены плюс оборот по операции за смену.
     *
     * @param counters Карта счетчиков смены.
     * @return Список пар (название операции, накопительная сумма на конец смены).
     */
    fun resolveNonNullableSums(counters: Map<String, Long>): List<Pair<String, Long>> {
        val result = mutableListOf<Pair<String, Long>>()
        for (op in operationsList()) {
            val explicitEndKey = CounterKeyFormats.NON_NULLABLE_SUM.format(op)
            val explicitEnd = counters[explicitEndKey]
            if (explicitEnd != null) {
                result += op to explicitEnd
            } else {
                val startKey = CounterKeyFormats.START_SHIFT_NON_NULLABLE_SUM.format(op)
                val opSumKey = CounterKeyFormats.OPERATION_SUM.format(op)
                val start = counters[startKey] ?: 0L
                val delta = counters[opSumKey] ?: 0L
                val endValue = start + delta
                result += op to endValue
            }
        }
        return result
    }

    /**
     * Возвращает начальные накопительные (необнуляемые) суммы на начало смены по всем типам операций.
     *
     * @param counters Карта счетчиков смены.
     * @return Список пар (название операции, накопительная сумма на начало смены).
     */
    fun resolveStartShiftNonNullableSums(counters: Map<String, Long>): List<Pair<String, Long>> {
        val result = mutableListOf<Pair<String, Long>>()
        for (op in operationsList()) {
            val key = CounterKeyFormats.START_SHIFT_NON_NULLABLE_SUM.format(op)
            val sum = counters[key] ?: 0L
            result += op to sum
        }
        return result
    }

    /**
     * Возвращает список кодов основных кассовых операций.
     */
    private fun operationsList(): List<String> =
        listOf("OPERATION_SELL", "OPERATION_SELL_RETURN", "OPERATION_BUY", "OPERATION_BUY_RETURN")
}

