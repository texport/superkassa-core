package io.github.texport.superkassa.core.domain.api.model.common

import kotlinx.serialization.Serializable

/**
 * Точное десятичное число: цена, количество, ставка, сумма.
 *
 * Деньги и количества приходят из внешнего мира десятичной записью — «150.55»,
 * «0.125», «12.5». В `Double` такая запись не помещается: 150.55 хранится как
 * 150.54999999999998, и после умножения на количество и деления на сто
 * расхождение доезжает до тиына. Число хранится так же, как записано:
 * целым [unscaled] и числом знаков после запятой [scale].
 *
 * @property unscaled Значащая часть без запятой: у «150.55» это 15055.
 * @property scale Число знаков после запятой: у «150.55» это 2.
 */
@Serializable(with = DecimalSerializer::class)
data class Decimal(val unscaled: Long, val scale: Int) : Comparable<Decimal> {

    init {
        require(scale in 0..MAX_SCALE) { "scale must be in 0..$MAX_SCALE, got $scale" }
    }

    /** Знак числа: -1, 0 или 1. */
    val signum: Int get() = unscaled.compareTo(0L)

    /**
     * Пересчитывает число в доли, которых [targetScale] в единице.
     *
     * Лишние знаки округляются к ближайшему, половина — от нуля: так считает
     * касса при выделении налога и при скидке в процентах.
     *
     * @param targetScale требуемое число знаков после запятой.
     * @return значение в долях: у «150.55» при `targetScale = 2` это 15055.
     */
    fun scaled(targetScale: Int): Long {
        if (targetScale >= scale) return unscaled * pow10(targetScale - scale)
        val divisor = pow10(scale - targetScale)
        return roundedDiv(unscaled, divisor)
    }

    /**
     * Сравнение — по значению: «150.0» меньше «150.01» и равно «150.00».
     *
     * Числа приводятся к общему числу знаков, а не к плавающей точке:
     * проверка «сумма не меньше нуля» не имеет права зависеть от округления.
     */
    override fun compareTo(other: Decimal): Int {
        val common = maxOf(scale, other.scale)
        return scaled(common).compareTo(other.scaled(common))
    }

    /**
     * Равенство — по значению, а не по записи: «150.0» и «150.00» — одно число.
     *
     * Одна и та же сумма приходит с разным числом знаков от кассы, от ОФД
     * и из справочника; сравнение записей рассорило бы их между собой.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val that = other as? Decimal ?: return false
        return normalized() == that.normalized()
    }

    override fun hashCode(): Int = normalized().let { 31 * it.first.hashCode() + it.second }

    /** Число без хвостовых нулей: пара «значащая часть, число знаков». */
    private fun normalized(): Pair<Long, Int> {
        var value = unscaled
        var digits = scale
        while (digits > 0 && value % 10L == 0L) {
            value /= 10L
            digits--
        }
        return Pair(value, digits)
    }

    /** Десятичная запись числа: «150.55», «-0.125», «12». */
    override fun toString(): String {
        if (scale == 0) return unscaled.toString()
        val sign = if (unscaled < 0) "-" else ""
        val digits = kotlin.math.abs(unscaled).toString().padStart(scale + 1, '0')
        return sign + digits.dropLast(scale) + "." + digits.takeLast(scale)
    }

    companion object {
        /** Больше знаков не бывает ни у количества, ни у ставки. */
        const val MAX_SCALE: Int = 9

        /** Ноль. */
        val ZERO: Decimal = Decimal(0L, 0)

        /**
         * Разбирает десятичную запись.
         *
         * @param text запись вида «150.55», «-0.125», «12», «1e2» не принимается.
         * @return разобранное число.
         * @throws IllegalArgumentException если запись не десятичная.
         */
        fun parse(text: String): Decimal {
            val trimmed = text.trim()
            require(trimmed.isNotEmpty()) { "empty decimal" }
            val negative = trimmed.startsWith('-')
            val body = trimmed.removePrefix("-").removePrefix("+")
            val point = body.indexOf('.')
            val digits = if (point < 0) body else body.substring(0, point) + body.substring(point + 1)
            val scale = if (point < 0) 0 else body.length - point - 1
            require(digits.isNotEmpty() && digits.all { it.isDigit() }) { "not a decimal: $text" }
            require(scale <= MAX_SCALE) { "too many fraction digits: $text" }
            val value = digits.toLong()
            return Decimal(if (negative) -value else value, scale)
        }

        /** Собирает число из долей: `ofScaled(15055, 2)` — это «150.55». */
        fun ofScaled(value: Long, scale: Int): Decimal = Decimal(value, scale)

        /** Делит с округлением к ближайшему, половина — от нуля. */
        internal fun roundedDiv(value: Long, divisor: Long): Long {
            val half = divisor / 2
            return if (value >= 0) (value + half) / divisor else (value - half) / divisor
        }

        private fun pow10(power: Int): Long {
            var result = 1L
            repeat(power) { result *= 10 }
            return result
        }
    }
}
