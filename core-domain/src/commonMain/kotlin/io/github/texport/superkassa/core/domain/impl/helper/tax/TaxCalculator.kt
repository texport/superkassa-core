package io.github.texport.superkassa.core.domain.impl.helper.tax

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.common.TaxRegime
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptItem
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.model.receipt.TaxLine
import io.github.texport.superkassa.core.domain.api.model.receipt.TicketTaxResult
import kotlin.math.abs
import kotlin.math.sign

/**
 * НДС чека «в том числе»: налог выделяется из суммы, в которую он заложен.
 *
 * Расчёт один для бумаги, счётчиков смены и запроса в БФД — прежде у каждого
 * был свой, и налог на чеке, в X/Z и у БФД расходился.
 *
 * - Налог позиции: сумма × ставка / (100 + ставка), к ближайшему тиыну.
 *   Сторно вычитает налог своей строки.
 * - Налог чека по ставке — сумма налогов позиций: так его складывает БФД
 *   (`OperationCalculator.updateTaxes`), а налог с суммы группы расходился
 *   бы с ним на тиын.
 * - Скидка или наценка на чек делится между ставками пропорционально
 *   обороту, как БФД масштабирует оборот (`processFraction`), и меняет
 *   налог чека на налог своей доли.
 * - Ставка позиции берётся у позиции, ставка кассы — когда своей нет.
 *   Неплательщик НДС налог не выделяет.
 * - НДС 0 % — налог с нулевой суммой. «Без НДС» — позиция без налога.
 */
class TaxCalculator {

    /**
     * Налог чека по позициям, по скидке или наценке и итогом по ставкам.
     *
     * @param request чек с позициями, режимом и ставкой кассы, скидкой или наценкой.
     */
    fun calculate(request: ReceiptRequest): TicketTaxResult {
        val itemTaxes = request.items.map { item ->
            taxOf(vatGroupOf(item, request.taxRegime, request.defaultVatGroup), item.sum.tiyn())
        }
        val modifier = Modifier.of(request)
        val groups = groupTotals(request.items, itemTaxes).map { it.modifiedBy(modifier) }
        return TicketTaxResult(
            ticketTaxes = groups.filter { it.turnover > 0 }.map { it.line() },
            itemTaxes = itemTaxes,
            modifierTaxes = groups.mapNotNull { it.modifierTax }.filter { it.taxSum.tiyn() > 0 }
        )
    }

    /**
     * Ставка, по которой облагается позиция, либо `null`, если налога нет.
     *
     * @param item позиция чека.
     * @param regime налоговый режим кассы.
     * @param default ставка чека или кассы для позиции без своей ставки.
     */
    fun vatGroupOf(item: ReceiptItem, regime: TaxRegime, default: VatGroup?): VatGroup? {
        if (regime == TaxRegime.NO_VAT) return null
        return (item.vatGroup ?: default)?.takeIf { it != VatGroup.NO_VAT }
    }

    private fun groupTotals(items: List<ReceiptItem>, itemTaxes: List<TaxLine?>): List<GroupTotal> {
        val totals = LinkedHashMap<VatGroup, GroupTotal>()
        items.zip(itemTaxes).forEach { (item, tax) ->
            if (tax == null) return@forEach
            val sign = if (item.isStorno) -1L else 1L
            val total = totals.getOrPut(tax.vatGroup) { GroupTotal(tax.vatGroup, 0L, 0L) }
            totals[tax.vatGroup] = total.copy(
                turnover = total.turnover + sign * item.sum.tiyn(),
                tax = total.tax + sign * tax.taxSum.tiyn()
            )
        }
        return totals.values.toList()
    }

    /** Оборот и налог одной ставки чека. */
    private data class GroupTotal(
        val group: VatGroup,
        val turnover: Long,
        val tax: Long,
        val modifierTax: TaxLine? = null
    ) {
        fun modifiedBy(modifier: Modifier?): GroupTotal {
            if (modifier == null || turnover <= 0L) return this
            val share = modifier.shareOf(turnover)
            val shareTax = taxOf(group, abs(share)) ?: return this
            return copy(
                turnover = turnover + share,
                tax = tax + share.sign * shareTax.taxSum.tiyn(),
                modifierTax = shareTax
            )
        }

        fun line(): TaxLine = TaxLine(group, group.percent, Money.fromTiyn(turnover - tax), Money.fromTiyn(tax))
    }

    /**
     * Скидка или наценка на чек как поправка к сумме позиций.
     *
     * @property delta наценка со знаком плюс, скидка — со знаком минус.
     * @property itemsTotal сумма позиций до скидки или наценки.
     */
    private class Modifier(private val delta: Long, private val itemsTotal: Long) {
        /** Изменение оборота ставки: доля скидки со знаком минус, наценки — плюс. */
        fun shareOf(turnover: Long): Long =
            proportionalShare(turnover, (itemsTotal + delta).coerceAtLeast(0L), itemsTotal) - turnover

        companion object {
            fun of(request: ReceiptRequest): Modifier? {
                val delta = (request.markup?.tiyn() ?: 0L) - (request.discount?.tiyn() ?: 0L)
                val itemsTotal = request.items.sumOf { if (it.isStorno) -it.sum.tiyn() else it.sum.tiyn() }
                return if (delta == 0L || itemsTotal <= 0L) null else Modifier(delta, itemsTotal)
            }
        }
    }

    private companion object {
        /** Сто процентов в тысячных долях. */
        const val PERCENT_THOUSANDTHS: Long = 100_000

        /**
         * Налог, заложенный в сумму: ставка задана в тысячных, доля считается
         * целыми тиынами и округляется к ближайшему один раз.
         */
        fun taxOf(group: VatGroup?, sumTiyn: Long): TaxLine? {
            if (group == null || group == VatGroup.NO_VAT) return null
            val rate = group.percentThousandths.toLong()
            val tax = Decimal.roundedDiv(sumTiyn * rate, PERCENT_THOUSANDTHS + rate)
            return TaxLine(group, group.percent, Money.fromTiyn(sumTiyn - tax), Money.fromTiyn(tax))
        }
    }
}
