package io.github.texport.superkassa.core.data.room

import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.ADMIN_PIN
import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.CASHIER_PIN
import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.KKM
import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.cash
import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.item
import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptSellRequest
import kz.kazakhtelecom.proto.v203.CommandTypeEnum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * Предел смены в сутки: одно правило и одни часы для запрета продажи,
 * для экрана и для автозакрытия.
 */
class RoomShiftDayLimitTest {
    private val clock = ManualClock()
    private val kassa = RoomKassa(clock = clock)

    @Test
    fun `экран знает предел смены тем же правилом, каким продажа получает отказ`() {
        kassa.api.openShift(KKM, ADMIN_PIN)
        clock.advance(3.hours)
        val firstSaleAt = clock.now()
        sell("100", "sale-1")
        val deadline = firstSaleAt + 24.hours.inWholeMilliseconds

        clock.advance(24.hours)
        assertEquals(deadline to false, shiftState())
        sell("100", "sale-2")

        clock.advance(1.milliseconds)
        assertEquals(deadline to true, shiftState())
        val refused = assertFailsWith<ValidationException> { sell("100", "sale-3") }
        assertEquals("SHIFT_LONGER_THAN_DAY", refused.code)
    }

    @Test
    fun `смена без продаж предела не имеет`() {
        kassa.api.openShift(KKM, ADMIN_PIN)
        clock.advance(30.hours)

        assertEquals(null to false, shiftState())
    }

    @Test
    fun `автозакрытие закрывает смену за полчаса до предела и не раньше`() {
        kassa.settings(autoCloseShift = true)
        kassa.api.openShift(KKM, ADMIN_PIN)
        sell("100", "sale-1")

        clock.advance(23.hours + 29.minutes)
        assertNull(kassa.api.autoCloseShift(KKM))
        assertNotNull(kassa.storage.findOpenShift(KKM))

        clock.advance(1.minutes)
        assertEquals(DeliveryStatus.ONLINE_OK, kassa.api.autoCloseShift(KKM)?.deliveryStatus)
        assertNull(kassa.storage.findOpenShift(KKM))
        assertEquals(1, kassa.bfd.closeShifts().size)
    }

    @Test
    fun `без настройки автозакрытие смену не трогает`() {
        kassa.api.openShift(KKM, ADMIN_PIN)
        sell("100", "sale-1")
        clock.advance(25.hours)

        assertNull(kassa.api.autoCloseShift(KKM))
        assertNotNull(kassa.storage.findOpenShift(KKM))
    }

    @Test
    fun `отклонённое автозакрытие оставляет смену открытой и не повторяется само`() {
        kassa.settings(autoCloseShift = true)
        kassa.api.openShift(KKM, ADMIN_PIN)
        sell("100", "sale-1")
        clock.advance(24.hours)
        kassa.bfd.reject(CommandTypeEnum.COMMAND_CLOSE_SHIFT, INCORRECT_REQUEST_DATA)

        assertEquals(DeliveryStatus.ONLINE_ERROR, kassa.api.autoCloseShift(KKM)?.deliveryStatus)
        clock.advance(5.minutes)
        assertNull(kassa.api.autoCloseShift(KKM))

        assertNotNull(kassa.storage.findOpenShift(KKM))
        assertEquals(1, kassa.bfd.closeShifts().size)
        assertTrue(kassa.api.getOpenShift(KKM, CASHIER_PIN).dayLimitExceeded, "the screen sees why sales are refused")
    }

    private fun shiftState(): Pair<Long?, Boolean> =
        kassa.api.getOpenShift(KKM, CASHIER_PIN).let { it.dayLimitAt to it.dayLimitExceeded }

    private fun sell(total: String, key: String) {
        kassa.api.createSellReceipt(
            KKM, CASHIER_PIN,
            ReceiptSellRequest(idempotencyKey = key, items = listOf(item(total)), payments = listOf(cash(total)))
        )
    }

    private companion object {
        const val INCORRECT_REQUEST_DATA = 13
    }
}
