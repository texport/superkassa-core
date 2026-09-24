package io.github.texport.superkassa.testing.impl.bfd

import io.github.texport.superkassa.testing.api.bfd.BfdReport
import io.github.texport.superkassa.testing.api.bfd.tiyn
import kz.kazakhtelecom.proto.v203.MoneyPlacementEnum
import kz.kazakhtelecom.proto.v203.OperationTypeEnum
import kz.kazakhtelecom.proto.v203.Request
import kz.kazakhtelecom.proto.v203.TicketRequest

/**
 * Итоги смены на стороне БФД — по правилам референса
 * (`OperationCalculator` 11.42.1.1: `addTicket`, `processMoneyPlacement`,
 * `closeShift`, `openShift`).
 *
 * Строки операций и отделов считают позиции, а не чеки: +1 за каждый
 * товар, сторно товара — +0 и минус сумма. Скидки и наценки считаются
 * каждая отдельно: на чек и на каждую позицию; сторно — +0 и минус сумма.
 * Строка чеков считает чеки, оплаты — каждую оплату чека. Число чеков
 * и операций «за всё время» переходит в следующую смену, остальное
 * при закрытии обнуляется.
 *
 * Не повторяется одна странность референса: сторно скидки первой в чеке,
 * без скидки перед ним, референс записывает положительной суммой. Налоги
 * БФД стенда не считает: сверка итогов идёт на смене без НДС.
 */
internal class BfdShiftCounters {
    private var report = BfdReport()
    private val closed = mutableListOf<BfdReport>()

    /** Итоги текущей смены. */
    @Synchronized
    fun current(): BfdReport = report

    /** Итоги закрытых смен по порядку: каждая — как в её Z-отчёте. */
    @Synchronized
    fun closedShifts(): List<BfdReport> = closed.toList()

    /** Учитывает документ, который БФД только что принял. */
    @Synchronized
    fun apply(request: Request) {
        request.ticket?.let { report = BfdTicketTally.add(report, it) }
        request.money_placement?.let { report = place(report, it.operation, tiyn(it.sum), it.is_offline == true) }
        request.close_shift?.let {
            val withdraw = it.withdraw_money == true && report.cashTiyn != 0L
            if (withdraw) report = place(report, WITHDRAWAL, report.cashTiyn, it.is_offline == true)
            closed += report
            report = nextShift(report)
        }
    }

    private fun place(from: BfdReport, operation: MoneyPlacementEnum, sum: Long, offline: Boolean): BfdReport {
        val was = from.placements[operation] ?: BfdReport.PlacementTally(0, 0, 0, 0)
        val now = BfdReport.PlacementTally(
            totalCount = was.totalCount + 1,
            count = was.count + 1,
            sumTiyn = was.sumTiyn + sum,
            offlineCount = was.offlineCount + if (offline) 1 else 0
        )
        val cash = if (operation == WITHDRAWAL) -sum else sum
        return from.copy(placements = from.placements + (operation to now), cashTiyn = from.cashTiyn + cash)
    }

    /** Новая смена: переходят необнуляемые суммы, ящик и счёт чеков и операций за всё время. */
    private fun nextShift(last: BfdReport) = BfdReport(
        tickets = last.tickets.mapValues { (_, t) -> ZERO_TICKETS.copy(totalCount = t.totalCount) },
        placements = last.placements.mapValues { (_, p) -> BfdReport.PlacementTally(p.totalCount, 0, 0, 0) },
        nonNullable = last.nonNullable,
        startShiftNonNullable = last.nonNullable,
        cashTiyn = last.cashTiyn
    )
}

private val WITHDRAWAL = MoneyPlacementEnum.MONEY_PLACEMENT_WITHDRAWAL
private val ZERO_TICKETS = BfdReport.TicketTally(0, 0, 0, emptyMap(), 0, 0, 0, 0)

/** Кладёт в ящик и выручку: продажа и возврат покупки — плюс, возврат продажи и покупка — минус. */
internal fun TicketRequest.direction(): Long = when (operation) {
    OperationTypeEnum.OPERATION_SELL, OperationTypeEnum.OPERATION_BUY_RETURN -> 1L
    else -> -1L
}
