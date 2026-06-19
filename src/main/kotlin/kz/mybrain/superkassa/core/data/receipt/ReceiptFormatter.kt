package kz.mybrain.superkassa.core.data.receipt

import kz.mybrain.superkassa.core.domain.model.Money
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object ReceiptFormatter {

    fun moneyToCents(m: Money): Long {
        return m.bills * 100L + m.coins
    }

    fun formatCents(cents: Long): String {
        return String.format(Locale.US, "%.2f", cents / 100.0)
    }

    fun formatMoney(m: Money): String {
        return formatCents(moneyToCents(m))
    }

    fun formatDate(epochMillis: Long): String {
        val instant = Instant.ofEpochMilli(epochMillis)
        val zoned = instant.atZone(ZoneId.systemDefault())
        return zoned.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))
    }

    fun escape(s: String): String =
        s.replace("&", "&amp;")
         .replace("<", "&lt;")
         .replace(">", "&gt;")
         .replace("\"", "&quot;")
}
