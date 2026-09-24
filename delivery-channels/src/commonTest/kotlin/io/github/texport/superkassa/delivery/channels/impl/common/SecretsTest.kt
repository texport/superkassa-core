package io.github.texport.superkassa.delivery.channels.impl.common

import kotlin.test.Test
import kotlin.test.assertEquals

class SecretsTest {

    @Test
    fun `известный ключ прячется и как есть, и в виде параметра адреса`() {
        val secrets = Secrets(listOf("12:AB/c d", null, " "))

        assertEquals("x *** y", secrets.mask("x 12:AB/c d y"))
        assertEquals("x *** y", secrets.mask("x 12%3AAB%2Fc%20d y"))
    }

    @Test
    fun `ключ по месту в тексте прячется и без знания его значения`() {
        val secrets = Secrets(emptyList())

        assertEquals("https://api.telegram.org/bot***/sendMessage", secrets.mask("https://api.telegram.org/bot1:AA/sendMessage"))
        assertEquals("https://sms.test/send?api_key=***&phone=777", secrets.mask("https://sms.test/send?api_key=k1&phone=777"))
        assertEquals("token=*** key=*** access_token=***", secrets.mask("token=a1 key=b2 access_token=c3"))
        assertEquals("Authorization: Bearer ***", secrets.mask("Authorization: Bearer abc"))
        assertEquals("nothing to hide", secrets.mask("nothing to hide"))
    }

    @Test
    fun `род отказа называет и причину`() {
        assertEquals("IllegalStateException", reasonOf(IllegalStateException("x")))
        assertEquals("IllegalStateException (ArithmeticException)", reasonOf(IllegalStateException("x", ArithmeticException())))
    }
}
