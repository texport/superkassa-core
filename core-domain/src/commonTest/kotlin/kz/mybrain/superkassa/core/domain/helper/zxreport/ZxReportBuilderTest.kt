package kz.mybrain.superkassa.core.domain.helper.zxreport

import kz.mybrain.superkassa.core.domain.model.common.CounterKeyFormats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ZxReportBuilderTest {

    @Test
    fun `build constructs complete ZxReportInput correctly with active and missing counters`() {
        val counters = mapOf(
            CounterKeyFormats.CASH_SUM to 12000L,
            CounterKeyFormats.REVENUE_SUM to 15000L,
            CounterKeyFormats.REVENUE_IS_NEGATIVE to 0L,
            CounterKeyFormats.NON_NULLABLE_SUM.format("OPERATION_SELL") to 50000L,
            CounterKeyFormats.START_SHIFT_NON_NULLABLE_SUM.format("OPERATION_SELL") to 45000L
        )

        val report = ZxReportBuilder.build(
            counters = counters,
            dateTimeMillis = 1625097600000L,
            shiftNumber = 42,
            openShiftTimeMillis = 1625068800000L,
            closeShiftTimeMillis = 1625097600000L
        )

        assertEquals(1625097600000L, report.dateTimeMillis)
        assertEquals(42, report.shiftNumber)
        assertEquals(1625068800000L, report.openShiftTimeMillis)
        assertEquals(1625097600000L, report.closeShiftTimeMillis)
        assertEquals(12000L, report.cashSumBills)
        assertEquals(15000L, report.revenueBills)
        assertEquals(0, report.revenueCoins)
        assertNotNull(report.nonNullableSums)
        assertNotNull(report.startShiftNonNullableSums)
        assertNotNull(report.sections)
        assertNotNull(report.operations)
        assertNotNull(report.discounts)
        assertNotNull(report.markups)
        assertNotNull(report.totalResult)
        assertNotNull(report.ticketOperations)
        assertNotNull(report.moneyPlacements)
        assertNotNull(report.taxes)
    }

    @Test
    fun `build support null closeShiftTimeMillis for X-report`() {
        val counters = emptyMap<String, Long>()

        val report = ZxReportBuilder.build(
            counters = counters,
            dateTimeMillis = 1625097600000L,
            shiftNumber = 42,
            openShiftTimeMillis = 1625068800000L,
            closeShiftTimeMillis = null
        )

        assertNull(report.closeShiftTimeMillis)
    }
}
