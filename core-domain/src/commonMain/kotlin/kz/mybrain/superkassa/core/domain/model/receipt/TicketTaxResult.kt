package kz.mybrain.superkassa.core.domain.model.receipt

import kotlinx.serialization.Serializable

/**
 * Результат вычисления распределения налогов по чеку.
 *
 * @property ticketTaxes Список рассчитанных строк налогообложения по чеку ([TaxLine]).
 */
@Serializable
data class TicketTaxResult(
    val ticketTaxes: List<TaxLine>
)
