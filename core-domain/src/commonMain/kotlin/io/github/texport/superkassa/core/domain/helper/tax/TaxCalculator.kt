package io.github.texport.superkassa.core.domain.helper.tax

import io.github.texport.superkassa.core.domain.model.common.Money
import io.github.texport.superkassa.core.domain.model.common.TaxRegime
import io.github.texport.superkassa.core.domain.model.common.VatGroup
import io.github.texport.superkassa.core.domain.model.receipt.ReceiptItem
import io.github.texport.superkassa.core.domain.model.receipt.TaxLine
import io.github.texport.superkassa.core.domain.model.receipt.TicketTaxResult

/**
 * Калькулятор распределения сумм налогов по позициям чека.
 *
 * Отвечает за вычисление сумм НДС и налогооблагаемой базы для всего чека
 * на основе налогового режима, ставок НДС и переданных позиций чека.
 */
class TaxCalculator {

    /**
     * Рассчитывает налоговые суммы чека на уровне всего билета (чека).
     *
     * Группирует позиции чека по определенным ставкам НДС (VatGroup) с учетом
     * налогового режима организации (TaxRegime.MIXED позволяет использовать индивидуальные
     * ставки на позициях, в противном случае применяется общая дефолтная ставка).
     *
     * Налог рассчитывается методом выделения из общей суммы (groupTotal) по формуле:
     * НДС = Оборот - (Оборот / (1 + Ставка% / 100)).
     *
     * @param items Список позиций (товаров/услуг) в чеке.
     * @param taxRegime Налоговый режим организации.
     * @param defaultVatGroup Ставка НДС по умолчанию для ККМ.
     * @param overrideVatGroup Необязательное принудительное переопределение ставки НДС для всех позиций.
     * @return Результат расчета [TicketTaxResult], содержащий детальную информацию по каждой ставке.
     */
    fun calculateTicketTaxes(
        items: List<ReceiptItem>,
        taxRegime: TaxRegime,
        defaultVatGroup: VatGroup,
        overrideVatGroup: VatGroup? = null
    ): TicketTaxResult {
        if (items.isEmpty()) return TicketTaxResult(emptyList())
        if (taxRegime == TaxRegime.NO_VAT) return TicketTaxResult(emptyList())

        // Группируем элементы по вычисленной группе НДС
        val itemsByGroup = items.groupBy { item ->
            overrideVatGroup
                ?: if (taxRegime == TaxRegime.MIXED) {
                    item.vatGroup ?: defaultVatGroup
                } else {
                    defaultVatGroup
                }
        }

        val taxLines = mutableListOf<TaxLine>()

        itemsByGroup.forEach { (vatGroup, groupItems) ->
            val percent = vatGroup.percent

            // Расчет налогооблагаемой базы производится только для облагаемых НДС групп (процент > 0).
            // Необлагаемый оборот (NO_VAT) и ставка НДС 0% (VAT_0) здесь отсекаются, так как для них
            // налоговые начисления и налогооблагаемый оборот не рассчитываются.
            if (percent > 0) {
                val groupTotal = groupItems.sumOf { item ->
                    val itemTotal = item.sum.bills.toDouble() + item.sum.coins / 100.0
                    if (item.isStorno) -itemTotal else itemTotal
                }
                if (groupTotal > 0.0) {
                    val vatAmount = groupTotal - groupTotal / (1.0 + percent / 100.0)
                    val baseAmount = groupTotal - vatAmount
                    taxLines.add(
                        TaxLine(
                            vatGroup = vatGroup,
                            percent = percent,
                            taxBase = Money.fromTenge(baseAmount),
                            taxSum = Money.fromTenge(vatAmount)
                        )
                    )
                }
            }
        }

        return TicketTaxResult(taxLines)
    }
}
