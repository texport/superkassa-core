package kz.mybrain.superkassa.core.domain.helper.zxreport

import kz.mybrain.superkassa.core.domain.model.zxreport.OperationAggregate
import kz.mybrain.superkassa.core.domain.model.zxreport.SectionAggregate
import kz.mybrain.superkassa.core.domain.model.common.CounterKeyFormats
import kz.mybrain.superkassa.core.domain.model.common.format

/**
 * Объект-построитель блока операций (operations), секций (sections), скидок (discounts),
 * наценок (markups), общих итогов (totalResult) и выручки (revenue) для Zx-отчета.
 */
object ZxOperationsBlockBuilder {

    /**
     * Вычисляет общую сумму выручки за смену.
     *
     * Метод сначала проверяет наличие явного счетчика выручки [CounterKeyFormats.REVENUE_SUM]
     * и знака выручки [CounterKeyFormats.REVENUE_IS_NEGATIVE].
     * Если данные счетчики отсутствуют, применяется резервная (старая) логика расчета по операциям:
     * выручка = (продажи + покупки) - (возвраты продаж + возвраты покупок).
     *
     * @param counters Карта счетчиков смены.
     * @return Пара (сумма выручки в минимальных денежных единицах, остаток в монетах - всегда 0).
     */
    fun resolveRevenue(counters: Map<String, Long>): Pair<Long, Int> {
        val revenueBillsCounter = counters[CounterKeyFormats.REVENUE_SUM]
        val isNegativeFlag = counters[CounterKeyFormats.REVENUE_IS_NEGATIVE] == 1L

        if (revenueBillsCounter != null) {
            val abs = kotlin.math.abs(revenueBillsCounter)
            val signed = if (isNegativeFlag) -abs else abs
            return signed to 0
        }

        // Фолбэк на старую схему через операции / неотменяемые суммы.
        val operations = operationsList()

        var total = 0L
        operations.forEach { op ->
            val nonNullableKey = CounterKeyFormats.NON_NULLABLE_SUM.format(op)
            val opSumKey = CounterKeyFormats.OPERATION_SUM.format(op)
            val sum = counters[nonNullableKey] ?: counters[opSumKey] ?: 0L
            if (sum != 0L) {
                total += when (op) {
                    "OPERATION_SELL", "OPERATION_BUY" -> sum
                    "OPERATION_SELL_RETURN", "OPERATION_BUY_RETURN" -> -sum
                    else -> sum
                }
            }
        }
        return total to 0
    }

    /**
     * Формирует список агрегированных данных по кассовым операциям за смену.
     *
     * @param counters Карта счетчиков смены.
     * @return Список объектов [OperationAggregate], содержащих количество и суммы по операциям.
     */
    fun resolveOperations(counters: Map<String, Long>): List<OperationAggregate> {
        val result = mutableListOf<OperationAggregate>()
        for (op in operationsList()) {
            val count = counters[CounterKeyFormats.OPERATION_COUNT.format(op)] ?: 0L
            val sum = counters[CounterKeyFormats.OPERATION_SUM.format(op)] ?: 0L
            result += OperationAggregate(operation = op, count = count, sumBills = sum)
        }
        return result
    }

    /**
     * Вычисляет агрегированные данные по отделам/секциям (sections) кассы.
     *
     * Если счетчики отделов отсутствуют, возвращает одну дефолтную секцию "1"
     * с операциями из [fallbackOperations].
     *
     * @param counters Карта счетчиков смены.
     * @param fallbackOperations Список операций для резервного заполнения дефолтной секции.
     * @return Список объектов [SectionAggregate] по отделам.
     */
    fun resolveSections(
        counters: Map<String, Long>,
        fallbackOperations: List<OperationAggregate>
    ): List<SectionAggregate> {
        // Если секционных счётчиков нет, сохраняем поведение по умолчанию: одна секция "1".
        val sectionKeys = counters.keys.filter { it.startsWith("section.") }
        if (sectionKeys.isEmpty()) {
            if (fallbackOperations.isEmpty()) return emptyList()
            return listOf(SectionAggregate(sectionCode = "1", operations = fallbackOperations))
        }

        // Собираем коды секций из ключей формата "section.%s.operation.%s.(count|sum)".
        val sectionCodes = sectionKeys.mapNotNull { key ->
            val parts = key.split('.')
            if (parts.size >= 4 && parts[0] == "section" && parts[2] == "operation") {
                parts[1]
            } else {
                null
            }
        }.toSet().sorted()

        val result = mutableListOf<SectionAggregate>()
        for (sectionCode in sectionCodes) {
            val ops = operationsList().map { op ->
                val countKey = CounterKeyFormats.SECTION_OPERATION_COUNT.format(sectionCode, op)
                val sumKey = CounterKeyFormats.SECTION_OPERATION_SUM.format(sectionCode, op)
                val count = counters[countKey] ?: 0L
                val sum = counters[sumKey] ?: 0L
                OperationAggregate(operation = op, count = count, sumBills = sum)
            }
            result += SectionAggregate(sectionCode = sectionCode, operations = ops)
        }
        return result
    }

    /**
     * Формирует агрегированную информацию по предоставленным скидкам (discounts) за смену.
     *
     * @param counters Карта счетчиков смены.
     * @return Список объектов [OperationAggregate] с суммами скидок для каждого типа операции.
     */
    fun resolveDiscounts(counters: Map<String, Long>): List<OperationAggregate> {
        val result = mutableListOf<OperationAggregate>()
        for (op in operationsList()) {
            val sum = counters[CounterKeyFormats.DISCOUNT_SUM.format(op)] ?: 0L
            val count = counters[CounterKeyFormats.OPERATION_COUNT.format(op)] ?: 0L
            result += OperationAggregate(operation = op, count = count, sumBills = sum)
        }
        return result
    }

    /**
     * Формирует агрегированную информацию по наценкам (markups) за смену.
     *
     * @param counters Карта счетчиков смены.
     * @return Список объектов [OperationAggregate] с суммами наценок для каждого типа операции.
     */
    fun resolveMarkups(counters: Map<String, Long>): List<OperationAggregate> {
        val result = mutableListOf<OperationAggregate>()
        for (op in operationsList()) {
            val sum = counters[CounterKeyFormats.MARKUP_SUM.format(op)] ?: 0L
            val count = counters[CounterKeyFormats.OPERATION_COUNT.format(op)] ?: 0L
            result += OperationAggregate(operation = op, count = count, sumBills = sum)
        }
        return result
    }

    /**
     * Вычисляет общие итоги смены с учетом скидок и наценок.
     *
     * Итоговая сумма для каждой операции рассчитывается по формуле:
     * итого = сумма_операции - сумма_скидок + сумма_наценок.
     *
     * @param counters Карта счетчиков смены.
     * @return Список объектов [OperationAggregate] с скорректированными итоговыми суммами.
     */
    fun resolveTotalResult(counters: Map<String, Long>): List<OperationAggregate> {
        val result = mutableListOf<OperationAggregate>()
        for (op in operationsList()) {
            val count = counters[CounterKeyFormats.OPERATION_COUNT.format(op)] ?: 0L
            val baseSum = counters[CounterKeyFormats.OPERATION_SUM.format(op)] ?: 0L
            val discount = counters[CounterKeyFormats.DISCOUNT_SUM.format(op)] ?: 0L
            val markup = counters[CounterKeyFormats.MARKUP_SUM.format(op)] ?: 0L
            val adjustedSum = baseSum - discount + markup
            result += OperationAggregate(
                operation = op,
                count = count,
                sumBills = adjustedSum
            )
        }
        return result
    }

    /**
     * Возвращает список кодов кассовых операций.
     */
    private fun operationsList(): List<String> =
        listOf("OPERATION_SELL", "OPERATION_SELL_RETURN", "OPERATION_BUY", "OPERATION_BUY_RETURN")
}

