package kz.mybrain.superkassa.core.presentation.mapper

import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.model.common.Money
import kz.mybrain.superkassa.core.domain.model.common.VatGroup
import kz.mybrain.superkassa.core.domain.model.receipt.PaymentType
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptOperationType
import kz.mybrain.superkassa.core.presentation.model.ParentTicketDto
import kz.mybrain.superkassa.core.presentation.model.ReceiptItemDto
import kz.mybrain.superkassa.core.presentation.model.ReceiptPaymentDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ReceiptMapperTest {

    @Test
    fun `toReceiptItem maps basic fields correctly`() {
        val dto = ReceiptItemDto(
            name = "Test Item",
            price = 150.0,
            quantity = 2,
            barcode = "12345678",
            vatGroup = "VAT_16",
            measureUnitCode = "796"
        )
        val item = ReceiptMapper.toReceiptItem(dto)
        assertEquals("Test Item", item.name)
        assertEquals(Money.fromTenge(150.0), item.price)
        assertEquals(2L, item.quantity)
        assertEquals(Money.fromTenge(300.0), item.sum)
        assertEquals("12345678", item.barcode)
        assertEquals(VatGroup.VAT_16, item.vatGroup)
        assertEquals("796", item.measureUnitCode)
        assertNull(item.discount)
        assertNull(item.markup)
    }

    @Test
    fun `toReceiptItem calculates item discount and markup correctly`() {
        val discountDto = ReceiptItemDto(
            name = "Discount Item",
            price = 100.0,
            quantity = 2,
            discountPercent = 10.0
        )
        val discountItem = ReceiptMapper.toReceiptItem(discountDto)
        assertEquals(Money.fromTenge(180.0), discountItem.sum)
        assertEquals(Money.fromTenge(20.0), discountItem.discount)

        val markupDto = ReceiptItemDto(
            name = "Markup Item",
            price = 100.0,
            quantity = 2,
            markupSum = 15.0
        )
        val markupItem = ReceiptMapper.toReceiptItem(markupDto)
        assertEquals(Money.fromTenge(215.0), markupItem.sum)
        assertEquals(Money.fromTenge(15.0), markupItem.markup)
    }

    @Test
    fun `toReceiptItem throws on invalid measure unit`() {
        val dto = ReceiptItemDto(
            name = "Bad Item",
            price = 10.0,
            quantity = 1,
            measureUnitCode = "INVALID_CODE"
        )
        assertFailsWith<ValidationException> {
            ReceiptMapper.toReceiptItem(dto)
        }
    }

    @Test
    fun `toReceiptItem throws on invalid vat group`() {
        val dto = ReceiptItemDto(
            name = "Bad Vat Item",
            price = 10.0,
            quantity = 1,
            vatGroup = "INVALID_VAT"
        )
        assertFailsWith<IllegalArgumentException> {
            ReceiptMapper.toReceiptItem(dto)
        }
    }

    @Test
    fun `toReceiptPayment maps payment types correctly`() {
        val dto = ReceiptPaymentDto(type = "CASH", sum = 500.0)
        val payment = ReceiptMapper.toReceiptPayment(dto)
        assertEquals(PaymentType.CASH, payment.type)
        assertEquals(Money.fromTenge(500.0), payment.sum)

        assertFailsWith<IllegalArgumentException> {
            ReceiptMapper.toReceiptPayment(dto.copy(type = "BAD_TYPE"))
        }
    }

    @Test
    fun `toReceiptRequest maps full sell request correctly`() {
        val itemDto = ReceiptItemDto(name = "Item 1", price = 100.0, quantity = 2)
        val paymentDto = ReceiptPaymentDto(type = "CASH", sum = 200.0)

        val request = ReceiptMapper.toReceiptRequest(
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
            taken = 250.0,
            change = null,
            defaultVatGroup = "VAT_0"
        )

        assertEquals("kkm-1", request.kkmId)
        assertEquals("1234", request.pin)
        assertEquals(ReceiptOperationType.SELL, request.operation)
        assertEquals("key-1", request.idempotencyKey)
        assertEquals(Money.fromTenge(200.0), request.total)
        assertEquals(Money.fromTenge(250.0), request.taken)
        assertEquals(Money.fromTenge(50.0), request.change)
        assertEquals(VatGroup.VAT_0, request.defaultVatGroup)
    }

    @Test
    fun `toReceiptRequest requires parentTicket for returns`() {
        val itemDto = ReceiptItemDto(name = "Item 1", price = 100.0, quantity = 1)
        val paymentDto = ReceiptPaymentDto(type = "CASH", sum = 100.0)

        assertFailsWith<ValidationException> {
            ReceiptMapper.toReceiptRequest(
                kkmId = "kkm-1",
                pin = "1234",
                operation = ReceiptOperationType.SELL_RETURN,
                idempotencyKey = "key-1",
                items = listOf(itemDto),
                discountPercent = null,
                discountSum = null,
                markupPercent = null,
                markupSum = null,
                payments = listOf(paymentDto),
                taken = 100.0,
                change = null,
                parentTicket = null
            )
        }
    }

    @Test
    fun `toReceiptRequest parses parent ticket successfully`() {
        val itemDto = ReceiptItemDto(name = "Item 1", price = 100.0, quantity = 1)
        val paymentDto = ReceiptPaymentDto(type = "CASH", sum = 100.0)
        val parentTicketDto = ParentTicketDto(
            parentTicketNumber = 12345,
            parentTicketDateTime = "2026-06-27T10:00:00Z",
            kgdKkmId = "kgd-1",
            parentTicketTotal = 100.0,
            parentTicketIsOffline = false
        )

        val request = ReceiptMapper.toReceiptRequest(
            kkmId = "kkm-1",
            pin = "1234",
            operation = ReceiptOperationType.SELL_RETURN,
            idempotencyKey = "key-1",
            items = listOf(itemDto),
            discountPercent = null,
            discountSum = null,
            markupPercent = null,
            markupSum = null,
            payments = listOf(paymentDto),
            taken = 100.0,
            change = null,
            parentTicket = parentTicketDto
        )

        assertNotNull(request.parentTicket)
        assertEquals(12345L, request.parentTicket!!.parentTicketNumber)
        assertEquals(1782554400000L, request.parentTicket!!.parentTicketDateTimeMillis)
    }

    @Test
    fun `toReceiptRequest checks discount conflict`() {
        val itemDto = ReceiptItemDto(name = "Item 1", price = 100.0, quantity = 1, discountPercent = 5.0)
        val paymentDto = ReceiptPaymentDto(type = "CASH", sum = 95.0)

        assertFailsWith<ValidationException> {
            ReceiptMapper.toReceiptRequest(
                kkmId = "kkm-1",
                pin = "1234",
                operation = ReceiptOperationType.SELL,
                idempotencyKey = "key-1",
                items = listOf(itemDto),
                discountPercent = 10.0,
                discountSum = null,
                markupPercent = null,
                markupSum = null,
                payments = listOf(paymentDto),
                taken = 95.0,
                change = null
            )
        }
    }

    @Test
    fun `toReceiptRequest throws when taken is less than cashSumTenge`() {
        val itemDto = ReceiptItemDto(name = "Item 1", price = 100.0, quantity = 1)
        val paymentDto = ReceiptPaymentDto(type = "CASH", sum = 100.0)

        assertFailsWith<IllegalArgumentException> {
            ReceiptMapper.toReceiptRequest(
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
                taken = 50.0,
                change = null
            )
        }
    }

    @Test
    fun `toReceiptRequest throws on invalid defaultVatGroup`() {
        val itemDto = ReceiptItemDto(name = "Item 1", price = 100.0, quantity = 1)
        val paymentDto = ReceiptPaymentDto(type = "CASH", sum = 100.0)

        assertFailsWith<IllegalArgumentException> {
            ReceiptMapper.toReceiptRequest(
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
                taken = 100.0,
                change = null,
                defaultVatGroup = "INVALID_VAT_GROUP"
            )
        }
    }
}
