package kz.mybrain.superkassa.core.domain.model.common

import kz.mybrain.superkassa.core.domain.exception.TrilingualMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommonModelTest {

    @Test
    fun testMoney() {
        val m1 = Money(100L, 50)
        val m1Copy = m1.copy()
        val mSame = Money(100L, 50)
        val mDiffBills = m1.copy(bills = 200L)
        val mDiffCoins = m1.copy(coins = 60)

        // equals & hashCode
        assertEquals(m1, m1)
        assertEquals(m1, m1Copy)
        assertEquals(m1, mSame)
        assertEquals(m1.hashCode(), mSame.hashCode())

        assertNotEquals(m1, Any())
        assertFalse(m1.equals(null))
        assertNotEquals(m1, mDiffBills)
        assertNotEquals(m1, mDiffCoins)

        assertTrue(m1.toString().contains("bills=100"))

        // fromTenge helper
        val mFromTenge1 = Money.fromTenge(1234.56)
        assertEquals(1234L, mFromTenge1.bills)
        assertEquals(56, mFromTenge1.coins)

        val mFromTenge2 = Money.fromTenge(1234.00)
        assertEquals(1234L, mFromTenge2.bills)
        assertEquals(0, mFromTenge2.coins)
    }

    @Test
    fun testUnitOfMeasurement() {
        // test fromCode
        assertEquals(UnitOfMeasurement.PIECE, UnitOfMeasurement.fromCode("796"))
        assertEquals(UnitOfMeasurement.KILOGRAM, UnitOfMeasurement.fromCode(" 116 "))
        assertEquals(UnitOfMeasurement.DEFAULT, UnitOfMeasurement.fromCode(""))

        assertFailsWith<IllegalArgumentException> {
            UnitOfMeasurement.fromCode("unknown")
        }

        // enum methods
        for (item in UnitOfMeasurement.entries) {
            val value = UnitOfMeasurement.valueOf(item.name)
            assertEquals(item, value)
            assertNotNullOrEmpty(item.code)
            assertNotNullOrEmpty(item.nameRus)
            assertNotNullOrEmpty(item.nameKaz)
            assertNotNullOrEmpty(item.shortRus)
            assertNotNullOrEmpty(item.shortKaz)
        }
    }

    private fun assertNotNullOrEmpty(str: String) {
        assertTrue(str.isNotEmpty())
    }

    @Test
    fun testVatGroup() {
        for (item in VatGroup.entries) {
            val value = VatGroup.valueOf(item.name)
            assertEquals(item, value)
            assertTrue(item.percent >= 0)
            assertTrue(item.percentThousandths >= 0)
            assertNotNullOrEmpty(item.description)
            assertNotNullOrEmpty(item.taxTypeCode)
        }
    }

    @Test
    fun testCounterKeyFormatsAndScopes() {
        assertEquals("GLOBAL", CounterScopes.GLOBAL)
        assertEquals("SHIFT", CounterScopes.SHIFT)
        assertTrue(CounterKeyFormats.CASH_SUM.isNotEmpty())
    }

    @Test
    fun testCounterSnapshot() {
        val s1 = CounterSnapshot("GLOBAL", null, "key-1", 100L, 123456789L)
        val s1Copy = s1.copy()
        val sSame = CounterSnapshot("GLOBAL", null, "key-1", 100L, 123456789L)
        val sDiffScope = s1.copy(scope = "SHIFT")
        val sDiffShiftId = s1.copy(shiftId = "s-1")
        val sDiffKey = s1.copy(key = "key-2")
        val sDiffVal = s1.copy(value = 200L)
        val sDiffUpdated = s1.copy(updatedAt = 987654321L)

        assertEquals(s1, s1)
        assertEquals(s1, s1Copy)
        assertEquals(s1, sSame)
        assertEquals(s1.hashCode(), sSame.hashCode())

        assertNotEquals(s1, Any())
        assertFalse(s1.equals(null))
        assertNotEquals(s1, sDiffScope)
        assertNotEquals(s1, sDiffShiftId)
        assertNotEquals(s1, sDiffKey)
        assertNotEquals(s1, sDiffVal)
        assertNotEquals(s1, sDiffUpdated)

        assertTrue(s1.toString().contains("key=key-1"))
    }

    @Test
    fun testTaxRegime() {
        for (item in TaxRegime.entries) {
            val value = TaxRegime.valueOf(item.name)
            assertEquals(item, value)
        }
    }

    @Test
    fun testTimeValidationResult() {
        val triMsg = TrilingualMessage("ru", "kk", "en")
        val res1 = TimeValidationResult(true, null, null)
        val res2 = TimeValidationResult(false, "error", triMsg)

        // properties
        assertTrue(res1.ok)
        assertNull(res1.reason)
        assertNull(res1.trilingualMessage)
        assertNull(res1.messageRu)
        assertNull(res1.messageKk)
        assertNull(res1.messageEn)
        assertNull(res1.trilingualMessage())

        assertFalse(res2.ok)
        assertEquals("error", res2.reason)
        assertEquals(triMsg, res2.trilingualMessage)
        assertEquals("ru", res2.messageRu)
        assertEquals("kk", res2.messageKk)
        assertEquals("en", res2.messageEn)
        assertEquals("RU: ru | KK: kk | EN: en", res2.trilingualMessage())

        // equals & hashCode
        val res2Copy = res2.copy()
        assertEquals(res2, res2)
        assertEquals(res2, res2Copy)
        assertEquals(res2.hashCode(), res2Copy.hashCode())
        
        val resDiffOk = res2.copy(ok = true)
        val resDiffReason = res2.copy(reason = "diff")
        val resDiffMsg = res2.copy(trilingualMessage = null)
        assertNotEquals(res2, resDiffOk)
        assertNotEquals(res2, resDiffReason)
        assertNotEquals(res2, resDiffMsg)
        assertNotEquals(res2, Any())
        assertFalse(res2.equals(null))

        assertTrue(res2.toString().contains("ok=false"))
    }
}
