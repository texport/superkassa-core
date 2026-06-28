package kz.mybrain.superkassa.core.domain.helper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kz.mybrain.superkassa.core.domain.model.common.CounterKeyFormats

class OfdInfoCountersSnapshotParserTest {

    @Test
    fun parseReportXSnapshotWithShiftAndGlobalCounters() {
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
                        "ticketsSum": { "ticketsSum": { "bills": 1000 } },
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
    fun parseReportZSnapshotFromServiceLastZReport() {
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
                    "revenue": { "sum": { "bills": 0, "coins": 0 }, "isNegative": true },
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
        assertEquals(1L, snapshot.shiftCounters[CounterKeyFormats.REVENUE_IS_NEGATIVE])
    }

    @Test
    fun parseMissingPayloadThrows() {
        val response = Json.parseToJsonElement("{}").jsonObject
        assertFailsWith<IllegalArgumentException> {
            OfdInfoCountersSnapshotParser.parse(response)
        }
    }

    @Test
    fun parseMissingReportAndServiceLastZReportThrows() {
        val response = Json.parseToJsonElement(
            """
            {
              "payload": {
                "report": {}
              }
            }
            """.trimIndent()
        ).jsonObject
        assertFailsWith<IllegalArgumentException> {
            OfdInfoCountersSnapshotParser.parse(response)
        }
    }

    @Test
    fun parseCorruptDateAndTimes() {
        val buildResponse = { dateJson: String, timeJson: String ->
            Json.parseToJsonElement(
                """
                {
                  "payload": {
                    "report": {
                      "reportType": "REPORT_X",
                      "zxReport": {
                        "shiftNumber": 1,
                        "openShiftTime": {
                          "date": $dateJson,
                          "time": $timeJson
                        }
                      }
                    }
                  }
                }
                """.trimIndent()
            ).jsonObject
        }

        // Missing year
        val res1 = buildResponse("{ \"month\": 3, \"day\": 19 }", "{ \"hour\": 10 }")
        assertNull(OfdInfoCountersSnapshotParser.parse(res1).openShiftTimeMillis)

        // Missing month
        val res2 = buildResponse("{ \"year\": 2026, \"day\": 19 }", "{ \"hour\": 10 }")
        assertNull(OfdInfoCountersSnapshotParser.parse(res2).openShiftTimeMillis)

        // Missing day
        val res3 = buildResponse("{ \"year\": 2026, \"month\": 3 }", "{ \"hour\": 10 }")
        assertNull(OfdInfoCountersSnapshotParser.parse(res3).openShiftTimeMillis)

        // Invalid date values (e.g. Feb 30)
        val res4 = buildResponse("{ \"year\": 2026, \"month\": 2, \"day\": 30 }", "{ \"hour\": 10 }")
        assertNull(OfdInfoCountersSnapshotParser.parse(res4).openShiftTimeMillis)

        // Null date
        val res5 = buildResponse("null", "{ \"hour\": 10 }")
        assertNull(OfdInfoCountersSnapshotParser.parse(res5).openShiftTimeMillis)

        // Null time
        val res6 = buildResponse("{ \"year\": 2026, \"month\": 3, \"day\": 19 }", "null")
        assertNull(OfdInfoCountersSnapshotParser.parse(res6).openShiftTimeMillis)
    }

    @Test
    fun parseSkipElementsWithMissingOperationOrPayment() {
        val response = Json.parseToJsonElement(
            """
            {
              "payload": {
                "report": {
                  "reportType": "REPORT_X",
                  "zxReport": {
                    "shiftNumber": 1,
                    "operations": [
                      { "count": 2 }
                    ],
                    "ticketOperations": [
                      {
                        "ticketsTotalCount": 2,
                        "payments": [
                          { "sum": { "bills": 100 } }
                        ]
                      }
                    ],
                    "nonNullableSums": [
                      { "sum": { "bills": 200 } }
                    ],
                    "startShiftNonNullableSums": [
                      { "sum": { "bills": 300 } }
                    ]
                  }
                }
              }
            }
            """.trimIndent()
        ).jsonObject

        val snapshot = OfdInfoCountersSnapshotParser.parse(response)
        // Ensure no exception was thrown, but the fields were skipped
        assertEquals(mapOf(CounterKeyFormats.CASH_SUM to 0L), snapshot.shiftCounters)
        assertTrue(snapshot.globalCounters.isEmpty())
    }

    @Test
    fun parseMoneyBillsEdgeCases() {
        val response = Json.parseToJsonElement(
            """
            {
              "payload": {
                "report": {
                  "reportType": "REPORT_X",
                  "zxReport": {
                    "shiftNumber": 1,
                    "cashSum": {},
                    "revenue": { "sum": {} }
                  }
                }
              }
            }
            """.trimIndent()
        ).jsonObject

        val snapshot = OfdInfoCountersSnapshotParser.parse(response)
        assertEquals(0L, snapshot.shiftCounters[CounterKeyFormats.CASH_SUM])
        assertEquals(0L, snapshot.shiftCounters[CounterKeyFormats.REVENUE_SUM])
    }
}
