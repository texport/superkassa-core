package io.github.texport.superkassa.testing.api.bfd

import io.github.texport.superkassa.testing.api.bfd.BfdReport.PlacementTally
import io.github.texport.superkassa.testing.api.bfd.BfdReport.TaxTally
import io.github.texport.superkassa.testing.api.bfd.BfdReport.TicketTally
import io.github.texport.superkassa.testing.api.bfd.BfdReport.Tally
import kz.kazakhtelecom.proto.v203.ZXReport
import kz.kazakhtelecom.proto.v203.Money as BfdMoney

/**
 * Итоги X/Z-отчёта, который прислала касса, в сравнимом виде [BfdReport]:
 * нулевые строки отброшены, порядок строк не важен.
 */
fun ZXReport.counters(): BfdReport = BfdReport(
    sections = sections.associate { it.section_code to it.operations.tallies() }.filterValues { it.isNotEmpty() },
    operations = operations.tallies(),
    discounts = discounts.tallies(),
    markups = markups.tallies(),
    totalResult = total_result.tallies(),
    tickets = ticket_operations.associate { it.operation to it.tally() }.filterValues { it != EMPTY_TICKETS },
    placements = money_placements.associate { it.operation to it.tally() }.filterValues { it != EMPTY_PLACEMENTS },
    taxes = taxes.associate { (it.tax_type to it.percent) to it.tallies() }.filterValues { it.isNotEmpty() },
    nonNullable = non_nullable_sums.associate { it.operation to tiyn(it.sum) }.filterValues { it != 0L },
    startShiftNonNullable = start_shift_non_nullable_sums.associate { it.operation to tiyn(it.sum) }
        .filterValues { it != 0L },
    cashTiyn = tiyn(cash_sum),
    revenueTiyn = tiyn(revenue.sum).let { if (revenue.is_negative) -it else it }
)

private fun List<ZXReport.Operation>.tallies() =
    associate { it.operation to Tally(it.count.toLong(), tiyn(it.sum)) }.filterValues { it != EMPTY }

private fun ZXReport.TicketOperation.tally() = TicketTally(
    totalCount = tickets_total_count.toLong(),
    count = tickets_count.toLong(),
    sumTiyn = tiyn(tickets_sum),
    payments = payments.associate { it.payment to it.tally() }.filterValues { it != EMPTY },
    offlineCount = (offline_count ?: 0).toLong(),
    discountTiyn = tiynOrZero(discount_sum),
    markupTiyn = tiynOrZero(markup_sum),
    changeTiyn = tiynOrZero(change_sum)
)

private fun ZXReport.TicketOperation.Payment.tally() = Tally((count ?: 0).toLong(), tiyn(sum))

private fun ZXReport.MoneyPlacement.tally() = PlacementTally(
    totalCount = operations_total_count.toLong(),
    count = operations_count.toLong(),
    sumTiyn = tiyn(operations_sum),
    offlineCount = (offline_count ?: 0).toLong()
)

private fun ZXReport.Tax.tallies() = operations
    .associate { it.operation to TaxTally(tiyn(it.turnover), tiyn(it.sum), tiynOrZero(it.turnover_without_tax)) }
    .filterValues { it != EMPTY_TAX }

/** Поле, обязательное с версии 2.0.0, а в схеме 2.0.3 необязательное: нет его — ноль. */
private fun tiynOrZero(money: BfdMoney?): Long = money?.let(::tiyn) ?: 0L

/** Деньги протокола в тиынах. */
internal fun tiyn(money: BfdMoney): Long = money.bills * TIYN_IN_TENGE + money.coins

private const val TIYN_IN_TENGE = 100L
private val EMPTY = Tally(0, 0)
private val EMPTY_TAX = TaxTally(0, 0, 0)
private val EMPTY_TICKETS = TicketTally(0, 0, 0, emptyMap(), 0, 0, 0, 0)
private val EMPTY_PLACEMENTS = PlacementTally(0, 0, 0, 0)
