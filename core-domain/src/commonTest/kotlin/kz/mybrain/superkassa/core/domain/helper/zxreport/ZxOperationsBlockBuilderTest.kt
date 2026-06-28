package kz.mybrain.superkassa.core.domain.helper.zxreport

import kz.mybrain.superkassa.core.domain.model.common.CounterKeyFormats
import kz.mybrain.superkassa.core.domain.model.zxreport.OperationAggregate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ZxOperationsBlockBuilderTest {

    @Test
    fun `resolveSections falls back to single section when no section counters`() {
        val counters = emptyMap<String, Long>()
        val fallback = listOf(
            OperationAggregate(
                operation = "OPERATION_SELL",
                count = 2L,
                sumBills = 2_000L
            )
        )

        val sections = ZxOperationsBlockBuilder.resolveSections(counters, fallback)

        assertEquals(1, sections.size)
        val section = sections.first()
        assertEquals("1", section.sectionCode)
        assertEquals(fallback, section.operations)
    }

    @Test
    fun `resolveSections returns empty list when no section counters and fallback operations is empty`() {
        val counters = emptyMap<String, Long>()
        val sections = ZxOperationsBlockBuilder.resolveSections(counters, emptyList())
        assertTrue(sections.isEmpty())
    }

    @Test
    fun `resolveSections filters out invalid section key formats`() {
        val counters = mapOf(
            "section.001" to 10L,
            "section.001.operation" to 5L,
            "section.001.operation.OPERATION_SELL.count" to 2L
        )
        val sections = ZxOperationsBlockBuilder.resolveSections(counters, emptyList())
        assertEquals(1, sections.size)
        assertEquals("001", sections.first().sectionCode)
    }

    @Test
    fun `resolveSections builds sections from section counters`() {
        val counters = mutableMapOf<String, Long>().apply {
            put(
                CounterKeyFormats.SECTION_OPERATION_COUNT.format("001", "OPERATION_SELL"),
                2L
            )
            put(
                CounterKeyFormats.SECTION_OPERATION_SUM.format("001", "OPERATION_SELL"),
                2_000L
            )
            put(
                CounterKeyFormats.SECTION_OPERATION_COUNT.format("002", "OPERATION_SELL"),
                1L
            )
            put(
                CounterKeyFormats.SECTION_OPERATION_SUM.format("002", "OPERATION_SELL"),
                1_000L
            )
        }

        val fallback = listOf(
            OperationAggregate(
                operation = "OPERATION_SELL",
                count = 3L,
                sumBills = 3_000L
            )
        )

        val sections = ZxOperationsBlockBuilder.resolveSections(counters, fallback)

        assertEquals(2, sections.size)
        val first = sections[0]
        val second = sections[1]

        assertEquals("001", first.sectionCode)
        assertEquals(4, first.operations.size)
        val firstSell = first.operations.first { it.operation == "OPERATION_SELL" }
        assertEquals(2L, firstSell.count)
        assertEquals(2_000L, firstSell.sumBills)
        // Остальные операции для секции "001" должны быть с нулевыми значениями.
        first.operations.filter { it.operation != "OPERATION_SELL" }.forEach {
            assertEquals(0L, it.count)
            assertEquals(0L, it.sumBills)
        }

        assertEquals("002", second.sectionCode)
        assertEquals(4, second.operations.size)
        val secondSell = second.operations.first { it.operation == "OPERATION_SELL" }
        assertEquals(1L, secondSell.count)
        assertEquals(1_000L, secondSell.sumBills)
        second.operations.filter { it.operation != "OPERATION_SELL" }.forEach {
            assertEquals(0L, it.count)
            assertEquals(0L, it.sumBills)
        }

        // Убеждаемся, что fallback не используется, когда есть реальные секционные счётчики.
        assertTrue(sections.flatMap { it.operations }.all { it.count != 3L && it.sumBills != 3_000L })
    }

    @Test
    fun `resolve aggregates and totalResult produce full operation set with discounts and markups applied`() {
        val counters = mutableMapOf<String, Long>().apply {
            // Только SELL имеет ненулевые значения.
            put(CounterKeyFormats.OPERATION_COUNT.format("OPERATION_SELL"), 2L)
            put(CounterKeyFormats.OPERATION_SUM.format("OPERATION_SELL"), 2_000L)
            put(CounterKeyFormats.DISCOUNT_SUM.format("OPERATION_SELL"), 100L)
            put(CounterKeyFormats.MARKUP_SUM.format("OPERATION_SELL"), 50L)
        }

        val operations = ZxOperationsBlockBuilder.resolveOperations(counters)
        val discounts = ZxOperationsBlockBuilder.resolveDiscounts(counters)
        val markups = ZxOperationsBlockBuilder.resolveMarkups(counters)
        val totalResult = ZxOperationsBlockBuilder.resolveTotalResult(counters)

        // Во всех агрегатах должен быть полный набор операций (4 элемента).
        assertEquals(4, operations.size)
        assertEquals(4, discounts.size)
        assertEquals(4, markups.size)
        assertEquals(4, totalResult.size)

        fun findOp(list: List<OperationAggregate>, op: String) =
            list.first { it.operation == op }

        // Для SELL значения берутся из счетчиков.
        with(findOp(operations, "OPERATION_SELL")) {
            assertEquals(2L, count)
            assertEquals(2_000L, sumBills)
        }
        with(findOp(discounts, "OPERATION_SELL")) {
            assertEquals(2L, count)
            assertEquals(100L, sumBills)
        }
        with(findOp(markups, "OPERATION_SELL")) {
            assertEquals(2L, count)
            assertEquals(50L, sumBills)
        }
        with(findOp(totalResult, "OPERATION_SELL")) {
            // totalResult = OPERATION_SUM - DISCOUNT_SUM + MARKUP_SUM.
            assertEquals(2L, count)
            assertEquals(2_000L - 100L + 50L, sumBills)
        }

        // Для остальных операций значения должны быть нулевыми.
        listOf("OPERATION_SELL_RETURN", "OPERATION_BUY", "OPERATION_BUY_RETURN").forEach { op ->
            with(findOp(operations, op)) {
                assertEquals(0L, count)
                assertEquals(0L, sumBills)
            }
            with(findOp(discounts, op)) {
                assertEquals(0L, count)
                assertEquals(0L, sumBills)
            }
            with(findOp(markups, op)) {
                assertEquals(0L, count)
                assertEquals(0L, sumBills)
            }
            with(findOp(totalResult, op)) {
                assertEquals(0L, count)
                assertEquals(0L, sumBills)
            }
        }
    }

    @Test
    fun `resolveRevenue returns positive signed revenue sum`() {
        val counters = mapOf(
            CounterKeyFormats.REVENUE_SUM to 1000L,
            CounterKeyFormats.REVENUE_IS_NEGATIVE to 0L
        )
        val (sum, coins) = ZxOperationsBlockBuilder.resolveRevenue(counters)
        assertEquals(1000L, sum)
        assertEquals(0, coins)
    }

    @Test
    fun `resolveRevenue returns negative signed revenue sum`() {
        val counters = mapOf(
            CounterKeyFormats.REVENUE_SUM to 1000L,
            CounterKeyFormats.REVENUE_IS_NEGATIVE to 1L
        )
        val (sum, coins) = ZxOperationsBlockBuilder.resolveRevenue(counters)
        assertEquals(-1000L, sum)
        assertEquals(0, coins)
    }

    @Test
    fun `resolveRevenue falls back to operation sums when explicit revenue sum is missing`() {
        val counters = mapOf(
            CounterKeyFormats.NON_NULLABLE_SUM.format("OPERATION_SELL") to 500L,
            CounterKeyFormats.OPERATION_SUM.format("OPERATION_BUY") to 300L,
            CounterKeyFormats.NON_NULLABLE_SUM.format("OPERATION_SELL_RETURN") to 100L,
            CounterKeyFormats.OPERATION_SUM.format("OPERATION_BUY_RETURN") to 50L
        )
        val (sum, coins) = ZxOperationsBlockBuilder.resolveRevenue(counters)
        // 500 + 300 - 100 - 50 = 650
        assertEquals(650L, sum)
        assertEquals(0, coins)
    }
}
