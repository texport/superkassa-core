package io.github.texport.superkassa.core.domain.usecase.auth

import io.github.texport.superkassa.core.domain.api.model.auth.PinHashes
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Сравнение хешей за постоянное время отвечает так же, как обычное равенство. */
class PinHashesTest {
    @Test
    fun `равные хеши совпадают, отличие в любом месте и в длине — нет`() {
        assertTrue(PinHashes.same("a1b2c3", "a1b2c3"))
        assertTrue(PinHashes.same("", ""))
        assertFalse(PinHashes.same("a1b2c3", "b1b2c3"))
        assertFalse(PinHashes.same("a1b2c3", "a1b2c4"))
        assertFalse(PinHashes.same("a1b2c3", "a1b2c"))
        assertFalse(PinHashes.same("a1b2c", "a1b2c3"))
        assertFalse(PinHashes.same("abc", "abc\u0000"))
    }
}
