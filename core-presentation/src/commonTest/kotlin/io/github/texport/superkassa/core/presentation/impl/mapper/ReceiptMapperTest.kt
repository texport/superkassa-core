package io.github.texport.superkassa.core.presentation.impl.mapper

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType
import io.github.texport.superkassa.core.presentation.api.model.receipt.ParentTicketRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptItemRequest
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptDomainType
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptDomainRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptPaymentRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ReceiptMapperTest {

    @Test
    fun `toItemInput maps basic fields correctly`() {
        val dto = ReceiptItemRequest(
            name = "Test Item",
            price = Decimal.parse("150.0"),
            quantity = Decimal.parse("2.0"),
            barcode = "12345678",
            vatGroup = "VAT_16",
            measureUnitCode = "796"
        )
        val item = ReceiptMapper.toItemInput(dto)
        assertEquals("Test Item", item.name)
        assertEquals(Decimal.parse("150.0"), item.price)
        assertEquals(Decimal.parse("2.0"), item.quantity)
        assertEquals("12345678", item.barcode)
        assertEquals("VAT_16", item.vatGroup)
        assertEquals("796", item.measureUnitCode)
        assertNull(item.discountPercent)
        assertNull(item.discountSum)
    }

    @Test
    fun `toItemInput carries the kazakh name`() {
        // Казахское наименование печатается на чеке и обязано доехать
        // до домена: в ОФД оно не уходит, и потерять его больше негде.
        val dto = ReceiptItemRequest(
            name = "Баранина",
            nameKk = "Қой еті",
            price = Decimal.parse("407.41"),
            quantity = Decimal.parse("1.0")
        )

        assertEquals("Қой еті", ReceiptMapper.toItemInput(dto).nameKk)
    }

    @Test
    fun `toItemInput leaves the kazakh name empty when it was not given`() {
        val dto = ReceiptItemRequest(name = "Баранина", price = Decimal.parse("407.41"), quantity = Decimal.parse("1.0"))

        assertNull(ReceiptMapper.toItemInput(dto).nameKk)
    }

    @Test
    fun `toPaymentInput maps payment types correctly`() {
        val dto = ReceiptPaymentRequest(type = "CASH", sum = Decimal.parse("500.0"))
        val payment = ReceiptMapper.toPaymentInput(dto)
        assertEquals("CASH", payment.type)
        assertEquals(Decimal.parse("500.0"), payment.sum)
    }

    private fun commandWithDomain(domain: ReceiptDomainRequest?) = ReceiptMapper.toCreateReceiptCommand(
        kkmId = "kkm-1",
        pin = "1234",
        operation = ReceiptOperationType.SELL,
        idempotencyKey = "key-domain",
        items = listOf(ReceiptItemRequest(name = "Item", price = Decimal.parse("100.0"), quantity = Decimal.parse("1.0"))),
        discountPercent = null,
        discountSum = null,
        markupPercent = null,
        markupSum = null,
        payments = listOf(ReceiptPaymentRequest(type = "CASH", sum = Decimal.parse("100.0"))),
        taken = Decimal.parse("100.0"),
        domain = domain
    )

    @Test
    fun `toCreateReceiptCommand maps every domain kind subblock`() {
        assertNull(commandWithDomain(null).domain)

        val taxi = commandWithDomain(
            ReceiptDomainRequest(
                type = "DOMAIN_TAXI",
                taxi = ReceiptDomainRequest.TaxiRequest(carNumber = "A123BC", isOrder = true, currentFee = Decimal.parse("12.34"))
            )
        ).domain
        assertNotNull(taxi)
        assertEquals(ReceiptDomainType.DOMAIN_TAXI, taxi.type)
        assertEquals("A123BC", taxi.taxi?.carNumber)
        assertEquals(Money.fromTenge(Decimal.parse("12.34")), taxi.taxi?.currentFee)

        val services = commandWithDomain(
            ReceiptDomainRequest(
                type = "DOMAIN_SERVICES",
                services = ReceiptDomainRequest.ServicesRequest(accountNumber = "acc-7")
            )
        ).domain
        assertEquals("acc-7", services?.services?.accountNumber)

        val gasOil = commandWithDomain(
            ReceiptDomainRequest(
                type = "DOMAIN_GASOIL",
                gasOil = ReceiptDomainRequest.GasOilRequest(
                    correctionNumber = "c-1",
                    correctionSum = Decimal.parse("5.0"),
                    cardNumber = "card-1"
                )
            )
        ).domain
        assertEquals(Money.fromTenge(Decimal.parse("5.0")), gasOil?.gasOil?.correctionSum)

        val parking = commandWithDomain(
            ReceiptDomainRequest(
                type = "DOMAIN_PARKING",
                parking = ReceiptDomainRequest.ParkingRequest(beginTimeMillis = 1L, endTimeMillis = 2L)
            )
        ).domain
        assertEquals(2L, parking?.parking?.endTimeMillis)
    }

    @Test
    fun `toCreateReceiptCommand rejects unknown domain kind`() {
        assertFailsWith<IllegalArgumentException> {
            commandWithDomain(ReceiptDomainRequest(type = "DOMAIN_UNKNOWN"))
        }
    }

    @Test
    fun `toCreateReceiptCommand maps full sell command correctly`() {
        val itemDto = ReceiptItemRequest(name = "Item 1", price = Decimal.parse("100.0"), quantity = Decimal.parse("2.0"))
        val paymentDto = ReceiptPaymentRequest(type = "CASH", sum = Decimal.parse("200.0"))

        val command = ReceiptMapper.toCreateReceiptCommand(
            kkmId = "kkm-1",
            pin = "1234",
            operation = ReceiptOperationType.SELL,
            idempotencyKey = "key-1",
            items = listOf(itemDto),
            discountPercent = null,
            discountSum = null,
            markupPercent = null,
            markupSum = null,
            payments = listOf(paymentDto),
            taken = Decimal.parse("250.0"),
            vatGroup = "VAT_0"
        )

        assertEquals("kkm-1", command.kkmId)
        assertEquals("1234", command.pin)
        assertEquals(ReceiptOperationType.SELL, command.operation)
        assertEquals("key-1", command.idempotencyKey)
        assertEquals(1, command.items.size)
        assertEquals(1, command.payments.size)
        assertEquals(Decimal.parse("250.0"), command.taken)
        assertEquals("VAT_0", command.vatGroup)
    }

    @Test
    fun `toParentTicket parses parent ticket successfully`() {
        val parentTicketDto = ParentTicketRequest(
            parentTicketNumber = 12345,
            parentTicketDateTime = "2026-06-27T10:00:00Z",
            kgdKkmId = "kgd-1",
            parentTicketTotal = Decimal.parse("100.0"),
            parentTicketIsOffline = false
        )

        val parentTicket = ReceiptMapper.toParentTicket(parentTicketDto)

        assertNotNull(parentTicket)
        assertEquals(12345L, parentTicket.parentTicketNumber)
        assertEquals(1782554400000L, parentTicket.parentTicketDateTimeMillis)
        assertEquals("kgd-1", parentTicket.kgdKkmId)
        assertEquals(Money.fromTenge(Decimal.parse("100.0")), parentTicket.parentTicketTotal)
        assertEquals(false, parentTicket.parentTicketIsOffline)
    }

    @Test
    fun `toParentTicket handles null correctly`() {
        assertNull(ReceiptMapper.toParentTicket(null))
    }

    @Test
    fun `toParentTicket parses parent ticket without Z suffix successfully`() {
        val parentTicketDto = ParentTicketRequest(
            parentTicketNumber = 12345,
            parentTicketDateTime = "2026-06-27T10:00:00",
            kgdKkmId = "kgd-1",
            parentTicketTotal = Decimal.parse("100.0"),
            parentTicketIsOffline = false
        )

        val parentTicket = ReceiptMapper.toParentTicket(parentTicketDto)

        assertNotNull(parentTicket)
        assertEquals(12345L, parentTicket.parentTicketNumber)
        assertEquals(1782554400000L, parentTicket.parentTicketDateTimeMillis)
    }

    @Test
    fun `toCreateReceiptCommand default parameters`() {
        val command = ReceiptMapper.toCreateReceiptCommand(
            kkmId = "kkm-1",
            pin = "1234",
            operation = ReceiptOperationType.SELL,
            idempotencyKey = "key-1",
            items = emptyList(),
            discountPercent = null,
            discountSum = null,
            markupPercent = null,
            markupSum = null,
            payments = emptyList(),
            taken = Decimal.parse("0.0")
        )
        assertNull(command.parentTicket)
        assertNull(command.vatGroup)
        assertNull(command.customerBin)
    }
}
