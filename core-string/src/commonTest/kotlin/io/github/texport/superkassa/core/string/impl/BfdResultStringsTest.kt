package io.github.texport.superkassa.core.string.impl

import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.string.api.TrilingualMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Отказ БФД кассир читает на своём языке: в каждом поле один язык, без
 * номера кода и без английских технических слов, и есть что сделать.
 */
class BfdResultStringsTest {

    @Test
    fun `каждый код протокола назван словами, на каждом языке своим текстом`() {
        (PROTOCOL_CODES + UNKNOWN_CODE).forEach { code -> assertCashierText(CoreStrings.bfdRefusal(code), "code $code") }
    }

    @Test
    fun `нет ответа и неотправленный запрос тоже сказаны словами`() {
        assertCashierText(CoreStrings.bfdNoAnswer(), "no answer")
        assertCashierText(CoreStrings.bfdRequestNotSent(), "not sent")
    }

    @Test
    fun `у каждого кода своя причина, неизвестный код - общими словами`() {
        assertEquals(PROTOCOL_CODES.size, PROTOCOL_CODES.map { CoreStrings.bfdRefusal(it).ru }.toSet().size)
        assertEquals(CoreStrings.bfdRefusal(UNKNOWN_CODE), CoreStrings.bfdRefusal(OTHER_UNKNOWN_CODE))
    }

    @Test
    fun `причина отклонённого документа берётся из той же таблицы`() {
        assertEquals(CoreStrings.bfdRefusal(INCORRECT_REQUEST_DATA), CoreStrings.documentFailedReason(INCORRECT_REQUEST_DATA))
    }

    private fun assertCashierText(message: TrilingualMessage, case: String) {
        assertTrue(message.ru.none { it in LATIN || it in KAZAKH }, "$case: ru has another language: ${message.ru}")
        assertTrue(message.kk.none { it in LATIN } && message.kk.any { it in KAZAKH }, "$case: kk is not Kazakh: ${message.kk}")
        assertTrue(message.en.none { it in CYRILLIC }, "$case: en has another language: ${message.en}")
        listOf(message.ru, message.kk, message.en).forEach { text ->
            assertTrue(text.none(Char::isDigit), "$case: technical number in the cashier text: $text")
            assertTrue(TECHNICAL.none { text.contains(it, ignoreCase = true) }, "$case: technical text: $text")
        }
    }

    private companion object {
        val PROTOCOL_CODES = (1..9) + (11..15) + (17..19) + listOf(254, 255)
        const val UNKNOWN_CODE = 16
        const val OTHER_UNKNOWN_CODE = 42
        const val INCORRECT_REQUEST_DATA = 13

        /** Латиница, кроме Z: смена закрывается Z-отчётом и на русском, и на казахском. */
        val LATIN = ('a'..'z') + ('A'..'Y')
        val CYRILLIC = ('а'..'я') + ('А'..'Я') + "ёЁәіңғүұқөһӘІҢҒҮҰҚӨҺ".toList()
        val KAZAKH = "әіңғүұқөһӘІҢҒҮҰҚӨҺ".toList()
        val TECHNICAL = listOf("RU:", "KK:", "EN:", "timeout", "code", "RESULT_TYPE")
    }
}
