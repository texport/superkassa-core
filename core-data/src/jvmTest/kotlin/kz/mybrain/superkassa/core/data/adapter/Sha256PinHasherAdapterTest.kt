package kz.mybrain.superkassa.core.data.adapter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class Sha256PinHasherAdapterTest {
    private val adapter = Sha256PinHasherAdapter()

    @Test
    fun testHashSuccess() {
        val pin = "1234"
        val hash = adapter.hash(pin)
        assertNotNull(hash)
        // SHA-256 for "1234" is "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4"
        assertEquals("03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4", hash)
    }
}
