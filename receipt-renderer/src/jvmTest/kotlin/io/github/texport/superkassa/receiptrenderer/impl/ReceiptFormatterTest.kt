package io.github.texport.superkassa.receiptrenderer.impl

import io.github.texport.superkassa.core.domain.api.model.common.*



import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.test.Test
import kotlin.test.assertEquals

/** Неразрывный пробел: им форма разделяет разряды и отделяет знак тенге. */
private const val NBSP = ' '

class ReceiptFormatterTest {

    @Test
    fun testMoneyToTiyn() {
        assertEquals(10050L, ReceiptFormatter.moneyToTiyn(Money(100, 50)))
        assertEquals(0L, ReceiptFormatter.moneyToTiyn(Money(0, 0)))
        assertEquals(99L, ReceiptFormatter.moneyToTiyn(Money(0, 99)))
    }

    @Test
    fun testFormatTiyn() {
        assertEquals("100,50$NBSP₸", ReceiptFormatter.formatTiyn(10050L))
        assertEquals("0,00$NBSP₸", ReceiptFormatter.formatTiyn(0L))
        assertEquals("0,09$NBSP₸", ReceiptFormatter.formatTiyn(9L))
    }

    /**
     * Сумма на чеке читается так же, как на экране кассы: разряды разделены,
     * дробная часть после запятой, знак тенге на месте. Прежде на ленте
     * стояло «1234567.89» — покупатель у кассы разбирал такое число по цифрам.
     */
    @Test
    fun testFormatTiynSeparatesGroups() {
        assertEquals("1${NBSP}234${NBSP}567,89$NBSP₸", ReceiptFormatter.formatTiyn(123456789L))
        assertEquals("999,99$NBSP₸", ReceiptFormatter.formatTiyn(99999L))
        assertEquals("1${NBSP}000,00$NBSP₸", ReceiptFormatter.formatTiyn(100000L))
    }

    /**
     * Знак минуса не теряется на сумме меньше тенге: «-0,50 ₸», а не «0,50 ₸».
     * На возврате мелочи форма показывала сумму со знаком плюс.
     */
    @Test
    fun testFormatTiynKeepsSignBelowOneTenge() {
        assertEquals("-0,50$NBSP₸", ReceiptFormatter.formatTiyn(-50L))
        assertEquals("-0,09$NBSP₸", ReceiptFormatter.formatTiyn(-9L))
        assertEquals("-5${NBSP}000,00$NBSP₸", ReceiptFormatter.formatTiyn(-500000L))
    }

    @Test
    fun testFormatMoney() {
        assertEquals("123,45$NBSP₸", ReceiptFormatter.formatMoney(Money(123, 45)))
    }

    /** Количество без дробной части печатается целым, хвостовые нули не печатаются. */
    @Test
    fun testFormatQuantity() {
        assertEquals("1", ReceiptFormatter.formatQuantity(1000L))
        assertEquals("2,5", ReceiptFormatter.formatQuantity(2500L))
        assertEquals("2,005", ReceiptFormatter.formatQuantity(2005L))
        assertEquals("0", ReceiptFormatter.formatQuantity(0L))
        assertEquals("-1,5", ReceiptFormatter.formatQuantity(-1500L))
    }

    /**
     * Большое дробное количество печатается числом, а не показательной записью:
     * перевод через `Double` выводил на ленту «1.00000005E7».
     */
    @Test
    fun testFormatQuantityStaysDecimal() {
        assertEquals("10${NBSP}000${NBSP}000,5", ReceiptFormatter.formatQuantity(10_000_000_500L))
        assertEquals("1${NBSP}000${NBSP}000,001", ReceiptFormatter.formatQuantity(1_000_000_001L))
    }

    /**
     * Дробная часть количества не округляется по дороге: `Double` не хранит
     * тысячные больших чисел, и последний знак терялся.
     */
    @Test
    fun testFormatQuantityKeepsThousandths() {
        assertEquals("9${NBSP}007${NBSP}199${NBSP}254${NBSP}740,993", ReceiptFormatter.formatQuantity(9_007_199_254_740_993L))
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
