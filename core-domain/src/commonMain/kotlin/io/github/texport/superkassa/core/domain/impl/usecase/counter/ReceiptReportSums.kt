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

    /**
     * Число позиций для строк операций, отделов и итога: сторно не считается.
     *
     * БФД считает в этих строках позиции, а не чеки (`OperationCalculator.getTotal`,
     * `extractSections`: +1 за товар, сторно — +0). Прежде касса ставила
     * сюда единицу за чек, и чек на три товара расходился с БФД на два.
     */
    val goods: Long = request.items.count { !it.isStorno }.toLong()

    /**
     * Число скидок: на чек и на каждую позицию отдельно, сторно не считается —
     * как `OperationCalculator.updateDiscounts`. Прежде касса брала сюда
     * число операций, и чек без скидки показывал в отчёте одну скидку.
     */
    val discountCount: Long = request.modifiers(request.discount, ReceiptItem::discount)

    /** Число наценок — по тем же правилам, что и скидок (`updateMarkups`). */
    val markupCount: Long = request.modifiers(request.markup, ReceiptItem::markup)

    /** Вклад позиции [item] в отдел: сумма до её скидки и наценки, сторно — с минусом. */
    fun section(item: ReceiptItem): Long = item.signed(item.sumBeforeModifiers)
}

/** Модификатор на чек и на каждую несторнированную позицию — по штуке. */
private fun ReceiptRequest.modifiers(onReceipt: Money?, onItem: (ReceiptItem) -> Money?): Long {
    val receipt = if (onReceipt.isPositive()) 1L else 0L
    return receipt + items.count { !it.isStorno && onItem(it).isPositive() }
}

private fun Money?.isPositive(): Boolean = this != null && tiyn() > 0L

/** Сумма [value] позиции со знаком: сторно её вычитает. */
private fun ReceiptItem.signed(value: Money?): Long {
    val tiyn = value?.tiyn() ?: 0L
    return if (isStorno) -tiyn else tiyn
}
