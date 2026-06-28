package kz.mybrain.superkassa.core.domain.model.delivery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class DeliveryModelTest {

    @Test
    fun testDeliveryRequestValidation() {
        // Validation fails if both payloadUrl and payloadBytes are null
        assertFailsWith<IllegalArgumentException> {
            DeliveryRequest(
                kkmId = "kkm-1",
                documentId = "doc-1",
                channel = "SMS",
                destination = "+77011112233",
                payloadType = "LINK",
                payloadUrl = null,
                payloadBytes = null
            )
        }

        // Validation passes if payloadUrl is set
        val reqUrl = DeliveryRequest(
            kkmId = "kkm-1",
            documentId = "doc-1",
            channel = "SMS",
            destination = "+77011112233",
            payloadType = "LINK",
            payloadUrl = "https://example.com/receipt/1",
            payloadBytes = null
        )
        assertEquals("https://example.com/receipt/1", reqUrl.payloadUrl)

        // Validation passes if payloadBytes is set
        val reqBytes = DeliveryRequest(
            kkmId = "kkm-1",
            documentId = "doc-1",
            channel = "PRINT",
            destination = null,
            payloadType = "ESC_POS",
            payloadUrl = null,
            payloadBytes = byteArrayOf(1, 2, 3)
        )
        assertTrue(byteArrayOf(1, 2, 3).contentEquals(reqBytes.payloadBytes))
    }

    @Test
    fun testDeliveryRequestEqualsAndHashCode() {
        val bytes1 = byteArrayOf(1, 2, 3)
        val bytes2 = byteArrayOf(1, 2, 3)
        val bytes3 = byteArrayOf(4, 5, 6)

        val req1 = DeliveryRequest("k1", "d1", "c1", "dest1", "t1", "u1", bytes1)
        val req1Copy = req1.copy()
        val reqSame = DeliveryRequest("k1", "d1", "c1", "dest1", "t1", "u1", bytes2)
        val reqDiffKkm = req1.copy(kkmId = "k2")
        val reqDiffDoc = req1.copy(documentId = "d2")
        val reqDiffChannel = req1.copy(channel = "c2")
        val reqDiffDest = req1.copy(destination = "dest2")
        val reqDiffType = req1.copy(payloadType = "t2")
        val reqDiffUrl = req1.copy(payloadUrl = "u2")
        val reqDiffBytes = req1.copy(payloadBytes = bytes3)
        val reqNullBytes = req1.copy(payloadBytes = null, payloadUrl = "u1")
        val reqNullDest = req1.copy(destination = null)

        // Same instance
        assertEquals(req1, req1)
        
        // Equal values
        assertEquals(req1, req1Copy)
        assertEquals(req1, reqSame)
        assertEquals(req1.hashCode(), reqSame.hashCode())
        
        // Not equal checks
        assertNotEquals(req1, Any())
        assertFalse(req1.equals(null))
        assertNotEquals(req1, reqDiffKkm)
        assertNotEquals(req1, reqDiffDoc)
        assertNotEquals(req1, reqDiffChannel)
        assertNotEquals(req1, reqDiffDest)
        assertNotEquals(req1, reqDiffType)
        assertNotEquals(req1, reqDiffUrl)
        assertNotEquals(req1, reqDiffBytes)
        assertNotEquals(req1, reqNullBytes)
        assertNotEquals(reqNullBytes, req1)
        assertNotEquals(reqNullDest, req1)

        // Verify toString is covered
        assertTrue(req1.toString().contains("kkmId=k1"))
        assertTrue(reqNullDest.toString().contains("destination=null"))
    }

    @Test
    fun testDeliveryStatusEnum() {
        for (status in DeliveryStatus.entries) {
            val value = DeliveryStatus.valueOf(status.name)
            assertEquals(status, value)
        }
    }
}
