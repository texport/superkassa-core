package io.github.texport.superkassa.core.domain.api.model.receipt

/**
 * Результат вычисления распределения налогов по чеку.
 *
 * @property ticketTaxes Список рассчитанных строк налогообложения по чеку ([TaxLine]).
 */
data class TicketTaxResult(
    val ticketTaxes: List<TaxLine>
)
