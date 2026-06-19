package kz.mybrain.superkassa.core.domain.tax

import kz.mybrain.superkassa.core.domain.model.Money
import kz.mybrain.superkassa.core.domain.model.ReceiptItem
import kz.mybrain.superkassa.core.domain.model.TaxRegime
import kz.mybrain.superkassa.core.domain.model.VatGroup

/**
 * Чистый сервис расчёта НДС для чеков.
 *
 * На первом этапе считает налоги только на уровне всего чека (ticket.taxes[]),
 * исходя из базовой группы НДС ККМ.
 */
class TaxCalculationService {

    data class TaxLine(
        val vatGroup: VatGroup,
        val percent: Int,
        val taxBase: Money,
        val taxSum: Money
    )

    data class TicketTaxResult(
        val ticketTaxes: List<TaxLine>
    )

    /**
     * Рассчитывает налоговые суммы по чеку на уровне ticket.taxes[].
     *
     * Правила:
     * - Для NO_VAT и VAT_0 сумма НДС = 0.
     * - Для VAT_16 используется формула выделения НДС из цены, включающей НДС:
     *   НДС = S - S / (1 + P/100), где S — сумма с НДС, P — ставка (16).
     *
     * На текущем шаге все позиции считаются с единой группой НДС (defaultVatGroup).
     */
    fun calculateTicketTaxes(
        items: List<ReceiptItem>,
        taxRegime: TaxRegime,
        defaultVatGroup: VatGroup,
        /**
         * Опциональное переопределение группы НДС.
         * Используется, например, для расчёта налога по одной позиции с её собственной группой.
         */
        overrideVatGroup: VatGroup? = null
    ): TicketTaxResult {
        if (items.isEmpty()) return TicketTaxResult(emptyList())
        if (taxRegime == TaxRegime.NO_VAT) return TicketTaxResult(emptyList())

        // Group items by resolved VatGroup
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
            val percent = when (vatGroup) {
                VatGroup.NO_VAT -> 0
                VatGroup.VAT_0 -> 0
                VatGroup.VAT_5 -> 5
                VatGroup.VAT_10 -> 10
                VatGroup.VAT_16 -> 16
            }

            if (percent > 0) {
                val groupTotal = groupItems.sumOf { it.sum.bills.toDouble() + it.sum.coins / 100.0 }
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

