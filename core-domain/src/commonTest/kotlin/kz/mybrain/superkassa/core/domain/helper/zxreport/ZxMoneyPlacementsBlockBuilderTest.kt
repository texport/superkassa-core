package kz.mybrain.superkassa.core.domain.helper.zxreport

import kz.mybrain.superkassa.core.domain.model.common.CounterKeyFormats
import kotlin.test.Test
import kotlin.test.assertEquals

class ZxMoneyPlacementsBlockBuilderTest {

    @Test
    fun `build with empty counters returns default aggregates with zeros`() {
        val counters = emptyMap<String, Long>()
        val result = ZxMoneyPlacementsBlockBuilder.build(counters)

        assertEquals(2, result.size)

        val deposit = result.first { it.operation == "MONEY_PLACEMENT_DEPOSIT" }
        assertEquals(0L, deposit.operationsTotalCount)
        assertEquals(0L, deposit.operationsCount)
        assertEquals(0L, deposit.operationsSumBills)
        assertEquals(0L, deposit.offlineCount)

        val withdrawal = result.first { it.operation == "MONEY_PLACEMENT_WITHDRAWAL" }
        assertEquals(0L, withdrawal.operationsTotalCount)
        assertEquals(0L, withdrawal.operationsCount)
        assertEquals(0L, withdrawal.operationsSumBills)
        assertEquals(0L, withdrawal.offlineCount)
    }

    @Test
    fun `build maps counter values correctly`() {
        val counters = mapOf(
            CounterKeyFormats.MONEY_PLACEMENT_TOTAL_COUNT.format("MONEY_PLACEMENT_DEPOSIT") to 10L,
            CounterKeyFormats.MONEY_PLACEMENT_COUNT.format("MONEY_PLACEMENT_DEPOSIT") to 8L,
            CounterKeyFormats.MONEY_PLACEMENT_SUM.format("MONEY_PLACEMENT_DEPOSIT") to 5000L,
            CounterKeyFormats.MONEY_PLACEMENT_OFFLINE_COUNT.format("MONEY_PLACEMENT_DEPOSIT") to 2L,
            CounterKeyFormats.MONEY_PLACEMENT_TOTAL_COUNT.format("MONEY_PLACEMENT_WITHDRAWAL") to 5L,
            CounterKeyFormats.MONEY_PLACEMENT_COUNT.format("MONEY_PLACEMENT_WITHDRAWAL") to 4L,
            CounterKeyFormats.MONEY_PLACEMENT_SUM.format("MONEY_PLACEMENT_WITHDRAWAL") to 3000L,
            CounterKeyFormats.MONEY_PLACEMENT_OFFLINE_COUNT.format("MONEY_PLACEMENT_WITHDRAWAL") to 1L
        )

        val result = ZxMoneyPlacementsBlockBuilder.build(counters)
        assertEquals(2, result.size)

        val deposit = result.first { it.operation == "MONEY_PLACEMENT_DEPOSIT" }
        assertEquals(10L, deposit.operationsTotalCount)
        assertEquals(8L, deposit.operationsCount)
        assertEquals(5000L, deposit.operationsSumBills)
        assertEquals(2L, deposit.offlineCount)

        val withdrawal = result.first { it.operation == "MONEY_PLACEMENT_WITHDRAWAL" }
        assertEquals(5L, withdrawal.operationsTotalCount)
        assertEquals(4L, withdrawal.operationsCount)
        assertEquals(3000L, withdrawal.operationsSumBills)
        assertEquals(1L, withdrawal.offlineCount)
    }
}
