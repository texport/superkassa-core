package kz.mybrain.superkassa.core.domain.model.report

import kz.mybrain.superkassa.core.domain.model.delivery.DeliveryStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ReportModelTest {

    @Test
    fun testPrintDocumentTypeEnum() {
        for (type in PrintDocumentType.entries) {
            val value = PrintDocumentType.valueOf(type.name)
            assertEquals(type, value)
        }
    }

    @Test
    fun testReportResultEqualsAndHashCode() {
        val payload1 = byteArrayOf(10, 20, 30)
        val payload2 = byteArrayOf(10, 20, 30)
        val payload3 = byteArrayOf(40, 50, 60)

        val result1 = ReportResult(
            documentId = "doc-123",
            deliveryStatus = DeliveryStatus.ONLINE_OK,
            deliveryError = null,
            deliveryPayload = payload1
        )
        val result1Copy = result1.copy()
        val resultSame = ReportResult("doc-123", DeliveryStatus.ONLINE_OK, null, payload2)
        val resultDiffDoc = result1.copy(documentId = "doc-456")
        val resultDiffStatus = result1.copy(deliveryStatus = DeliveryStatus.OFFLINE_QUEUED)
        val resultDiffError = result1.copy(deliveryError = "Some error")
        val resultDiffPayload = result1.copy(deliveryPayload = payload3)
        val resultNullPayload = result1.copy(deliveryPayload = null)

        // Same instance
        assertEquals(result1, result1)

        // Equal values
        assertEquals(result1, result1Copy)
        assertEquals(result1, resultSame)
        assertEquals(result1.hashCode(), resultSame.hashCode())

        // Not equal checks
        assertNotEquals(result1, Any())
        assertFalse(result1.equals(null))
        assertNotEquals(result1, resultDiffDoc)
        assertNotEquals(result1, resultDiffStatus)
        assertNotEquals(result1, resultDiffError)
        assertNotEquals(result1, resultDiffPayload)
        assertNotEquals(result1, resultNullPayload)
        assertNotEquals(resultNullPayload, result1)

        // Verify toString is covered
        assertTrue(result1.toString().contains("documentId=doc-123"))
        assertTrue(resultNullPayload.toString().contains("deliveryPayload=null"))
    }
}
