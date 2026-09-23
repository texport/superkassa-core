package io.github.texport.superkassa.core.domain.impl.helper.tax

/**
 * Доля `value × numerator / denominator` в целых тиынах, половина — к чётному.
 *
 * Так БФД масштабирует оборот ставки под скидку или наценку на чек
 * (`OperationCalculator.processFraction`, `RoundingMode.HALF_EVEN`), и доля
 * у кассы должна округляться тем же правилом, иначе оборот в Z расходится
 * с БФД на тиын.
 *
 * Произведение двух сумм чека в `Long` переполняется уже на десятках
 * миллионов тенге, поэтому деление идёт по разрядам множителя: частное
 * и остаток держатся раздельно и остаток всегда меньше делителя.
 *
 * @param value неотрицательная сумма в тиынах.
 * @param numerator неотрицательный множитель.
 * @param denominator положительный делитель.
 */
internal fun proportionalShare(value: Long, numerator: Long, denominator: Long): Long {
    require(value >= 0 && numerator >= 0 && denominator > 0) { "share of negative amounts is undefined" }
    val stepQuotient = numerator / denominator
    val stepRemainder = numerator % denominator
    var quotient = 0L
    var remainder = 0L
    for (bit in HIGHEST_BIT downTo 0) {
        quotient *= 2
        remainder *= 2
        if (remainder >= denominator) {
            quotient += 1
            remainder -= denominator
        }
        if ((value shr bit) and 1L == 1L) {
            quotient += stepQuotient
            remainder += stepRemainder
            if (remainder >= denominator) {
                quotient += 1
                remainder -= denominator
            }
        }
    }
    return quotient + halfEvenCarry(quotient, remainder, denominator)
}

private fun halfEvenCarry(quotient: Long, remainder: Long, denominator: Long): Long {
    val twice = remainder * 2
    return when {
        twice > denominator -> 1L
        twice == denominator -> quotient and 1L
        else -> 0L
    }
}

/** Старший значащий разряд неотрицательного `Long`. */
private const val HIGHEST_BIT = 62
