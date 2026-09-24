package io.github.texport.superkassa.core.domain.impl.helper

import io.github.texport.superkassa.core.domain.api.model.common.CounterKeyFormats
import io.github.texport.superkassa.core.domain.api.model.common.format
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

/**
 * Строки X/Z-отчёта БФД в счётчики смены кассы — поле в поле по смыслу.
 *
 * Что считает каждая строка, взято у БФД (`OperationCalculator`):
 * операции, отделы и итог — позиции, скидки и наценки — каждую отдельно,
 * строка чеков — чеки, оплаты — оплаты. «За всё время» у чеков и операций
 * с деньгами БФД переносит из смены в смену: разница с числом за смену —
 * счёт на начало смены. Прежде число позиций БФД ложилось в счётчик, где
 * касса держала число чеков, а скидки, наценки, отделы и операции
 * с деньгами из отчёта не брались вовсе.
 */
internal object OfdReportCountersParser {

    /** Операции, отделы, скидки и наценки. */
    fun operations(zx: JsonObject, into: MutableMap<String, Long>) {
        lines(zx, "operations", into, CounterKeyFormats.OPERATION_COUNT, CounterKeyFormats.OPERATION_SUM)
        lines(zx, "discounts", into, CounterKeyFormats.DISCOUNT_COUNT, CounterKeyFormats.DISCOUNT_SUM)
        lines(zx, "markups", into, CounterKeyFormats.MARKUP_COUNT, CounterKeyFormats.MARKUP_SUM)
        zx["sections"]?.jsonArray?.forEach { element ->
            val section = element.jsonObject
            val code = section["sectionCode"]?.jsonPrimitive?.content ?: return@forEach
            val count = CounterKeyFormats.SECTION_OPERATION_COUNT.format(code, "%s")
            val sum = CounterKeyFormats.SECTION_OPERATION_SUM.format(code, "%s")
            lines(section, "operations", into, count, sum)
        }
    }

    /** Строки чеков с оплатами и счёт чеков на начало смены. */
    fun tickets(zx: JsonObject, into: MutableMap<String, Long>) {
        zx["ticketOperations"]?.jsonArray?.forEach { element ->
            val line = element.jsonObject
            val op = line["operation"]?.jsonPrimitive?.content ?: return@forEach
            val total = count(line, "ticketsTotalCount")
            val shift = count(line, "ticketsCount")
            into[CounterKeyFormats.TICKET_TOTAL_COUNT.format(op)] = total
            into[CounterKeyFormats.START_SHIFT_TICKET_TOTAL_COUNT.format(op)] = total - shift
            into[CounterKeyFormats.TICKET_COUNT.format(op)] = shift
            into[CounterKeyFormats.TICKET_SUM.format(op)] = money(line, "ticketsSum")
            into[CounterKeyFormats.TICKET_OFFLINE_COUNT.format(op)] = count(line, "offlineCount")
            into[CounterKeyFormats.TICKET_DISCOUNT_SUM.format(op)] = money(line, "discountSum")
            into[CounterKeyFormats.TICKET_MARKUP_SUM.format(op)] = money(line, "markupSum")
            into[CounterKeyFormats.TICKET_CHANGE_SUM.format(op)] = money(line, "changeSum")
            val payCount = CounterKeyFormats.PAYMENT_COUNT.format(op, "%s")
            val paySum = CounterKeyFormats.PAYMENT_SUM.format(op, "%s")
            lines(line, "payments", into, payCount, paySum, kind = "payment")
        }
    }

    /** Внесения и изъятия и их счёт на начало смены. */
    fun placements(zx: JsonObject, into: MutableMap<String, Long>) {
        zx["moneyPlacements"]?.jsonArray?.forEach { element ->
            val line = element.jsonObject
            val op = line["operation"]?.jsonPrimitive?.content ?: return@forEach
            val total = count(line, "operationsTotalCount")
            val shift = count(line, "operationsCount")
            into[CounterKeyFormats.MONEY_PLACEMENT_TOTAL_COUNT.format(op)] = total
            into[CounterKeyFormats.START_SHIFT_MONEY_PLACEMENT_TOTAL_COUNT.format(op)] = total - shift
            into[CounterKeyFormats.MONEY_PLACEMENT_COUNT.format(op)] = shift
            into[CounterKeyFormats.MONEY_PLACEMENT_SUM.format(op)] = money(line, "operationsSum")
            into[CounterKeyFormats.MONEY_PLACEMENT_OFFLINE_COUNT.format(op)] = count(line, "offlineCount")
        }
    }

    /** Строки «вид — число — сумма» массива [array] в ключи [countKey] и [sumKey]. */
    private fun lines(
        parent: JsonObject,
        array: String,
        into: MutableMap<String, Long>,
        countKey: String,
        sumKey: String,
        kind: String = "operation"
    ) {
        parent[array]?.jsonArray?.forEach { element ->
            val line = element.jsonObject
            val name = line[kind]?.jsonPrimitive?.content ?: return@forEach
            into[countKey.format(name)] = count(line, "count")
            into[sumKey.format(name)] = money(line, "sum")
        }
    }

    private fun count(line: JsonObject, key: String): Long = line[key]?.jsonPrimitive?.long ?: 0L

    /** Деньги БФД парой «тенге и тиыны» — в тиынах целиком. */
    fun money(obj: JsonObject, key: String): Long {
        val money = obj[key]?.jsonObject ?: return 0L
        val bills = money["bills"]?.jsonPrimitive?.long ?: 0L
        val coins = money["coins"]?.jsonPrimitive?.long ?: 0L
        return bills * TIYN_IN_TENGE + coins
    }

    /** Тиынов в тенге. */
    private const val TIYN_IN_TENGE = 100L
}
