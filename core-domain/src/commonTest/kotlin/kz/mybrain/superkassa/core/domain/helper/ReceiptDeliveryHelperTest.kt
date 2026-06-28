package kz.mybrain.superkassa.core.domain.helper

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kz.mybrain.superkassa.core.domain.model.kkm.FiscalDocumentSnapshot
import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptRequest
import kz.mybrain.superkassa.core.domain.model.settings.*
import kz.mybrain.superkassa.core.domain.port.DeliveryPort
import kz.mybrain.superkassa.core.domain.port.DocumentConvertPort
import kz.mybrain.superkassa.core.domain.port.ReceiptRenderPort
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReceiptDeliveryHelperTest {

    private val storage = mockk<StoragePort>()
    private val delivery = mockk<DeliveryPort>(relaxed = true)
    private val documentConvertPort = mockk<DocumentConvertPort>()
    private val receiptRenderPort = mockk<ReceiptRenderPort>()

    private val mockMode = CoreMode.SERVER
    private val mockStorage = StorageSettings(engine = "sqlite", jdbcUrl = "jdbc:sqlite::memory:")

    @Test
    fun testDeliverReceiptNoSettings() {
        val coreSettings = CoreSettings(mode = mockMode, storage = mockStorage, delivery = null)
        val helper = ReceiptDeliveryHelper(storage, delivery, coreSettings, documentConvertPort, receiptRenderPort)

        every { storage.findKkm("kkm-1") } returns KkmInfo(id = "kkm-1", createdAt = 0L, updatedAt = 0L, mode = "ACTIVE", state = "ACTIVE")
        every { receiptRenderPort.renderHtml(any(), any(), any()) } returns "<html></html>"

        val receipt = mockk<ReceiptRequest>()
        val snapshot = mockk<FiscalDocumentSnapshot>()

        helper.deliverReceipt(
            kkmId = "kkm-1",
            documentId = "doc-1",
            receipt = receipt,
            docSnapshot = snapshot,
            receiptUrl = null,
            responseBin = byteArrayOf(1, 2, 3)
        )

        verify {
            delivery.deliver(match { it.channel == "PRINT" && it.payloadBytes?.contentEquals(byteArrayOf(1, 2, 3)) == true })
        }
    }

    @Test
    fun testDeliverReceiptWithPrintSettings() {
        val coreSettings = CoreSettings(
            mode = mockMode,
            storage = mockStorage,
            delivery = DeliverySettings(
                print = PrintDeliverySettings(
                    enabled = true,
                    paperWidthMm = 80,
                    connection = PrintConnectionSettings(host = "localhost", port = 9100)
                )
            )
        )
        val helper = ReceiptDeliveryHelper(storage, delivery, coreSettings, documentConvertPort, receiptRenderPort)

        every { storage.findKkm("kkm-1") } returns KkmInfo(id = "kkm-1", createdAt = 0L, updatedAt = 0L, mode = "ACTIVE", state = "ACTIVE")
        every { receiptRenderPort.renderHtml(any(), any(), any()) } returns "<html></html>"
        every { documentConvertPort.htmlToEscPos("<html></html>", 80) } returns byteArrayOf(4, 5)

        val receipt = mockk<ReceiptRequest>()
        val snapshot = mockk<FiscalDocumentSnapshot>()

        helper.deliverReceipt(
            kkmId = "kkm-1",
            documentId = "doc-1",
            receipt = receipt,
            docSnapshot = snapshot,
            receiptUrl = null,
            responseBin = null
        )

        verify {
            delivery.deliver(match { it.channel == "PRINT" && it.payloadBytes?.contentEquals(byteArrayOf(4, 5)) == true })
        }
    }

    @Test
    fun testDeliverReceiptWithChannels() {
        val coreSettings = CoreSettings(
            mode = mockMode,
            storage = mockStorage,
            delivery = DeliverySettings(
                channels = listOf(
                    DeliveryChannelSettings(
                        channel = "EMAIL",
                        enabled = true,
                        destination = "test@example.com",
                        payloadType = "LINK"
                    ),
                    DeliveryChannelSettings(
                        channel = "SMS",
                        enabled = true,
                        destination = "+12345",
                        payloadType = "DOCUMENT",
                        documentFormat = "PDF"
                    )
                )
            )
        )
        val helper = ReceiptDeliveryHelper(storage, delivery, coreSettings, documentConvertPort, receiptRenderPort)

        every { storage.findKkm("kkm-1") } returns KkmInfo(id = "kkm-1", createdAt = 0L, updatedAt = 0L, mode = "ACTIVE", state = "ACTIVE")
        every { receiptRenderPort.renderHtml(any(), any(), any()) } returns "<html></html>"
        every { documentConvertPort.htmlToPdf("<html></html>") } returns byteArrayOf(9, 9)

        val receipt = mockk<ReceiptRequest>()
        val snapshot = mockk<FiscalDocumentSnapshot>()

        helper.deliverReceipt(
            kkmId = "kkm-1",
            documentId = "doc-1",
            receipt = receipt,
            docSnapshot = snapshot,
            receiptUrl = "http://receipt.url",
            responseBin = null
        )

        verify {
            delivery.deliver(match { it.channel == "EMAIL" && it.payloadUrl == "http://receipt.url" })
            delivery.deliver(match { it.channel == "SMS" && it.payloadBytes?.contentEquals(byteArrayOf(9, 9)) == true })
        }
    }

    @Test
    fun testDeliverReceiptWithAlternativePaperWidths() {
        // Проверка ширины бумаги 48 мм и дефолтной 58 мм
        val coreSettings48 = CoreSettings(
            mode = mockMode,
            storage = mockStorage,
            delivery = DeliverySettings(
                print = PrintDeliverySettings(
                    enabled = true,
                    paperWidthMm = 48,
                    connection = PrintConnectionSettings(host = "localhost", port = 9100)
                )
            )
        )
        val helper48 = ReceiptDeliveryHelper(storage, delivery, coreSettings48, documentConvertPort, receiptRenderPort)

        every { storage.findKkm("kkm-1") } returns KkmInfo(id = "kkm-1", createdAt = 0L, updatedAt = 0L, mode = "ACTIVE", state = "ACTIVE")
        every { receiptRenderPort.renderHtml(any(), any(), any()) } returns "<html></html>"
        every { documentConvertPort.htmlToEscPos("<html></html>", 48) } returns byteArrayOf(4, 8)

        val receipt = mockk<ReceiptRequest>()
        val snapshot = mockk<FiscalDocumentSnapshot>()

        helper48.deliverReceipt("kkm-1", "doc-1", receipt, snapshot, null, null)

        verify {
            delivery.deliver(match { it.channel == "PRINT" && it.payloadBytes?.contentEquals(byteArrayOf(4, 8)) == true })
        }

        val coreSettingsDefault = CoreSettings(
            mode = mockMode,
            storage = mockStorage,
            delivery = DeliverySettings(
                print = PrintDeliverySettings(
                    enabled = true,
                    paperWidthMm = 58, // or any other value -> fallback to 58
                    connection = PrintConnectionSettings(host = "localhost", port = 9100)
                )
            )
        )
        val helperDefault = ReceiptDeliveryHelper(storage, delivery, coreSettingsDefault, documentConvertPort, receiptRenderPort)
        every { documentConvertPort.htmlToEscPos("<html></html>", 58) } returns byteArrayOf(5, 8)

        helperDefault.deliverReceipt("kkm-1", "doc-1", receipt, snapshot, null, null)

        verify {
            delivery.deliver(match { it.channel == "PRINT" && it.payloadBytes?.contentEquals(byteArrayOf(5, 8)) == true })
        }
    }

    @Test
    fun testDeliverReceiptWithBothAndImagePayloads() {
        // Проверка payloadType = BOTH, формата IMAGE и дефолтного fallback
        val coreSettings = CoreSettings(
            mode = mockMode,
            storage = mockStorage,
            delivery = DeliverySettings(
                channels = listOf(
                    DeliveryChannelSettings(
                        channel = "TELEGRAM",
                        enabled = true,
                        destination = "@user",
                        payloadType = "BOTH",
                        documentFormat = "IMAGE"
                    ),
                    DeliveryChannelSettings(
                        channel = "WHATSAPP",
                        enabled = true,
                        destination = "whatsapp-id",
                        payloadType = "DOCUMENT",
                        documentFormat = "UTF8" // fallback to UTF-8 html.toByteArray
                    )
                )
            )
        )
        val helper = ReceiptDeliveryHelper(storage, delivery, coreSettings, documentConvertPort, receiptRenderPort)

        every { storage.findKkm("kkm-1") } returns KkmInfo(id = "kkm-1", createdAt = 0L, updatedAt = 0L, mode = "ACTIVE", state = "ACTIVE")
        every { receiptRenderPort.renderHtml(any(), any(), any()) } returns "<html></html>"
        every { documentConvertPort.htmlToImage("<html></html>") } returns byteArrayOf(10, 10)

        val receipt = mockk<ReceiptRequest>()
        val snapshot = mockk<FiscalDocumentSnapshot>()

        helper.deliverReceipt("kkm-1", "doc-1", receipt, snapshot, "http://receipt.url", null)

        verify {
            delivery.deliver(match { it.channel == "TELEGRAM" && it.payloadUrl == "http://receipt.url" })
            delivery.deliver(match { it.channel == "TELEGRAM" && it.payloadBytes?.contentEquals(byteArrayOf(10, 10)) == true })
            delivery.deliver(match { it.channel == "WHATSAPP" && it.payloadBytes?.contentEquals("<html></html>".toByteArray(Charsets.UTF_8)) == true })
        }
    }

    @Test
    fun testRetryDelivery() {
        val coreSettings = CoreSettings(
            mode = mockMode,
            storage = mockStorage,
            delivery = DeliverySettings(
                print = PrintDeliverySettings(
                    enabled = true,
                    paperWidthMm = 80,
                    connection = PrintConnectionSettings(host = "localhost", port = 9100)
                ),
                channels = listOf(
                    DeliveryChannelSettings(
                        channel = "TELEGRAM",
                        enabled = true,
                        destination = "@user",
                        payloadType = "DOCUMENT",
                        documentFormat = "PDF"
                    ),
                    DeliveryChannelSettings(
                        channel = "EMAIL",
                        enabled = true,
                        destination = "test@example.com",
                        payloadType = "LINK"
                    ),
                    DeliveryChannelSettings(
                        channel = "SMS",
                        enabled = true,
                        destination = null, // missing destination -> should fail or skip
                        payloadType = "DOCUMENT",
                        documentFormat = "PDF"
                    )
                )
            )
        )
        val helper = ReceiptDeliveryHelper(storage, delivery, coreSettings, documentConvertPort, receiptRenderPort)

        every { storage.findKkm("kkm-1") } returns KkmInfo(id = "kkm-1", createdAt = 0L, updatedAt = 0L, mode = "ACTIVE", state = "ACTIVE")
        every { receiptRenderPort.renderHtml(any(), any(), any()) } returns "<html></html>"
        every { documentConvertPort.htmlToEscPos("<html></html>", 80) } returns byteArrayOf(4, 5)
        every { documentConvertPort.htmlToPdf("<html></html>") } returns byteArrayOf(9, 9)

        every { delivery.deliver(match { it.channel == "PRINT" }) } returns true
        every { delivery.deliver(match { it.channel == "TELEGRAM" }) } returns true

        val receipt = mockk<ReceiptRequest>()
        val snapshot = mockk<FiscalDocumentSnapshot>()

        val results = helper.retryDelivery("kkm-1", "doc-1", receipt, snapshot)

        assertEquals(4, results.size)
        assertEquals("PRINT" to true, results[0])
        assertEquals("TELEGRAM" to true, results[1])
        assertEquals("EMAIL" to false, results[2]) // LINK payload returns false
        assertEquals("SMS" to false, results[3]) // null destination returns false
    }

    @Test
    fun testRetryDeliveryNoSettings() {
        val coreSettings = CoreSettings(mode = mockMode, storage = mockStorage, delivery = null)
        val helper = ReceiptDeliveryHelper(storage, delivery, coreSettings, documentConvertPort, receiptRenderPort)

        every { storage.findKkm("kkm-1") } returns KkmInfo(id = "kkm-1", createdAt = 0L, updatedAt = 0L, mode = "ACTIVE", state = "ACTIVE")
        every { receiptRenderPort.renderHtml(any(), any(), any()) } returns "<html></html>"

        val receipt = mockk<ReceiptRequest>()
        val snapshot = mockk<FiscalDocumentSnapshot>()

        val results = helper.retryDelivery("kkm-1", "doc-1", receipt, snapshot)
        assertTrue(results.isEmpty())
    }
}
