package io.github.texport.superkassa.core.domain.api.model.receipt

import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup

/**
 * Рассчитанная строка налога чека по конкретной ставке НДС.
 *
 * @property vatGroup Группа ставки НДС.
 * @property percent Величина налоговой ставки в процентах.
 * @property taxBase Налогооблагаемая база (сумма облагаемого оборота без учета НДС).
 * @property taxSum Рассчитанная сумма налога.
 */
data class TaxLine(
    val vatGroup: VatGroup,
    val percent: Int,
    val taxBase: Money,
    val taxSum: Money
)
