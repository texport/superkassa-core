package kz.mybrain.superkassa.core.data.adapter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UuidGeneratorAdapterTest {

    @Test
    fun testNextId() {
        val id1 = UuidGeneratorAdapter.nextId()
        val id2 = UuidGeneratorAdapter.nextId()
        assertNotNull(id1)
        assertNotNull(id2)
        assertTrue(id1.isNotEmpty())
        assertTrue(id2.isNotEmpty())
        assertTrue(id1 != id2)
        // Verify UUID format (36 chars)
        assertEquals(36, id1.length)
    }

    @Test
    fun testGenerateFactoryNumber() {
        val fn = UuidGeneratorAdapter.generateFactoryNumber()
        assertNotNull(fn)
        // Format: KZT + YY + 10 Hex characters = 15 characters
        assertEquals(15, fn.length)
        assertTrue(fn.startsWith("KZT"))
        
        val currentYearShort = (java.time.Year.now().value % 100).toString().padStart(2, '0')
        assertTrue(fn.substring(3, 5) == currentYearShort)
    }
}
