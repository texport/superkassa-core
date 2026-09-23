package io.github.texport.superkassa.core.domain.api.model.common

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
    /**
     * Сумма целиком в тиынах.
     *
     * Тиын — наименьшая доля тенге, и только в них сумму можно хранить и
     * складывать без потерь. Раньше в журнал и в счётчики шли одни целые
     * тенге, и с каждого чека терялось до тиына: остаток денежного ящика
     * расходился с настоящим, а расхождение уходило в Z-отчёт.
     *
     * Метод, а не свойство: чек хранится сериализованным по бинам, и
     * вычисляемое свойство попадало в сохранённый JSON лишним полем.
     * Обратное чтение такого чека падало, и чек навсегда оставался
     * в очереди — не уходил в ОФД и не получал фискального признака.
     */
    fun tiyn(): Long = bills * TIYN_IN_TENGE + coins

    /**
     * Сумма словами сообщения: тенге, точка, два знака тиына.
     *
     * Нужна там, где сумма попадает в текст отказа кассиру. Печатью
     * и разметкой экрана она не управляет — там свои правила показа,
     * а здесь важно, чтобы в сообщении стояло «700.00», а не то, как
     * устроен тип.
     */
    fun asTenge(): String = "$bills." + coins.toString().padStart(TIYN_DIGITS, '0')

    companion object {
        /** Тиынов в тенге. */
        const val TIYN_IN_TENGE: Long = 100

        /** Знаков в дробной части: тиын — сотая доля тенге. */
        private const val TIYN_DIGITS: Int = 2

        /** Собирает сумму из тиынов. */
        fun fromTiyn(total: Long): Money =
            Money(bills = total / TIYN_IN_TENGE, coins = (total % TIYN_IN_TENGE).toInt())

        /**
         * Собирает сумму из десятичной записи в тенге.
         *
         * Доли тиына округляются к ближайшему: меньше тиына касса выдать
         * не может, а отбрасывание уводило бы остаток ящика вниз с каждого чека.
         *
         * @param amount сумма в тенге, например «1234.56».
         * @return сумма с bills = 1234 и coins = 56.
         */
        fun fromTenge(amount: Decimal): Money = fromTiyn(amount.scaled(TIYN_SCALE))

        /**
         * Сумма строки чека: цена × количество, к ближайшему тиыну.
         *
         * Половина тиына идёт вверх: 333,33 × 1,5 = 499,995 — это 500,00.
         * Правило одно для всех, кто считает строку: экран, чек и запрос
         * в БФД. Касса, усекавшая долю, получала 499,99 там, где ядро
         * считало 500,00, и чек отвергался несходящейся оплатой.
         *
         * @param price цена за единицу в тенге.
         * @param quantity количество; учитываются тысячные доли.
         */
        fun lineSum(price: Decimal, quantity: Decimal): Money = fromTiyn(
            Decimal.roundedDiv(price.scaled(TIYN_SCALE) * quantity.scaled(QUANTITY_SCALE), THOUSANDTHS_IN_ONE)
        )

        /** Знаков после запятой у тенге. */
        private const val TIYN_SCALE: Int = 2

        /** Количество хранится в тысячных долях единицы. */
        private const val QUANTITY_SCALE: Int = 3

        /** Тысячных в единице. */
        private const val THOUSANDTHS_IN_ONE: Long = 1_000
    }
}
