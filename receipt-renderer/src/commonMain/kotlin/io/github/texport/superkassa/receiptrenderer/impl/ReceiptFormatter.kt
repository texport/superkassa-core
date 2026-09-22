package io.github.texport.superkassa.receiptrenderer.impl

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.domain.api.model.common.Money
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Утилита форматирования чеков и денежных сумм для казахской/русской/английской разметки.
 *
 * Здесь объявлен весь набор знаков, которыми форма набирает числа: разделитель
 * разрядов, дробный разделитель и знак валюты. Сумму на ленте читает человек,
 * стоящий у кассы, и читает он её в том же виде, в каком её показывает экран.
 */
internal object ReceiptFormatter {

    /** Разделитель разрядов: неразрывный пробел, чтобы «1 234» не разорвало по строкам. */
    private const val GROUP_SEPARATOR = ' '

    /** Дробный разделитель суммы и количества. */
    private const val FRACTION_SEPARATOR = ','

    /** Знак валюты: тенге. */
    private const val CURRENCY_SIGN = "₸"

    /** Цифр в разряде. */
    private const val GROUP_SIZE = 3

    /** Знаков после запятой у суммы: тиын — сотая доля тенге. */
    private const val TIYN_SCALE = 2

    /** Знаков после запятой у количества: оно приходит в тысячных долях. */
    private const val QUANTITY_SCALE = 3

    /**
     * Конвертирует денежную структуру [Money] в общее количество тиын.
     *
     * @param m денежная сумма
     * @return общее количество тиын в виде [Long]
     */
    fun moneyToTiyn(m: Money): Long = m.tiyn()

    /**
     * Форматирует количество тиын в сумму на форме: «1 234 567,89 ₸».
     *
     * @param tiyn сумма в тиынах
     * @return форматированная строка с денежной суммой
     */
    fun formatTiyn(tiyn: Long): String =
        decimalOnForm(Decimal.ofScaled(tiyn, TIYN_SCALE)) + GROUP_SEPARATOR + CURRENCY_SIGN

    /**
     * Форматирует денежную структуру [Money] в сумму на форме.
     *
     * @param m денежная сумма
     * @return форматированная строка с денежной суммой
     */
    fun formatMoney(m: Money): String = formatTiyn(moneyToTiyn(m))

    /**
     * Форматирует временную метку в миллисекундах в стандартную строку даты и времени формата dd.MM.yyyy HH:mm:ss.
     *
     * @param epochMillis метка времени в миллисекундах
     * @return форматированная строка даты и времени в текущей системной таймзоне
     */
    @Suppress("DEPRECATION")
    fun formatDate(epochMillis: Long): String {
        val instant = Instant.fromEpochMilliseconds(epochMillis)
        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val day = localDateTime.day.toString().padStart(2, '0')
        val month = (localDateTime.month.ordinal + 1).toString().padStart(2, '0')
        val year = localDateTime.year
        val hour = localDateTime.hour.toString().padStart(2, '0')
        val minute = localDateTime.minute.toString().padStart(2, '0')
        val second = localDateTime.second.toString().padStart(2, '0')
        return "$day.$month.$year $hour:$minute:$second"
    }

    /**
     * Экранирует HTML специальные символы в переданной строке.
     *
     * @param s исходная строка
     * @return экранированная HTML-строка
     */
    fun escape(s: String): String =
        s.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")

    /**
     * Форматирует количество товара (переданное в тысячных долях) в читаемый вид.
     * Например:
     * 1000 -> "1"
     * 2500 -> "2,5"
     * 2005 -> "2,005"
     *
     * Тысячные — точное десятичное число, а не доля от деления: перевод
     * через `Double` печатал большое дробное количество показательной
     * записью «1.00000005E7» и терял последнюю тысячную.
     */
    fun formatQuantity(quantityThousandths: Long): String =
        decimalOnForm(Decimal.ofScaled(quantityThousandths, QUANTITY_SCALE), dropTrailingZeros = true)

    /**
     * Десятичная запись числа так, как её набирает форма: разряды целой части
     * разделены, дробная часть отделена запятой.
     *
     * Знак берётся у самой записи: у суммы меньше тенге целая часть — ноль,
     * и по ней минуса не видно.
     *
     * @param value точное десятичное число
     * @param dropTrailingZeros убрать незначащие нули дробной части
     * @return запись числа без знака валюты
     */
    private fun decimalOnForm(value: Decimal, dropTrailingZeros: Boolean = false): String {
        val plain = value.toString()
        val sign = if (plain.startsWith('-')) "-" else ""
        val digits = plain.removePrefix("-")
        val point = digits.indexOf('.')
        val whole = if (point < 0) digits else digits.substring(0, point)
        val rawFraction = if (point < 0) "" else digits.substring(point + 1)
        val fraction = if (dropTrailingZeros) rawFraction.trimEnd('0') else rawFraction
        val tail = if (fraction.isEmpty()) "" else "$FRACTION_SEPARATOR$fraction"
        return sign + groupDigits(whole) + tail
    }

    /** Разбивает целую часть на разряды по три цифры справа налево. */
    private fun groupDigits(whole: String): String =
        whole.reversed()
            .chunked(GROUP_SIZE)
            .joinToString(GROUP_SEPARATOR.toString())
            .reversed()
}
