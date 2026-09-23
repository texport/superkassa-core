package io.github.texport.superkassa.core.data.room

import io.github.texport.superkassa.testing.api.bfd.FakeBfd
import kz.kazakhtelecom.proto.v203.OperationTypeEnum
import kz.kazakhtelecom.proto.v203.TicketRequest
import kz.kazakhtelecom.proto.v203.ZXReport
import kz.kazakhtelecom.proto.v203.Money as BfdMoney

/**
 * Налог чека по ставкам так, как его складывает БФД.
 *
 * Повторяет эталон `OperationCalculator.updateTaxes` и
 * `mergeTaxReportIntoReport`: налог на весь чек (`taxes` чека) берётся
 * как есть, и тогда налоги позиций не читаются. Иначе налоги позиций
 * прибавляются, налоги сторно вычитаются, налог скидки на чек вычитается,
 * наценки — прибавляется. Позиция или скидка без `taxes` налог чека не меняет.
 *
 * @return ставка в тысячных процента → налог в тиынах.
 */
internal fun bfdTicketTax(ticket: TicketRequest): Map<Int, Long> {
    if (ticket.taxes.isNotEmpty()) return ticket.taxes.associate { it.percent to it.sum.tiyn() }
    val signed = ticket.items.flatMap { item ->
        item.commodity?.taxes.orEmpty().map { it to 1L } + item.storno_commodity?.taxes.orEmpty().map { it to -1L }
    } + ticket.amounts.discount?.taxes.orEmpty().map { it to -1L } +
        ticket.amounts.markup?.taxes.orEmpty().map { it to 1L }
    return signed.groupBy({ (tax, _) -> tax.percent }) { (tax, sign) -> sign * tax.sum.tiyn() }
        .mapValues { (_, sums) -> sums.sum() }
}

/**
 * Налоги заданы и у чека, и у позиций, скидки или наценки — такой чек
 * эталон отвергает («Taxes mix error», `TicketValidation.validateTaxesMix`).
 */
internal fun taxesMixed(ticket: TicketRequest): Boolean {
    val elsewhere = ticket.items.any { it.commodity?.taxes.orEmpty().isNotEmpty() || it.storno_commodity?.taxes.orEmpty().isNotEmpty() } ||
        ticket.amounts.discount?.taxes.orEmpty().isNotEmpty() || ticket.amounts.markup?.taxes.orEmpty().isNotEmpty()
    return ticket.taxes.isNotEmpty() && elsewhere
}

/** Последний чек, ушедший в БФД. */
internal fun FakeBfd.lastTicket(): TicketRequest = requests.mapNotNull { it.ticket }.last()

/** Итог X/Z по ставке и операции в последнем отчёте, ушедшем в БФД. */
internal fun FakeBfd.reportTax(percent: Int, operation: OperationTypeEnum): ZXReport.Tax.TaxOperation =
    xReports().last().taxes.single { it.percent == percent }.operations.single { it.operation == operation }

internal fun BfdMoney.tiyn(): Long = bills * TIYN_IN_TENGE + coins

/** Ставка 16 % в тысячных процента, как в протоколе. */
internal const val VAT_16_PERCENT = 16_000

private const val TIYN_IN_TENGE = 100L
