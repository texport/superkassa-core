package io.github.texport.superkassa.delivery.impl

import io.github.texport.superkassa.delivery.api.DeliveryServiceApi
import io.github.texport.superkassa.delivery.api.createDeliveryServiceApi
import io.github.texport.superkassa.delivery.api.model.DeliveryChannel
import io.github.texport.superkassa.delivery.api.model.DeliveryRequest
import io.github.texport.superkassa.delivery.api.model.DeliveryResult
import io.github.texport.superkassa.delivery.api.port.DeliveryPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeliveryServiceApiImplTest {

    @Test
    fun testSuccessfulDelivery() {
        val mockAdapter = object : DeliveryPort {
            override val channel = DeliveryChannel.EMAIL
            override fun send(request: DeliveryRequest): DeliveryResult {
                return DeliveryResult(true, "Sent successfully")
            }
        }

        val service = createDeliveryServiceApi(listOf(mockAdapter))
        val request = DeliveryRequest(
            cashboxId = "cashbox-1",
            documentId = "doc-1",
            channel = DeliveryChannel.EMAIL,
            destination = "test@example.com",
            payloadUrl = "https://example.com/receipt"
        )

        val result = service.deliver(request)
        assertTrue(result.ok)
        assertEquals("Sent successfully", result.message)
    }

    @Test
    fun testDeliveryNoAdapter() {
        val service = createDeliveryServiceApi(emptyList())
        val request = DeliveryRequest(
            cashboxId = "cashbox-1",
            documentId = "doc-1",
            channel = DeliveryChannel.SMS,
            destination = "+123456789",
            payloadBytes = byteArrayOf(1, 2, 3)
        )

        val result = service.deliver(request)
        assertFalse(result.ok)
        assertTrue(result.message!!.contains("No adapter for channel SMS"))
    }

    @Test
    fun testDeliveryFailedByAdapter() {
        val mockAdapter = object : DeliveryPort {
            override val channel = DeliveryChannel.WHATSAPP
            override fun send(request: DeliveryRequest): DeliveryResult {
                return DeliveryResult(false, "WhatsApp API error")
            }
        }

        val service = createDeliveryServiceApi(listOf(mockAdapter))
        val request = DeliveryRequest(
            cashboxId = "cashbox-1",
            documentId = "doc-2",
            channel = DeliveryChannel.WHATSAPP,
            destination = "+123456789",
            payloadUrl = "https://example.com/receipt"
        )

        val result = service.deliver(request)
        assertFalse(result.ok)
        assertEquals("WhatsApp API error", result.message)
    }

    @Test
    fun testDataClassCoverage() {
        val nullAny: Any? = null
        val req1 = DeliveryRequest("c1", "d1", DeliveryChannel.PRINT, "dest1", "url1")
        val req2 = DeliveryRequest("c1", "d1", DeliveryChannel.PRINT, "dest1", "url1")
        val req3 = DeliveryRequest("c2", "d1", DeliveryChannel.PRINT, "dest1", "url1")

        assertEquals(req1, req2)
        assertEquals(req1.hashCode(), req2.hashCode())
        assertTrue(req1.toString().contains("c1"))

        // component functions
        assertEquals("c1", req1.component1())
        assertEquals("d1", req1.component2())
        assertEquals(DeliveryChannel.PRINT, req1.component3())
        assertEquals("dest1", req1.component4())
        assertEquals("url1", req1.component5())
        assertEquals(null, req1.component6())

        val res1 = DeliveryResult(true, "msg1")
        val res2 = DeliveryResult(true, "msg1")
        assertEquals(res1, res2)
        assertEquals(res1.hashCode(), res2.hashCode())
        assertTrue(res1.toString().contains("msg1"))
        assertEquals(true, res1.component1())
        assertEquals("msg1", res1.component2())

        val copyReq = req1.copy(cashboxId = "c-copy")
        assertEquals("c-copy", copyReq.cashboxId)

        val copyRes = res1.copy(ok = false)
        assertFalse(copyRes.ok)

        // Enum values coverage
        assertTrue(DeliveryChannel.values().contains(DeliveryChannel.TELEGRAM))
        assertEquals(DeliveryChannel.TELEGRAM, DeliveryChannel.valueOf("TELEGRAM"))

        // Add additional tests to cover all generated branches in equals, toString, hashCode
        val reqWithBytes = DeliveryRequest("c1", "d1", DeliveryChannel.PRINT, "dest1", payloadBytes = byteArrayOf(1))
        val reqWithBytes2 = DeliveryRequest("c1", "d1", DeliveryChannel.PRINT, "dest1", payloadBytes = byteArrayOf(1))
        val reqWithBytesDiff = DeliveryRequest("c1", "d1", DeliveryChannel.PRINT, "dest1", payloadBytes = byteArrayOf(2))

        assertEquals(reqWithBytes, reqWithBytes2)
        assertFalse(reqWithBytes == reqWithBytesDiff)
        assertFalse(req1 == reqWithBytes)
        assertFalse(req1 == req3)
        assertFalse(req1.equals(nullAny))
        assertFalse(req1.equals("different type"))
        val nullReq: DeliveryRequest? = null
        assertFalse(req1.equals(nullReq))
        assertFalse(req1 == nullReq)

        // Hash code differences
        assertTrue(req1.hashCode() != req3.hashCode())
        assertTrue(reqWithBytes.hashCode() != reqWithBytesDiff.hashCode())

        // DeliveryResult equals branches
        val resNoMsg1 = DeliveryResult(true)
        val resNoMsg2 = DeliveryResult(true)
        val resFalseNoMsg = DeliveryResult(false)
        assertEquals(resNoMsg1, resNoMsg2)
        assertFalse(resNoMsg1 == resFalseNoMsg)
        assertFalse(res1 == resNoMsg1)
        assertFalse(res1.equals(nullAny))
        assertFalse(res1.equals("different type"))
        assertTrue(res1.hashCode() != resFalseNoMsg.hashCode())

        // Extra copy coverage
        val reqCopyAll = req1.copy("c2", "d2", DeliveryChannel.SMS, "dest2", "url2", byteArrayOf(5))
        assertEquals("c2", reqCopyAll.cashboxId)
        assertEquals("d2", reqCopyAll.documentId)
        assertEquals(DeliveryChannel.SMS, reqCopyAll.channel)
        assertEquals("dest2", reqCopyAll.destination)
        assertEquals("url2", reqCopyAll.payloadUrl)
        assertTrue(reqCopyAll.payloadBytes!!.contentEquals(byteArrayOf(5)))

        val resCopyAll = res1.copy(false, "msg2")
        assertFalse(resCopyAll.ok)
        assertEquals("msg2", resCopyAll.message)

        // Equals mismatch branches coverage
        assertTrue(req1 === req1)
        assertFalse(req1 == req1.copy(cashboxId = "diff"))
        assertFalse(req1 == req1.copy(documentId = "diff"))
        assertFalse(req1 == req1.copy(channel = DeliveryChannel.EMAIL))
        assertFalse(req1 == req1.copy(destination = "diff"))
        assertFalse(req1 == req1.copy(payloadUrl = "diff"))

        val reqNullBytes = req1.copy(payloadBytes = null)
        val reqNonNullBytes = req1.copy(payloadBytes = byteArrayOf(1))
        val reqNonNullBytes2 = req1.copy(payloadBytes = byteArrayOf(2))
        assertFalse(reqNullBytes == reqNonNullBytes)
        assertFalse(reqNonNullBytes == reqNullBytes)
        assertFalse(reqNonNullBytes == reqNonNullBytes2)
        assertFalse(req1.equals(object : Any() {}))

        // Mismatch on destination null vs non-null
        val reqNullDest = req1.copy(destination = null)
        val reqNullDest2 = req1.copy(destination = null)
        assertEquals(reqNullDest, reqNullDest2)
        assertEquals(reqNullDest.hashCode(), reqNullDest2.hashCode())
        assertFalse(req1 == reqNullDest)
        assertFalse(reqNullDest == req1)
        
        // Explicitly test this === other branch
        assertTrue(req1.equals(req1))

        // Mismatch on payloadUrl null vs non-null
        val reqNullUrl = req1.copy(payloadUrl = null, payloadBytes = byteArrayOf(1))
        val reqNonNullUrl = req1.copy(payloadUrl = "some", payloadBytes = byteArrayOf(1))
        assertFalse(reqNullUrl == reqNonNullUrl)
        assertFalse(reqNonNullUrl == reqNullUrl)
    }
}
