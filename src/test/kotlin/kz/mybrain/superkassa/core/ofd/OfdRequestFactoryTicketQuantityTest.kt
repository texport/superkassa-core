package kz.mybrain.superkassa.core.ofd

import kz.mybrain.superkassa.core.data.ofd.OfdCodecService
import kz.mybrain.superkassa.core.data.ofd.OfdRequestFactory
import kz.mybrain.superkassa.core.domain.model.Money
import kz.mybrain.superkassa.core.domain.model.PaymentType
import kz.mybrain.superkassa.core.domain.model.ReceiptItem
import kz.mybrain.superkassa.core.domain.model.ReceiptOperationType
import kz.mybrain.superkassa.core.domain.model.ReceiptPayment
import kz.mybrain.superkassa.core.domain.model.ReceiptRequest
import kz.mybrain.superkassa.core.domain.model.TaxRegime
import kz.mybrain.superkassa.core.domain.model.VatGroup
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class OfdRequestFactoryTicketQuantityTest {

    @Test
    fun `buildTicketRequest sends commodity quantity in thousandths`() {
        val request =
            ReceiptRequest(
                kkmId = "kkm-1",
                pin = "1111",
                operation = ReceiptOperationType.SELL,
                items = listOf(ReceiptItem("Item", "001", 2, Money(1000, 0), Money(2000, 0))),
                payments = listOf(ReceiptPayment(PaymentType.CASH, Money(2000, 0))),
                total = Money(2000, 0),
                idempotencyKey = "idem-1"
            )

        val json =
            OfdRequestFactory.buildTicketRequest(
                ofdId = "kazakhtelecom",
                protocolVersion = "203",
                deviceId = 1L,
                token = 123L,
                reqNum = 1,
                request = request
            )

        val quantity =
            json["payload"]!!
                .jsonObject["ticket"]!!
                .jsonObject["items"]!!
                .jsonArray.first()
                .jsonObject["commodity"]!!
                .jsonObject["quantity"]!!
                .jsonPrimitive.long

        assertEquals(2_000L, quantity)
    }

    @Test
    fun `buildTicketRequest sends storno commodity quantity in thousandths`() {
        val request =
            ReceiptRequest(
                kkmId = "kkm-1",
                pin = "1111",
                operation = ReceiptOperationType.SELL_RETURN,
                items = listOf(ReceiptItem("Item", "001", 5, Money(1000, 0), Money(5000, 0), isStorno = true)),
                payments = listOf(ReceiptPayment(PaymentType.CASH, Money(5000, 0))),
                total = Money(5000, 0),
                idempotencyKey = "idem-2"
            )

        val json =
            OfdRequestFactory.buildTicketRequest(
                ofdId = "kazakhtelecom",
                protocolVersion = "203",
                deviceId = 1L,
                token = 123L,
                reqNum = 1,
                request = request
            )

        val quantity =
            json["payload"]!!
                .jsonObject["ticket"]!!
                .jsonObject["items"]!!
                .jsonArray.first()
                .jsonObject["stornoCommodity"]!!
                .jsonObject["quantity"]!!
                .jsonPrimitive.long

        assertEquals(5_000L, quantity)
    }

    @Test
    fun `buildTicketRequest sends listExciseStamp correctly`() {
        val request =
            ReceiptRequest(
                kkmId = "kkm-1",
                pin = "1111",
                operation = ReceiptOperationType.SELL,
                items = listOf(
                    ReceiptItem(
                        name = "Item",
                        sectionCode = "001",
                        quantity = 1,
                        price = Money(1000, 0),
                        sum = Money(1000, 0),
                        listExciseStamp = listOf("STAMP-1", "STAMP-2")
                    )
                ),
                payments = listOf(ReceiptPayment(PaymentType.CASH, Money(1000, 0))),
                total = Money(1000, 0),
                idempotencyKey = "idem-3"
            )

        val json =
            OfdRequestFactory.buildTicketRequest(
                ofdId = "kazakhtelecom",
                protocolVersion = "203",
                deviceId = 1L,
                token = 123L,
                reqNum = 1,
                request = request
            )

        val stamps =
            json["payload"]!!
                .jsonObject["ticket"]!!
                .jsonObject["items"]!!
                .jsonArray.first()
                .jsonObject["commodity"]!!
                .jsonObject["listExciseStamp"]!!
                .jsonArray.map { it.jsonPrimitive.content }

        assertEquals(listOf("STAMP-1", "STAMP-2"), stamps)
    }

    @Test
    fun `buildTicketRequest can be encoded by OfdCodecService when taxes are present`() {
        val now = System.currentTimeMillis()
        val serviceInfo = kz.mybrain.superkassa.core.domain.model.OfdServiceInfo(
            orgTitle = "Test Org",
            orgAddress = "Test Address",
            orgAddressKz = "Test Address KZ",
            orgInn = "123456789012",
            orgOkved = "47301",
            geoLatitude = 1,
            geoLongitude = 1,
            geoSource = "TEST"
        )
        val serviceBlock = OfdRequestFactory.buildServicePayload(
            serviceInfo = serviceInfo,
            registrationNumber = "RN-1",
            factoryNumber = "FN-1",
            systemId = "SYS-1",
            offlineBeginMillis = now - 1_000,
            offlineEndMillis = now
        )

        val request = ReceiptRequest(
            kkmId = "kkm-1",
            pin = "1111",
            operation = ReceiptOperationType.SELL,
            taxRegime = TaxRegime.VAT_PAYER,
            defaultVatGroup = VatGroup.VAT_16,
            items = listOf(
                ReceiptItem(
                    name = "Item with VAT 16",
                    sectionCode = "001",
                    quantity = 1,
                    price = Money(1000, 0),
                    sum = Money(1000, 0),
                    vatGroup = VatGroup.VAT_16
                )
            ),
            payments = listOf(ReceiptPayment(PaymentType.CASH, Money(1000, 0))),
            total = Money(1000, 0),
            idempotencyKey = "idem-4"
        )

        val json = OfdRequestFactory.buildTicketRequest(
            ofdId = "kazakhtelecom",
            protocolVersion = "203",
            deviceId = 1L,
            token = 123L,
            reqNum = 1,
            request = request,
            serviceBlock = serviceBlock
        )

        val codec = OfdCodecService()
        val bytes = codec.encode(json)
        assertNotNull(bytes)
        assertTrue(bytes.isNotEmpty())
    }

}

