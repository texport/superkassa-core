package io.github.texport.superkassa.core.presentation.impl.mapper

import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType
import io.github.texport.superkassa.core.presentation.api.model.receipt.ParentTicketRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptItemRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptPaymentRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ReceiptMapperTest {

    @Test
    fun `toItemInput maps basic fields correctly`() {
        val dto = ReceiptItemRequest(
            name = "Test Item",
            price = 150.0,
            quantity = 2.0,
            barcode = "12345678",
            vatGroup = "VAT_16",
            measureUnitCode = "796"
        )
        val item = ReceiptMapper.toItemInput(dto)
        assertEquals("Test Item", item.name)
        assertEquals(150.0, item.price)
        assertEquals(2.0, item.quantity)
        assertEquals("12345678", item.barcode)
        assertEquals("VAT_16", item.vatGroup)
        assertEquals("796", item.measureUnitCode)
        assertNull(item.discountPercent)
        assertNull(item.discountSum)
    }

    @Test
    fun `toPaymentInput maps payment types correctly`() {
        val dto = ReceiptPaymentRequest(type = "CASH", sum = 500.0)
        val payment = ReceiptMapper.toPaymentInput(dto)
        assertEquals("CASH", payment.type)
        assertEquals(500.0, payment.sum)
    }

    @Test
    fun `toCreateReceiptCommand maps full sell command correctly`() {
        val itemDto = ReceiptItemRequest(name = "Item 1", price = 100.0, quantity = 2.0)
        val paymentDto = ReceiptPaymentRequest(type = "CASH", sum = 200.0)

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
            taken = 250.0,
            defaultVatGroup = "VAT_0"
        )

        assertEquals("kkm-1", command.kkmId)
        assertEquals("1234", command.pin)
        assertEquals(ReceiptOperationType.SELL, command.operation)
        assertEquals("key-1", command.idempotencyKey)
        assertEquals(1, command.items.size)
        assertEquals(1, command.payments.size)
        assertEquals(250.0, command.taken)
        assertEquals("VAT_0", command.defaultVatGroup)
    }

    @Test
    fun `toParentTicket parses parent ticket successfully`() {
        val parentTicketDto = ParentTicketRequest(
            parentTicketNumber = 12345,
            parentTicketDateTime = "2026-06-27T10:00:00Z",
            kgdKkmId = "kgd-1",
            parentTicketTotal = 100.0,
            parentTicketIsOffline = false
        )

        val parentTicket = ReceiptMapper.toParentTicket(parentTicketDto)

        assertNotNull(parentTicket)
        assertEquals(12345L, parentTicket.parentTicketNumber)
        assertEquals(1782554400000L, parentTicket.parentTicketDateTimeMillis)
        assertEquals("kgd-1", parentTicket.kgdKkmId)
        assertEquals(Money.fromTenge(100.0), parentTicket.parentTicketTotal)
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
            parentTicketTotal = 100.0,
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
            taken = 0.0
        )
        assertNull(command.parentTicket)
        assertNull(command.defaultVatGroup)
        assertNull(command.customerBin)
    }
}
