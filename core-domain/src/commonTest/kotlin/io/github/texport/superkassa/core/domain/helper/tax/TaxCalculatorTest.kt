package io.github.texport.superkassa.core.domain.impl.helper.tax

import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.common.TaxRegime
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptItem
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TaxCalculatorTest {

    private val calculator = TaxCalculator()

    @Test
    fun `позиции разных ставок дают по строке налога на ставку`() {
        val result = calculator.calculate(receipt(TaxRegime.MIXED, item(116_000, VatGroup.VAT_16), item(105_000, VatGroup.VAT_5)))

        assertEquals(listOf(VatGroup.VAT_16 to 16_000L, VatGroup.VAT_5 to 5_000L), result.ticketTaxes.map { it.vatGroup to it.taxSum.tiyn() })
    }

    @Test
    fun `неплательщик НДС налог не выделяет`() {
        val result = calculator.calculate(receipt(TaxRegime.NO_VAT, item(100_000, VatGroup.VAT_16)))

        assertEquals(emptyList(), result.ticketTaxes)
        assertEquals(listOf(null), result.itemTaxes)
    }

    @Test
    fun `сторно всей позиции гасит налог ставки`() {
        val result = calculator.calculate(receipt(TaxRegime.VAT_PAYER, item(116_000), item(116_000, storno = true)))

        assertEquals(emptyList(), result.ticketTaxes)
        assertEquals(listOf(16_000L, 16_000L), result.itemTaxes.map { it?.taxSum?.tiyn() })
    }

    @Test
    fun `налог выделяется целыми тиынами`() {
        // 435.84 при ставке 16 % — это ровно 60.12 налога и 375.72 базы.
        val line = calculator.calculate(receipt(TaxRegime.VAT_PAYER, item(43_584))).ticketTaxes.single()

        assertEquals(6_012L to 37_572L, line.taxSum.tiyn() to line.taxBase.tiyn())
    }

    @Test
    fun `налог чека - сумма налогов позиций, как складывает БФД`() {
        // Три позиции по 1,00: налог каждой 0,14, у чека 0,42. С суммы группы
        // вышло бы 0,41, и бумага расходилась бы с БФД на тиын.
        val result = calculator.calculate(receipt(TaxRegime.VAT_PAYER, item(100), item(100), item(100)))

        assertEquals(42L, result.ticketTaxes.single().taxSum.tiyn())
    }

    @Test
    fun `скидка на чек уменьшает налог на налог своей доли`() {
        val result = calculator.calculate(receipt(TaxRegime.VAT_PAYER, item(100_000), discount = 10_000))

        val line = result.ticketTaxes.single()
        assertEquals(12_414L to 90_000L, line.taxSum.tiyn() to line.taxBase.tiyn() + line.taxSum.tiyn())
        assertEquals(1_379L, result.modifierTaxes.single().taxSum.tiyn())
    }

    @Test
    fun `скидка делится между ставками пропорционально обороту`() {
        val result = calculator.calculate(
            receipt(TaxRegime.MIXED, item(116_000, VatGroup.VAT_16), item(100_000, VatGroup.NO_VAT), discount = 21_600)
        )

        // 10 % скидки: ставке 16 % достаётся 116,00 из 216,00 — оборот 1044,00.
        val line = result.ticketTaxes.single()
        assertEquals(104_400L to 14_400L, line.taxBase.tiyn() + line.taxSum.tiyn() to line.taxSum.tiyn())
    }

    @Test
    fun `НДС 0 процентов - налог с нулевой суммой, а не без НДС`() {
        val result = calculator.calculate(receipt(TaxRegime.MIXED, item(50_000, VatGroup.VAT_0), item(30_000, VatGroup.NO_VAT)))

        val line = result.ticketTaxes.single()
        assertEquals(Triple(VatGroup.VAT_0, 0L, 50_000L), Triple(line.vatGroup, line.taxSum.tiyn(), line.taxBase.tiyn()))
        assertEquals(VatGroup.VAT_0, result.itemTaxes[0]?.vatGroup)
        assertNull(result.itemTaxes[1])
    }

    @Test
    fun `плательщик НДС берёт ставку позиции, ставку кассы - когда своей нет`() {
        val result = calculator.calculate(receipt(TaxRegime.VAT_PAYER, item(116_000), item(100_000, VatGroup.NO_VAT)))

        assertEquals(listOf(VatGroup.VAT_16 to 16_000L), result.ticketTaxes.map { it.vatGroup to it.taxSum.tiyn() })
    }

    @Test
    fun `скидка позиции - налог строки полной суммой, у скидки разница, итог после скидки`() {
        val discounted = item(90_000).copy(price = Money.fromTiyn(100_000), discount = Money.fromTiyn(10_000))

        val result = calculator.calculate(receipt(TaxRegime.VAT_PAYER, discounted, item(50_000)))

        assertEquals(listOf(13_793L, 6_897L), result.itemTaxes.map { it?.taxSum?.tiyn() })
        assertEquals(1_379L to 8_621L, result.itemModifierTaxes[0]?.let { it.taxSum.tiyn() to it.taxBase.tiyn() })
        assertNull(result.itemModifierTaxes[1])
        assertEquals(listOf(12_414L + 6_897L), result.ticketTaxes.map { it.taxSum.tiyn() })
    }

    @Test
    fun `наценка позиции без НДС налога не несёт`() {
        val marked = item(105_000, VatGroup.NO_VAT).copy(markup = Money.fromTiyn(5_000))

        assertEquals(listOf(null), calculator.calculate(receipt(TaxRegime.VAT_PAYER, marked)).itemModifierTaxes)
    }

    @Test
    fun `доля считается без переполнения и половина идёт к чётному`() {
        assertEquals(2L, proportionalShare(5, 1, 2))
        assertEquals(4L, proportionalShare(7, 1, 2))
        assertEquals(900_000_000_000L, proportionalShare(1_000_000_000_000L, 9_000_000_000L, 10_000_000_000L))
    }

    private fun item(sumTiyn: Long, vat: VatGroup? = null, storno: Boolean = false) = ReceiptItem(
        name = "Нан", sectionCode = "001", quantity = 1_000, price = Money.fromTiyn(sumTiyn),
        sum = Money.fromTiyn(sumTiyn), vatGroup = vat, isStorno = storno
    )

    private fun receipt(regime: TaxRegime, vararg items: ReceiptItem, discount: Long? = null) = ReceiptRequest(
        kkmId = "kkm", pin = "0000", operation = ReceiptOperationType.SELL, items = items.toList(),
        payments = emptyList(), total = Money.fromTiyn(0), idempotencyKey = "key", taxRegime = regime,
        defaultVatGroup = VatGroup.VAT_16, discount = discount?.let { Money.fromTiyn(it) }
    )
}
