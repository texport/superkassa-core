package io.github.texport.superkassa.core.domain.model.common

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.domain.api.model.common.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

/** Точное десятичное число: разбор, запись, округление и обмен по JSON. */
class DecimalTest {

    @Test
    fun parsesDecimalNotation() {
        assertEquals(Decimal(15055L, 2), Decimal.parse("150.55"))
        assertEquals(Decimal(-125L, 3), Decimal.parse("-0.125"))
        assertEquals(Decimal(12L, 0), Decimal.parse("12"))
        assertEquals(Decimal(12L, 0), Decimal.parse(" +12 "))
    }

    @Test
    fun refusesWhatIsNotDecimalNotation() {
        assertFailsWith<IllegalArgumentException> { Decimal.parse("") }
        assertFailsWith<IllegalArgumentException> { Decimal.parse("1e2") }
        assertFailsWith<IllegalArgumentException> { Decimal.parse("сто") }
        assertFailsWith<IllegalArgumentException> { Decimal.parse("0.1234567890") }
        assertFailsWith<IllegalArgumentException> { Decimal(1L, -1) }
    }

    @Test
    fun keepsNotationWhenPrinted() {
        assertEquals("150.55", Decimal.parse("150.55").toString())
        assertEquals("-0.125", Decimal.parse("-0.125").toString())
        assertEquals("0.05", Decimal.parse("0.05").toString())
        assertEquals("12", Decimal.parse("12").toString())
    }

    @Test
    fun comparesByValueNotByNotation() {
        assertEquals(Decimal.parse("150.0"), Decimal.parse("150.00"))
        assertEquals(Decimal.parse("150.0").hashCode(), Decimal.parse("150.00").hashCode())
        assertNotEquals(Decimal.parse("150.01"), Decimal.parse("150.10"))
        assertTrue(!Decimal.parse("1.0").equals("1.0"))
    }

    @Test
    fun roundsHalfAwayFromZero() {
        assertEquals(15055L, Decimal.parse("150.55").scaled(2))
        assertEquals(1506L, Decimal.parse("150.55").scaled(1))
        assertEquals(-1506L, Decimal.parse("-150.55").scaled(1))
        assertEquals(150550L, Decimal.parse("150.55").scaled(3))
        assertEquals(1, Decimal.parse("0.01").signum)
        assertEquals(0, Decimal.ZERO.signum)
        assertEquals(-1, Decimal.parse("-0.01").signum)
    }

    @Test
    fun moneyKeepsTheTiynThatDoubleUsedToLose() {
        // 150.55 в Double хранится как 150.54999999999998: округление
        // к ближайшему тиыну обязано дать 15055, а не 15054.
        assertEquals(15055L, Money.fromTenge(Decimal.parse("150.55")).tiyn())
        assertEquals(Money(1234L, 56), Money.fromTenge(Decimal.parse("1234.56")))
        assertEquals(Money(0L, 1), Money.fromTenge(Decimal.parse("0.005")))
    }

    @Test
    fun travelsThroughJsonAsANumber() {
        val json = Json.encodeToString(Decimal.parse("150.55"))

        assertEquals("150.55", json)
        assertEquals(Decimal.parse("150.55"), Json.decodeFromString<Decimal>(json))
        assertEquals(Decimal.parse("0.125"), Json.decodeFromString<Decimal>("0.125"))
    }
}
