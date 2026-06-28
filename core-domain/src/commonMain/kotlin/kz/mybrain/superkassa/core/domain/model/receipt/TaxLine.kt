package kz.mybrain.superkassa.core.domain.model.receipt

import kotlinx.serialization.Serializable
import kz.mybrain.superkassa.core.domain.model.common.Money
import kz.mybrain.superkassa.core.domain.model.common.VatGroup

/**
 * Рассчитанная строка налога чека по конкретной ставке НДС.
 *
 * @property vatGroup Группа ставки НДС.
 * @property percent Величина налоговой ставки в процентах.
 * @property taxBase Налогооблагаемая база (сумма оборота по данной ставке).
 * @property taxSum Рассчитанная сумма налога.
 */
@Serializable
data class TaxLine(
    val vatGroup: VatGroup,
    val percent: Int,
    val taxBase: Money,
    val taxSum: Money
)
