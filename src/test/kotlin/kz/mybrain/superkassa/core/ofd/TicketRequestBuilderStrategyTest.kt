package kz.mybrain.superkassa.core.ofd

import kz.mybrain.superkassa.core.data.adapter.TicketRequestBuilderStrategy
import kz.mybrain.superkassa.core.data.ofd.OfdConfig
import kz.mybrain.superkassa.core.domain.model.FiscalDocumentSnapshot
import kz.mybrain.superkassa.core.domain.model.Money
import kz.mybrain.superkassa.core.domain.model.OfdCommandRequest
import kz.mybrain.superkassa.core.domain.model.OfdCommandType
import kz.mybrain.superkassa.core.domain.model.OfdServiceInfo
import kz.mybrain.superkassa.core.domain.model.PaymentType
import kz.mybrain.superkassa.core.domain.model.ReceiptItem
import kz.mybrain.superkassa.core.domain.model.ReceiptOperationType
import kz.mybrain.superkassa.core.domain.model.ReceiptPayment
import kz.mybrain.superkassa.core.domain.model.ReceiptRequest
import kz.mybrain.superkassa.core.support.TestStoragePort
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TicketRequestBuilderStrategyTest {

    @Test
    fun `build uses persisted receipt payload for ticket request`() {
        val storage = TestStoragePort()
        val receipt =
            ReceiptRequest(
                kkmId = "kkm-1",
                pin = "1111",
                operation = ReceiptOperationType.BUY_RETURN,
                items =
                    listOf(
                        ReceiptItem(
                            name = "Milk",
                            sectionCode = "001",
                            quantity = 1,
                            price = Money(777, 0),
                            sum = Money(777, 0)
                        )
                    ),
                payments = listOf(ReceiptPayment(PaymentType.CARD, Money(777, 0))),
                total = Money(777, 0),
                idempotencyKey = "idem-1"
            )

        storage.addFiscalDocument(
            document =
                FiscalDocumentSnapshot(
                    id = "doc-1",
                    cashboxId = "kkm-1",
                    shiftId = "shift-1",
                    docType = "CHECK",
                    docNo = 1L,
                    shiftNo = 1L,
                    createdAt = 1_700_000_000_000L,
                    totalAmount = 777L,
                    currency = "KZT",
                    fiscalSign = null,
                    autonomousSign = null,
                    isAutonomous = false,
                    ofdStatus = "PENDING",
                    deliveredAt = null
                ),
            receipt = receipt
        )

        val command =
            OfdCommandRequest(
                kkmId = "kkm-1",
                commandType = OfdCommandType.TICKET,
                payloadRef = "doc-1",
                ofdProviderId = "kazakhtelecom",
                ofdEnvironmentId = "test",
                deviceId = 11L,
                token = 22L,
                reqNum = 33,
                registrationNumber = "RN-1",
                factoryNumber = "FN-1",
                ofdSystemId = "SYS-1",
                serviceInfo =
                    OfdServiceInfo(
                        orgTitle = "Org",
                        orgAddress = "Addr",
                        orgAddressKz = "Addr KZ",
                        orgInn = "123456789012",
                        orgOkved = "47301",
                        geoLatitude = 1,
                        geoLongitude = 1,
                        geoSource = "GPS"
                    ),
                offlineBeginMillis = 1_700_000_000_000L,
                offlineEndMillis = 1_700_000_000_100L
            )

        val strategy = TicketRequestBuilderStrategy(storage)
        val json = strategy.build(command, OfdConfig(protocolVersion = "203"))
        assertNotNull(json)

        val payload = json["payload"]!!.jsonObject
        val ticket = payload["ticket"]!!.jsonObject
        assertEquals("OPERATION_BUY_RETURN", ticket["operation"]!!.jsonPrimitive.content)

        val total =
            ticket["amounts"]!!
                .jsonObject["total"]!!
                .jsonObject["bills"]!!
                .jsonPrimitive.long
        assertEquals(777L, total)
    }
}
