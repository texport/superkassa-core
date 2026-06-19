package kz.mybrain.superkassa.core.tax

import kz.mybrain.superkassa.core.domain.model.Money
import kz.mybrain.superkassa.core.domain.model.ReceiptItem
import kz.mybrain.superkassa.core.domain.model.TaxRegime
import kz.mybrain.superkassa.core.domain.model.VatGroup
import kz.mybrain.superkassa.core.domain.tax.TaxCalculationService
import kotlin.test.Test
import kotlin.test.assertEquals

class TaxCalculationServiceTest {

    private val taxService = TaxCalculationService()

    @Test
    fun `calculateTicketTaxes respects mixed vat groups from items`() {
        val items = listOf(
            ReceiptItem(
                name = "Item VAT16",
                sectionCode = "001",
                quantity = 1,
                price = Money(1160, 0),
                sum = Money(1160, 0),
                vatGroup = VatGroup.VAT_16
            ),
            ReceiptItem(
                name = "Item VAT5",
                sectionCode = "001",
                quantity = 1,
                price = Money(1050, 0),
                sum = Money(1050, 0),
                vatGroup = VatGroup.VAT_5
            )
        )

        val result = taxService.calculateTicketTaxes(
            items = items,
            taxRegime = TaxRegime.MIXED,
            defaultVatGroup = VatGroup.VAT_16
        )

        // В mixed-режиме должны появляться отдельные строки налогов по группам товаров.
        assertEquals(2, result.ticketTaxes.size)
        val groups = result.ticketTaxes.map { it.vatGroup }.toSet()
        assertEquals(setOf(VatGroup.VAT_16, VatGroup.VAT_5), groups)
    }

    @Test
    fun `calculateTicketTaxes returns empty for no vat regime`() {
        val items = listOf(
            ReceiptItem(
                name = "Item",
                sectionCode = "001",
                quantity = 1,
                price = Money(1000, 0),
                sum = Money(1000, 0),
                vatGroup = VatGroup.VAT_16
            )
        )

        val result = taxService.calculateTicketTaxes(
            items = items,
            taxRegime = TaxRegime.NO_VAT,
            defaultVatGroup = VatGroup.NO_VAT
        )

        assertEquals(0, result.ticketTaxes.size)
    }
}

