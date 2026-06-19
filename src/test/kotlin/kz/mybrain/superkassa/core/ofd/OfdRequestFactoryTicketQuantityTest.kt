package kz.mybrain.superkassa.core.ofd

import kz.mybrain.superkassa.core.data.ofd.OfdRequestFactory
import kz.mybrain.superkassa.core.domain.model.Money
import kz.mybrain.superkassa.core.domain.model.PaymentType
import kz.mybrain.superkassa.core.domain.model.ReceiptItem
import kz.mybrain.superkassa.core.domain.model.ReceiptOperationType
import kz.mybrain.superkassa.core.domain.model.ReceiptPayment
import kz.mybrain.superkassa.core.domain.model.ReceiptRequest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.test.Test
import kotlin.test.assertEquals

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
                items = listOf(ReceiptItem("Item", "001", 5, Money(1000, 0), Money(5000, 0))),
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
}
