package io.github.texport.superkassa.testing.impl.bfd

import io.github.texport.superkassa.testing.api.bfd.BfdReport
import io.github.texport.superkassa.testing.api.bfd.BfdReport.Tally
import io.github.texport.superkassa.testing.api.bfd.BfdReport.TicketTally
import io.github.texport.superkassa.testing.api.bfd.tiyn
import kz.kazakhtelecom.proto.v203.PaymentTypeEnum
import kz.kazakhtelecom.proto.v203.TicketRequest
import kz.kazakhtelecom.proto.v203.TicketRequest.Item.ItemTypeEnum

/**
 * Чек в итогах смены БФД — `OperationCalculator.addTicket`: отделы
 * и операции (`extractSections`, `getTotal`), скидки и наценки
 * (`updateDiscounts`, `updateMarkups`), окончательный итог (`consider`),
 * строка чеков (`updateTicketOperations`), необнуляемая сумма, ящик и выручка.
 */
internal object BfdTicketTally {

    fun add(report: BfdReport, ticket: TicketRequest): BfdReport {
        val op = ticket.operation
        var sections = report.sections
        var goods = Tally(0, 0)
        var discounts = Tally(0, 0)
        var markups = Tally(0, 0)
        ticket.items.forEach { item ->
            val line = line(item)
            when (item.type) {
                ItemTypeEnum.ITEM_TYPE_COMMODITY, ItemTypeEnum.ITEM_TYPE_STORNO_COMMODITY -> {
                    goods += line
                    val code = item.commodity?.section_code ?: checkNotNull(item.storno_commodity).section_code
                    val section = sections[code].orEmpty()
                    sections = sections + (code to section.plus(op, line))
                }
                ItemTypeEnum.ITEM_TYPE_DISCOUNT, ItemTypeEnum.ITEM_TYPE_STORNO_DISCOUNT -> discounts += line
                else -> markups += line
            }
        }
        ticket.amounts.discount?.let { discounts += Tally(1, tiyn(it.sum)) }
        ticket.amounts.markup?.let { markups += Tally(1, tiyn(it.sum)) }
        val total = tiyn(ticket.amounts.total)
        val cash = ticket.payments.filter { it.type == CASH }.sumOf { tiyn(it.sum) }
        return report.copy(
            sections = sections,
            operations = report.operations.plus(op, goods),
            discounts = report.discounts.plus(op, discounts.takeIf { it != NONE }),
            markups = report.markups.plus(op, markups.takeIf { it != NONE }),
            totalResult = report.totalResult.plus(op, final(goods, discounts, markups)),
            tickets = report.tickets + (op to tickets(report.tickets[op], ticket)),
            nonNullable = report.nonNullable + (op to (report.nonNullable[op] ?: 0L) + total),
            cashTiyn = report.cashTiyn + ticket.direction() * cash,
            revenueTiyn = report.revenueTiyn + ticket.direction() * total
        )
    }

    /** Окончательный итог — `consider`: позиции без скидок и с наценками, число — позиций. */
    private fun final(goods: Tally, discounts: Tally, markups: Tally) =
        Tally(goods.count, goods.sumTiyn - discounts.sumTiyn + markups.sumTiyn)

    /** Строка чека: товар и модификатор — +1 и сумма, сторно — +0 и минус сумма. */
    private fun line(item: TicketRequest.Item): Tally = when (item.type) {
        ItemTypeEnum.ITEM_TYPE_COMMODITY -> Tally(1, tiyn(checkNotNull(item.commodity).sum))
        ItemTypeEnum.ITEM_TYPE_STORNO_COMMODITY -> Tally(0, -tiyn(checkNotNull(item.storno_commodity).sum))
        ItemTypeEnum.ITEM_TYPE_DISCOUNT -> Tally(1, tiyn(checkNotNull(item.discount).sum))
        ItemTypeEnum.ITEM_TYPE_STORNO_DISCOUNT -> Tally(0, -tiyn(checkNotNull(item.storno_discount).sum))
        ItemTypeEnum.ITEM_TYPE_MARKUP -> Tally(1, tiyn(checkNotNull(item.markup).sum))
        ItemTypeEnum.ITEM_TYPE_STORNO_MARKUP -> Tally(0, -tiyn(checkNotNull(item.storno_markup).sum))
    }

    private fun tickets(was: TicketTally?, ticket: TicketRequest): TicketTally {
        val base = was ?: TicketTally(0, 0, 0, emptyMap(), 0, 0, 0, 0)
        val payments = ticket.payments.fold(base.payments) { acc, p -> acc.plus(p.type, Tally(1, tiyn(p.sum))) }
        val amounts = ticket.amounts
        return TicketTally(
            totalCount = base.totalCount + 1,
            count = base.count + 1,
            sumTiyn = base.sumTiyn + tiyn(amounts.total),
            payments = payments,
            offlineCount = base.offlineCount + if (ticket.offline_ticket_number != null) 1 else 0,
            discountTiyn = base.discountTiyn + (amounts.discount?.let { tiyn(it.sum) } ?: 0L),
            markupTiyn = base.markupTiyn + (amounts.markup?.let { tiyn(it.sum) } ?: 0L),
            changeTiyn = base.changeTiyn + (amounts.change?.let(::tiyn) ?: 0L)
        )
    }

    private operator fun Tally.plus(other: Tally) = Tally(count + other.count, sumTiyn + other.sumTiyn)

    /** Строка [key] пополняется на [add]; без добавки строка не заводится — как у референса. */
    private fun <K> Map<K, Tally>.plus(key: K, add: Tally?): Map<K, Tally> =
        if (add == null) this else this + (key to (this[key]?.plus(add) ?: add))

    private val NONE = Tally(0, 0)
    private val CASH = PaymentTypeEnum.PAYMENT_CASH
}
