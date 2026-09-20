package io.github.texport.superkassa.receiptrenderer.impl

import io.github.texport.superkassa.core.domain.api.model.common.*



import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.test.Test
import kotlin.test.assertEquals

class ReceiptFormatterTest {

    @Test
    fun testMoneyToTiyn() {
        assertEquals(10050L, ReceiptFormatter.moneyToTiyn(Money(100, 50)))
        assertEquals(0L, ReceiptFormatter.moneyToTiyn(Money(0, 0)))
        assertEquals(99L, ReceiptFormatter.moneyToTiyn(Money(0, 99)))
    }

    @Test
    fun testFormatTiyn() {
        assertEquals("100.50", ReceiptFormatter.formatTiyn(10050L))
        assertEquals("0.00", ReceiptFormatter.formatTiyn(0L))
        assertEquals("0.09", ReceiptFormatter.formatTiyn(9L))
    }

    @Test
    fun testFormatMoney() {
        assertEquals("123.45", ReceiptFormatter.formatMoney(Money(123, 45)))
    }

    @Test
    fun testFormatDate() {
        val millis = 1782200000000L // Some epoch millis
        val expected = Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))
        assertEquals(expected, ReceiptFormatter.formatDate(millis))
    }

    @Test
    fun testEscape() {
        val input = "Hello <world> & \"peace\""
        val expected = "Hello &lt;world&gt; &amp; &quot;peace&quot;"
        assertEquals(expected, ReceiptFormatter.escape(input))
    }
}
