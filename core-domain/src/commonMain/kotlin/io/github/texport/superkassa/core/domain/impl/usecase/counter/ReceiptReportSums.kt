package io.github.texport.superkassa.core.domain.impl.usecase.counter

import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptItem
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest

/**
 * Суммы чека для счётчиков X/Z — по тем правилам, по которым их складывает
 * БФД из запроса чека (`OperationCalculator`).
 *
 * Операции и отделы — без скидок и наценок: сумма позиций до них. Скидки
 * и наценки — на чек и на позиции вместе, у сторно позиции со знаком минус.
 * Итог с ними — сумма чека. Скидка и наценка в строке чеков — только на сам
 * чек. Правила одни для счётчиков, которые копит чек, и для пересчёта смены:
 * прежде у каждого была своя копия, и операции получали сумму чека, из
 * которой итог отчёта вычитал скидку второй раз.
 *
 * @param request чек.
 */
class ReceiptReportSums(private val request: ReceiptRequest) {
    /** Сумма чека: итог отчёта. */
    val total: Long = request.total.tiyn()

    /** Скидки чека и его позиций. */
    val discounts: Long = (request.discount?.tiyn() ?: 0L) + request.items.sumOf { it.signed(it.discount) }

    /** Наценки чека и его позиций. */
    val markups: Long = (request.markup?.tiyn() ?: 0L) + request.items.sumOf { it.signed(it.markup) }

    /** Операции без скидок и наценок: сумма позиций до них, как `OperationCalculator.getTotal`. */
    val operations: Long get() = request.items.sumOf(::section)

    /** Скидка на сам чек — для строки чеков. */
    val ticketDiscount: Long = request.discount?.tiyn() ?: 0L

    /** Наценка на сам чек — для строки чеков. */
    val ticketMarkup: Long = request.markup?.tiyn() ?: 0L

    /** Вклад позиции [item] в отдел: сумма до её скидки и наценки, сторно — с минусом. */
    fun section(item: ReceiptItem): Long = item.signed(item.sumBeforeModifiers)
}

/** Сумма [value] позиции со знаком: сторно её вычитает. */
private fun ReceiptItem.signed(value: Money?): Long {
    val tiyn = value?.tiyn() ?: 0L
    return if (isStorno) -tiyn else tiyn
}
