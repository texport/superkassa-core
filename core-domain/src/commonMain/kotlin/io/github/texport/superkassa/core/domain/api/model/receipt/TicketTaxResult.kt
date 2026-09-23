package io.github.texport.superkassa.core.domain.api.model.receipt

/**
 * Налог чека, разложенный так, как его видят бумага, счётчики и БФД.
 *
 * @property ticketTaxes итог чека по ставкам: оборот с учётом скидки или
 * наценки на чек и налог, равный сумме налогов позиций с поправкой
 * на налог скидки или наценки. Его печатает чек и копят счётчики смены.
 * @property itemTaxes налог каждой позиции в порядке позиций чека;
 * `null` — позиция без НДС. Уходит в БФД в `taxes` позиции.
 * @property modifierTaxes налог скидки или наценки на чек по ставкам.
 * Уходит в БФД в `taxes` скидки или наценки: по нему БФД уменьшает
 * или увеличивает налог чека.
 */
data class TicketTaxResult(
    val ticketTaxes: List<TaxLine>,
    val itemTaxes: List<TaxLine?> = emptyList(),
    val modifierTaxes: List<TaxLine> = emptyList()
)
