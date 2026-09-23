package io.github.texport.superkassa.core.domain.impl.usecase.receipt

import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.string.api.CoreStrings

/**
 * Границы чисел в чеке.
 *
 * Прежде их проверял только узел — аннотациями запроса и проверяющим
 * Spring. Касса в приложении ядро зовёт напрямую, и без этих правил
 * пропускала скидку в 150 %, отрицательную наценку и позицию по нулевой
 * цене. Правило живёт здесь, одно для узла и для приложения.
 *
 * @throws ValidationException с кодом `RECEIPT_VALUE_OUT_OF_RANGE`.
 */
internal fun requireReceiptWithinBounds(command: CreateReceiptCommand) {
    command.items.forEachIndexed { index, item ->
        val at = "items[$index]"
        atLeast(item.price, MIN_PRICE, "$at.price")
        within(item.quantity, ZERO, MAX_QUANTITY, "$at.quantity", exclusiveMin = true)
        percent(item.discountPercent, "$at.discountPercent")
        percent(item.markupPercent, "$at.markupPercent")
        atLeast(item.discountSum, ZERO, "$at.discountSum")
        atLeast(item.markupSum, ZERO, "$at.markupSum")
    }
    percent(command.discountPercent, "discountPercent")
    percent(command.markupPercent, "markupPercent")
    atLeast(command.discountSum, ZERO, "discountSum")
    atLeast(command.markupSum, ZERO, "markupSum")
    atLeast(command.taken, ZERO, "taken")
    command.payments.forEachIndexed { index, payment -> atLeast(payment.sum, ZERO, "payments[$index].sum") }
    command.parentTicket?.let { basis ->
        if (basis.parentTicketTotal.tiyn() <= 0L) outOfRange("parentTicket.parentTicketTotal", ">= 0.01")
    }
}

private fun percent(value: Decimal?, field: String) = within(value, ZERO, HUNDRED, field, exclusiveMin = false)

private fun atLeast(value: Decimal?, min: Decimal, field: String) {
    if (value != null && value < min) outOfRange(field, ">= $min")
}

private fun within(value: Decimal?, min: Decimal, max: Decimal, field: String, exclusiveMin: Boolean) {
    if (value == null) return
    val belowMin = if (exclusiveMin) value <= min else value < min
    if (belowMin || value > max) {
        val left = if (exclusiveMin) "(" else "["
        outOfRange(field, "$left$min; $max]")
    }
}

private fun outOfRange(field: String, bounds: String): Nothing =
    throw ValidationException(CoreStrings.valueOutOfRange(field, bounds), "RECEIPT_VALUE_OUT_OF_RANGE")

private val ZERO = Decimal.ZERO
private val HUNDRED = Decimal.parse("100")
private val MIN_PRICE = Decimal.parse("0.01")
private val MAX_QUANTITY = Decimal.parse("999999999")
