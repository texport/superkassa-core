package io.github.texport.superkassa.core.domain.strings

import io.github.texport.superkassa.core.string.api.TrilingualMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Обёртка причины в трёхъязычное сообщение.
 *
 * Причина от ОФД часто приходит уже трёхъязычной строкой. Пока её
 * вставляли целиком в каждый из трёх языков, за несколько повторов
 * сообщение вырастало в стену, где каждый язык нёс в себе все три:
 * на стенде такая строка накопилась за 530 попыток одной задачи.
 */
class TrilingualWrappingTest {

    @Test
    fun `трёхъязычная причина раскладывается по языкам`() {
        val cause = TrilingualMessage(ru = "нет полей", kk = "өрістер жоқ", en = "no fields").compact()

        val wrapped = template().wrapping(cause)

        assertEquals("Отказ: нет полей", wrapped.ru)
        assertEquals("Бас тарту: өрістер жоқ", wrapped.kk)
        assertEquals("Refused: no fields", wrapped.en)
    }

    @Test
    fun `обычная причина попадает во все три языка как есть`() {
        val wrapped = template().wrapping("timeout")

        assertEquals("Отказ: timeout", wrapped.ru)
        assertEquals("Бас тарту: timeout", wrapped.kk)
        assertEquals("Refused: timeout", wrapped.en)
    }

    @Test
    fun `повторная обёртка не удваивает языки`() {
        val once = template().wrapping("timeout")

        val twice = template().wrapping(once.compact())

        assertEquals("Отказ: Отказ: timeout", twice.ru)
        assertEquals("Бас тарту: Бас тарту: timeout", twice.kk)
    }

    @Test
    fun `не трёхъязычная строка не разбирается`() {
        assertNull(TrilingualMessage.ofCompact("просто текст"))
    }

    private fun template() = TrilingualMessage(
        ru = "Отказ: ${TrilingualMessage.PLACEHOLDER}",
        kk = "Бас тарту: ${TrilingualMessage.PLACEHOLDER}",
        en = "Refused: ${TrilingualMessage.PLACEHOLDER}"
    )
}
