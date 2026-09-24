package io.github.texport.superkassa.core.domain.impl.helper

import io.github.texport.superkassa.core.domain.api.model.common.CounterKeyFormats
import io.github.texport.superkassa.core.domain.api.model.common.format
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Сверка берёт из отчёта БФД каждую строку в счётчик того же смысла:
 * позиции — в строку операций, чеки — в строку чеков, скидки и наценки —
 * поштучно, и счёт «за всё время» на начало смены.
 */
class OfdReportCountersParserTest {

    @Test
    fun `позиции, чеки, скидки, наценки, отделы и деньги БФД ложатся в счётчики того же смысла`() {
        val shift = OfdInfoCountersSnapshotParser.parse(REPORT).shiftCounters

        val expected = mapOf(
            CounterKeyFormats.OPERATION_COUNT.format(SELL) to 7L,
            CounterKeyFormats.TICKET_COUNT.format(SELL) to 5L,
            CounterKeyFormats.TICKET_TOTAL_COUNT.format(SELL) to 12L,
            CounterKeyFormats.START_SHIFT_TICKET_TOTAL_COUNT.format(SELL) to 7L,
            CounterKeyFormats.DISCOUNT_COUNT.format(SELL) to 2L,
            CounterKeyFormats.DISCOUNT_SUM.format(SELL) to 24_000L,
            CounterKeyFormats.MARKUP_COUNT.format(SELL) to 3L,
            CounterKeyFormats.MARKUP_SUM.format(SELL) to 6_550L,
            CounterKeyFormats.SECTION_OPERATION_COUNT.format("001", SELL) to 7L,
            CounterKeyFormats.SECTION_OPERATION_SUM.format("001", SELL) to 635_000L,
            CounterKeyFormats.PAYMENT_COUNT.format(SELL, "PAYMENT_CARD") to 1L,
            CounterKeyFormats.MONEY_PLACEMENT_COUNT.format(DEPOSIT) to 1L,
            CounterKeyFormats.MONEY_PLACEMENT_TOTAL_COUNT.format(DEPOSIT) to 2L,
            CounterKeyFormats.START_SHIFT_MONEY_PLACEMENT_TOTAL_COUNT.format(DEPOSIT) to 1L,
            CounterKeyFormats.MONEY_PLACEMENT_SUM.format(DEPOSIT) to 500_000L,
            CounterKeyFormats.MONEY_PLACEMENT_OFFLINE_COUNT.format(DEPOSIT) to 1L
        )
        assertEquals(expected, shift.filterKeys { it in expected })
    }

    private companion object {
        const val SELL = "OPERATION_SELL"
        const val DEPOSIT = "MONEY_PLACEMENT_DEPOSIT"

        val REPORT = Json.parseToJsonElement(
            """
            {"payload": {"report": {"reportType": "REPORT_X", "zxReport": {
              "shiftNumber": 2,
              "sections": [{"sectionCode": "001", "operations": [
                {"operation": "OPERATION_SELL", "count": 7, "sum": {"bills": 6350}}]}, {"operations": []}],
              "operations": [{"operation": "OPERATION_SELL", "count": 7, "sum": {"bills": 6350}}],
              "discounts": [{"operation": "OPERATION_SELL", "count": 2, "sum": {"bills": 240}}],
              "markups": [{"operation": "OPERATION_SELL", "count": 3, "sum": {"bills": 65, "coins": 50}}, {"count": 1}],
              "ticketOperations": [{"operation": "OPERATION_SELL", "ticketsTotalCount": 12, "ticketsCount": 5,
                "ticketsSum": {"bills": 6175}, "payments": [{"payment": "PAYMENT_CARD", "count": 1, "sum": {"bills": 875}}]},
                {"ticketsCount": 1}],
              "moneyPlacements": [{"operation": "MONEY_PLACEMENT_DEPOSIT", "operationsTotalCount": 2,
                "operationsCount": 1, "operationsSum": {"bills": 5000}, "offlineCount": 1}, {"operationsCount": 1}],
              "cashSum": {"bills": 14300}
            }}}}
            """.trimIndent()
        ).jsonObject
    }
}
