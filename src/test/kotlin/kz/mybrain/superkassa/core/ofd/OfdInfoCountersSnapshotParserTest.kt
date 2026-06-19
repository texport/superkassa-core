package kz.mybrain.superkassa.core.ofd

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kz.mybrain.superkassa.core.application.policy.CounterKeyFormats
import kz.mybrain.superkassa.core.application.service.OfdInfoCountersSnapshotParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class OfdInfoCountersSnapshotParserTest {

    @Test
    fun `parse report x snapshot with shift and global counters`() {
        val response = Json.parseToJsonElement(
            """
            {
              "payload": {
                "report": {
                  "reportType": "REPORT_X",
                  "zxReport": {
                    "shiftNumber": 12,
                    "openShiftTime": {
                      "date": { "year": 2026, "month": 3, "day": 19 },
                      "time": { "hour": 10, "minute": 0, "second": 0 }
                    },
                    "operations": [
                      { "operation": "OPERATION_SELL", "count": 2, "sum": { "bills": 1000, "coins": 0 } }
                    ],
                    "ticketOperations": [
                      {
                        "operation": "OPERATION_SELL",
                        "ticketsTotalCount": 2,
                        "ticketsCount": 2,
                        "ticketsSum": { "bills": 1000, "coins": 0 },
                        "payments": [
                          { "payment": "PAYMENT_CASH", "sum": { "bills": 700, "coins": 0 }, "count": 1 },
                          { "payment": "PAYMENT_CARD", "sum": { "bills": 300, "coins": 0 }, "count": 1 }
                        ],
                        "offlineCount": 0,
                        "discountSum": { "bills": 0, "coins": 0 },
                        "markupSum": { "bills": 0, "coins": 0 },
                        "changeSum": { "bills": 50, "coins": 0 }
                      }
                    ],
                    "cashSum": { "bills": 700, "coins": 0 },
                    "revenue": { "sum": { "bills": 1000, "coins": 0 }, "isNegative": false },
                    "nonNullableSums": [
                      { "operation": "OPERATION_SELL", "sum": { "bills": 5000, "coins": 0 } }
                    ],
                    "startShiftNonNullableSums": [
                      { "operation": "OPERATION_SELL", "sum": { "bills": 4000, "coins": 0 } }
                    ]
                  }
                }
              }
            }
            """.trimIndent()
        ).jsonObject

        val snapshot = OfdInfoCountersSnapshotParser.parse(response)
        assertNotNull(snapshot)
        assertTrue(snapshot.isOpenShift)
        assertEquals(12, snapshot.shiftNumber)

        val globalSellKey = CounterKeyFormats.NON_NULLABLE_SUM.format("OPERATION_SELL")
        assertEquals(5000L, snapshot.globalCounters[globalSellKey])

        assertEquals(2L, snapshot.shiftCounters[CounterKeyFormats.OPERATION_COUNT.format("OPERATION_SELL")])
        assertEquals(1000L, snapshot.shiftCounters[CounterKeyFormats.OPERATION_SUM.format("OPERATION_SELL")])
        assertEquals(
            4000L,
            snapshot.shiftCounters[CounterKeyFormats.START_SHIFT_NON_NULLABLE_SUM.format("OPERATION_SELL")]
        )
        assertEquals(700L, snapshot.shiftCounters[CounterKeyFormats.CASH_SUM])
    }

    @Test
    fun `parse report z snapshot from service last z report`() {
        val response = Json.parseToJsonElement(
            """
            {
              "payload": {
                "service": {
                  "lastZReport": {
                    "shiftNumber": 25,
                    "closeShiftTime": {
                      "date": { "year": 2026, "month": 3, "day": 18 },
                      "time": { "hour": 20, "minute": 1, "second": 2 }
                    },
                    "cashSum": { "bills": 0, "coins": 0 },
                    "revenue": { "sum": { "bills": 0, "coins": 0 }, "isNegative": false },
                    "nonNullableSums": [
                      { "operation": "OPERATION_BUY", "sum": { "bills": 2100, "coins": 0 } }
                    ]
                  }
                }
              }
            }
            """.trimIndent()
        ).jsonObject

        val snapshot = OfdInfoCountersSnapshotParser.parse(response)
        assertNotNull(snapshot)
        assertFalse(snapshot.isOpenShift)
        assertEquals("REPORT_Z", snapshot.reportType)
        assertEquals(25, snapshot.shiftNumber)

        val globalBuyKey = CounterKeyFormats.NON_NULLABLE_SUM.format("OPERATION_BUY")
        assertEquals(2100L, snapshot.globalCounters[globalBuyKey])
    }
}
