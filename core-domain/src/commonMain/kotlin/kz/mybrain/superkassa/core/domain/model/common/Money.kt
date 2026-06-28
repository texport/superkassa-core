package kz.mybrain.superkassa.core.domain.model.common

import kotlinx.serialization.Serializable

/**
 * Представление денежной суммы с фиксированной точностью (соответствует формату bills/coins в ОФД).
 *
 * @property bills Целая часть суммы (тенге).
 * @property coins Дробная часть суммы (тиыны).
 */
@Serializable
data class Money(
    val bills: Long,
    val coins: Int
) {
    companion object {
        /**
         * Преобразует сумму в виде Double (тенге) в формат Money (bills/coins).
         * bills - целая часть (тенге), coins - дробная часть (тиыны, умноженные на 100).
         *
         * @param amount Сумма в тенге (например, 1234.56)
         * @return Объект [Money] с bills=1234 и coins=56
         */
        fun fromTenge(amount: Double): Money {
            val totalCoins = kotlin.math.round(amount * 100).toLong()
            val bills = totalCoins / 100
            val coins = (totalCoins % 100).toInt()
            return Money(bills = bills, coins = coins)
        }
    }
}
