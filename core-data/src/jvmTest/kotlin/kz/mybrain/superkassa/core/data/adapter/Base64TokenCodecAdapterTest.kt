package kz.mybrain.superkassa.core.data.adapter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class Base64TokenCodecAdapterTest {
    private val adapter = Base64TokenCodecAdapter()

    @Test
    fun testEncodeDecodeSuccess() {
        val originalToken = 123456789L
        val encoded = adapter.encodeToken(originalToken)
        assertNotNull(encoded)

        val decoded = adapter.decodeToken(encoded)
        assertEquals(originalToken, decoded)
    }

    @Test
    fun testDecodeNullOrBlank() {
        assertNull(adapter.decodeToken(null))
        assertNull(adapter.decodeToken(""))
        assertNull(adapter.decodeToken("   "))
    }

    @Test
    fun testDecodeInvalidBase64() {
        // Invalid Base64 characters
        assertNull(adapter.decodeToken("invalid-base64-!!!"))
    }

    @Test
    fun testDecodeNonNumericContent() {
        // "hello" in Base64 is "aGVsbG8="
        assertNull(adapter.decodeToken("aGVsbG8="))
    }
}
