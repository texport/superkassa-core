package io.github.texport.superkassa.core.domain.impl.usecase.receipt

import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptItem
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.impl.helper.tax.TaxCalculator
import io.github.texport.superkassa.core.domain.impl.helper.tax.proportionalShare

/**
 * Ставка возврата суммой — ставка чека-основания, а не кассы.
 *
 * Возврат суммой приходит одной строкой без ставки, и прежде она облагалась
 * ставкой кассы: продажа без НДС и возврат её суммой давали у БФД налог,
 * которого в продаже не было. Строка без ставки получает ставку основания;
 * если в основании несколько ставок, строка делится между ними в той же
 * пропорции, в какой они были в основании.
 *
 * Строки со своей ставкой — возврат по отмеченным позициям — не трогаются.
 *
 * @param items позиции возврата.
 * @param basis сохранённый чек-основание.
 */
internal fun withBasisVat(items: List<ReceiptItem>, basis: ReceiptRequest): List<ReceiptItem> {
    val turnovers = turnoversByRate(basis)
    if (turnovers.isEmpty()) return items
    return items.flatMap { item ->
        when {
            item.vatGroup != null -> listOf(item)
            turnovers.size == 1 || item.discount != null || item.markup != null ->
                listOf(item.copy(vatGroup = turnovers.maxBy { it.second }.first))
            else -> split(item, turnovers)
        }
    }
}

/**
 * НДС возврата суммой по чеку-основанию.
 *
 * Основание с НДС на весь чек возвращается так же — ставкой основания на
 * весь чек. Основание с НДС по позициям раскладывает строки возврата по
 * своим ставкам ([withBasisVat]).
 *
 * @param items позиции возврата.
 * @param basis сохранённый чек-основание.
 */
internal fun basisVat(items: List<ReceiptItem>, basis: ReceiptRequest): VatScope {
    val whole = basis.vatGroup
    if (whole != null && items.none { it.vatGroup != null }) return VatScope(items, whole)
    return VatScope(withBasisVat(items, basis), null)
}

/** Оборот основания по ставкам, с налогом; то, что не обложено, — «без НДС». */
private fun turnoversByRate(basis: ReceiptRequest): List<Pair<VatGroup, Long>> {
    val taxed = TaxCalculator().calculate(basis).ticketTaxes
        .map { it.vatGroup to it.taxBase.tiyn() + it.taxSum.tiyn() }
    val untaxed = basis.total.tiyn() - taxed.sumOf { it.second }
    return (taxed + (VatGroup.NO_VAT to untaxed)).filter { it.second > 0 }
}

/** Строка суммой, разложенная по ставкам основания; остаток округления — самой крупной доле. */
private fun split(item: ReceiptItem, turnovers: List<Pair<VatGroup, Long>>): List<ReceiptItem> {
    val whole = turnovers.sumOf { it.second }
    val sum = item.sum.tiyn()
    val parts = turnovers.map { (group, turnover) -> group to proportionalShare(sum, turnover, whole) }
    val largest = parts.indices.maxBy { parts[it].second }
    val drift = sum - parts.sumOf { it.second }
    return parts.mapIndexedNotNull { index, (group, part) ->
        val tiyn = if (index == largest) part + drift else part
        if (tiyn <= 0L) return@mapIndexedNotNull null
        val money = Money.fromTiyn(tiyn)
        item.copy(vatGroup = group, price = money, sum = money, quantity = ONE_UNIT)
    }
}

/** Одна единица в тысячных долях: строка суммой — это «1 × сумма». */
private const val ONE_UNIT: Long = 1_000
