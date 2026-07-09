package io.github.texport.superkassa.core.data.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UtilTest {

    @Test
    fun testBase64Coder() {
        val original = "Hello World! @2026"
        val bytes = original.encodeToByteArray()
        val encoded = Base64Coder.encode(bytes)
        val decoded = Base64Coder.decode(encoded)
        assertEquals(original, decoded.decodeToString())

        // Test with different padding lengths
        for (i in 0..10) {
            val src = ByteArray(i) { it.toByte() }
            val enc = Base64Coder.encode(src)
            val dec = Base64Coder.decode(enc)
            assertTrue(src.contentEquals(dec))
        }
    }

    @Test
    fun testSha256() {
        val input = "superkassa_pin_1234"
        val hash = Sha256.hash(input)
        assertEquals("d970eff0971b2dc37f9f8c88ccb5f7aec301646309a4c5c2186f4afd86b75867", hash)
    }

    @Test
    fun testCrc32() {
        val input = "Hello CRC32".encodeToByteArray()
        val crc = Crc32.calculate(input)
        assertEquals(1203018389L, crc)
    }

    @Test
    fun testPlatformUtils() {
        val map = createConcurrentMap<String, Int>()
        map["test"] = 123
        assertEquals(123, map["test"])
    }
}
