package io.github.texport.superkassa.core.domain.api.model.receipt

/**
 * Налог чека, разложенный так, как его видят бумага, счётчики и БФД.
 *
 * @property ticketTaxes итог чека по ставкам: оборот с учётом скидки или
 * наценки на чек и налог, равный сумме налогов позиций с поправкой
 * на налог скидки или наценки. Его печатает чек и копят счётчики смены.
 * @property itemTaxes налог каждой позиции в порядке позиций чека — с суммы
 * строки до её скидки и наценки; `null` — позиция без НДС. Уходит в БФД
 * в `taxes` позиции.
 * @property itemModifierTaxes налог скидки или наценки каждой позиции:
 * разница налога строки до и после неё; `null` — у позиции её нет или нет
 * НДС. Уходит в БФД в `taxes` элемента скидки или наценки, и налог позиции
 * у БФД выходит налогом суммы после скидки.
 * @property modifierTaxes налог скидки или наценки на чек по ставкам.
 * Уходит в БФД в `taxes` скидки или наценки: по нему БФД уменьшает
 * или увеличивает налог чека.
 * @property receiptTaxes налог на весь чек, когда НДС задан одной ставкой
 * на чек. Уходит в БФД в `taxes` самого чека, а позиции, скидка и наценка
 * налогов тогда не несут: CPCR допускает налоги либо там, либо там.
 */
data class TicketTaxResult(
    val ticketTaxes: List<TaxLine>,
    val itemTaxes: List<TaxLine?> = emptyList(),
    val itemModifierTaxes: List<TaxLine?> = emptyList(),
    val modifierTaxes: List<TaxLine> = emptyList(),
    val receiptTaxes: List<TaxLine> = emptyList()
)
