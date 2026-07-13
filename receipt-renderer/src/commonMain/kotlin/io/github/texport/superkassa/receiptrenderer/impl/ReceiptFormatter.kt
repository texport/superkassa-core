package io.github.texport.superkassa.receiptrenderer.impl

import io.github.texport.superkassa.core.domain.api.model.common.Money
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Утилита форматирования чеков и денежных сумм для казахской/русской/английской разметки.
 */
internal object ReceiptFormatter {

    /**
     * Конвертирует денежную структуру [Money] в общее количество тиын (центов).
     *
     * @param m денежная сумма
     * @return общее количество тиын в виде [Long]
     */
    fun moneyToCents(m: Money): Long {
        return m.bills * 100L + m.coins
    }

    /**
     * Форматирует количество тиын (центов) в строку с точкой в качестве разделителя (например, 100.50).
     *
     * @param cents количество тиын (монет/центов)
     * @return форматированная строка с денежной суммой
     */
    fun formatCents(cents: Long): String {
        val whole = cents / 100
        val fraction = cents % 100
        val absFraction = if (fraction < 0) -fraction else fraction
        val fractionStr = if (absFraction < 10) "0$absFraction" else "$absFraction"
        return "$whole.$fractionStr"
    }

    /**
     * Форматирует денежную структуру [Money] в строку с точкой в качестве разделителя.
     *
     * @param m денежная сумма
     * @return форматированная строка с денежной суммой
     */
    fun formatMoney(m: Money): String {
        return formatCents(moneyToCents(m))
    }

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
     * 2500 -> "2.5"
     * 2005 -> "2.005"
     */
    fun formatQuantity(quantityThousandths: Long): String {
        val qtyDouble = quantityThousandths / 1000.0
        if (quantityThousandths % 1000L == 0L) {
            return (quantityThousandths / 1000L).toString()
        }
        var str = qtyDouble.toString()
        if (str.contains('.')) {
            while (str.endsWith('0')) {
                str = str.substring(0, str.length - 1)
            }
            if (str.endsWith('.')) {
                str = str.substring(0, str.length - 1)
            }
        }
        return str
    }
}
