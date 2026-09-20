package io.github.texport.superkassa.core.domain.impl.helper.tax

import kotlin.test.Test
import kotlin.test.assertEquals
import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.common.TaxRegime
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptItem

class TaxCalculatorTest {

    private val taxService = TaxCalculator()

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

    @Test
    fun `calculateTicketTaxes subtracts storno items from group total`() {
        val items = listOf(
            ReceiptItem(
                name = "Item",
                sectionCode = "001",
                quantity = 1,
                price = Money(1160, 0),
                sum = Money(1160, 0),
                vatGroup = VatGroup.VAT_16,
                isStorno = false
            ),
            ReceiptItem(
                name = "Item Cancelled",
                sectionCode = "001",
                quantity = 1,
                price = Money(1160, 0),
                sum = Money(1160, 0),
                vatGroup = VatGroup.VAT_16,
                isStorno = true
            )
        )

        val result = taxService.calculateTicketTaxes(
            items = items,
            taxRegime = TaxRegime.VAT_PAYER,
            defaultVatGroup = VatGroup.VAT_16
        )

        // Итоговая сумма группы НДС должна быть 0 (1160 - 1160), поэтому налоговых строк быть не должно.
        assertEquals(0, result.ticketTaxes.size)
    }

    @Test
    fun `налог выделяется целыми тиынами`() {
        // 435.84 при ставке 16 % — это ровно 60.12 налога и 375.72 базы.
        // В плавающей точке 435.84 - 435.84 / 1.16 давало 60.11999999999998,
        // и налог уходил в ОФД на тиын меньше базы плюс налога.
        val items = listOf(
            ReceiptItem(
                name = "Кофе",
                sectionCode = "001",
                quantity = 1,
                price = Money.fromTiyn(43584L),
                sum = Money.fromTiyn(43584L),
                vatGroup = VatGroup.VAT_16
            )
        )

        val result = taxService.calculateTicketTaxes(
            items = items,
            taxRegime = TaxRegime.VAT_PAYER,
            defaultVatGroup = VatGroup.VAT_16
        )

        val line = result.ticketTaxes.single()
        assertEquals(6012L, line.taxSum.tiyn())
        assertEquals(37572L, line.taxBase.tiyn())
        assertEquals(43584L, line.taxBase.tiyn() + line.taxSum.tiyn())
    }
}
