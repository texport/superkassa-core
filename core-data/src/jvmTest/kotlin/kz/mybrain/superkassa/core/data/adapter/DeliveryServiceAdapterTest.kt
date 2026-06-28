package kz.mybrain.superkassa.core.data.adapter

import io.github.texport.superkassa.delivery.application.service.DeliveryService
import io.github.texport.superkassa.delivery.domain.model.DeliveryChannel
import io.github.texport.superkassa.delivery.domain.model.DeliveryResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kz.mybrain.superkassa.core.domain.model.delivery.DeliveryRequest as CoreDeliveryRequest

class DeliveryServiceAdapterTest {
    private val deliveryService: DeliveryService = mockk()
    private val adapter = DeliveryServiceAdapter(deliveryService)

    @Test
    fun testDeliverSuccess() {
        val coreRequest = CoreDeliveryRequest(
            kkmId = "kkm-1",
            documentId = "doc-1",
            channel = "EMAIL",
            destination = "test@example.com",
            payloadType = "HTML",
            payloadUrl = "http://example.com/receipt",
            payloadBytes = byteArrayOf(1, 2, 3)
        )

        every { deliveryService.deliver(any()) } returns DeliveryResult(true)

        val result = adapter.deliver(coreRequest)
        assertTrue(result)

        verify {
            deliveryService.deliver(
                withArg {
                    assertEquals("kkm-1", it.cashboxId)
                    assertEquals("doc-1", it.documentId)
                    assertEquals(DeliveryChannel.EMAIL, it.channel)
                    assertEquals("test@example.com", it.destination)
                }
            )
        }
    }

    @Test
    fun testDeliverUnknownChannelUsesPrintFallback() {
        val coreRequest = CoreDeliveryRequest(
            kkmId = "kkm-1",
            documentId = "doc-1",
            channel = "UNKNOWN_CHANNEL",
            destination = "test@example.com",
            payloadType = "HTML",
            payloadUrl = "http://example.com"
        )

        every { deliveryService.deliver(any()) } returns DeliveryResult(true)

        val result = adapter.deliver(coreRequest)
        assertTrue(result)

        verify {
            deliveryService.deliver(
                withArg {
                    assertEquals(DeliveryChannel.PRINT, it.channel)
                }
            )
        }
    }

    @Test
    fun testDeliverFailure() {
        val coreRequest = CoreDeliveryRequest(
            kkmId = "kkm-1",
            documentId = "doc-1",
            channel = "EMAIL",
            destination = "test@example.com",
            payloadType = "HTML",
            payloadUrl = "http://example.com"
        )

        every { deliveryService.deliver(any()) } returns DeliveryResult(false)

        val result = adapter.deliver(coreRequest)
        assertFalse(result)
    }

    @Test
    fun testDeliverThrowsException() {
        val coreRequest = CoreDeliveryRequest(
            kkmId = "kkm-1",
            documentId = "doc-1",
            channel = "EMAIL",
            destination = "test@example.com",
            payloadType = "HTML",
            payloadUrl = "http://example.com"
        )

        every { deliveryService.deliver(any()) } throws RuntimeException("Service failure")

        val result = adapter.deliver(coreRequest)
        assertFalse(result)
    }
}
