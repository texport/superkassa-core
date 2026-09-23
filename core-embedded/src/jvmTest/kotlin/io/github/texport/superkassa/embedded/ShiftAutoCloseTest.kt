package io.github.texport.superkassa.embedded

import io.github.texport.superkassa.core.data.api.systemClock
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.embedded.TestCashRegister.ADMIN_PIN
import io.github.texport.superkassa.embedded.TestCashRegister.CASHIER_PIN
import io.github.texport.superkassa.embedded.TestCashRegister.KKM_ID
import io.github.texport.superkassa.embedded.impl.EmbeddedSuperkassa
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Встраиваемая касса сама закрывает смену до предела в сутки, если
 * у кассы включено автозакрытие: по часам, пока касса открыта.
 */
class ShiftAutoCloseTest {
    private val dir: File = createTempDirectory("kassa-").toFile()
    private val clock = SteppedClock()

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    @Test
    fun `проверка по часам закрывает смену за полчаса до предела, без связи — автономно`() {
        TestCashRegister.open(dir, clock = clock).use { kassa ->
            openShiftWithSale(kassa)

            clock.advance(23.hours)
            assertEquals(0, kassa.closeDueShiftsNow())
            assertNotNull(kassa.storage.findOpenShift(KKM_ID))

            clock.advance(31.minutes)
            assertEquals(1, kassa.closeDueShiftsNow())
            assertNull(kassa.storage.findOpenShift(KKM_ID))
        }
    }

    @Test
    fun `открытая касса проверяет смены сама, не дожидаясь вызова`() {
        TestCashRegister.open(dir, clock = clock, shiftCheckInterval = 50.milliseconds).use { kassa ->
            openShiftWithSale(kassa)

            clock.advance(24.hours)

            val deadline = System.nanoTime() + 10.seconds.inWholeNanoseconds
            while (kassa.storage.findOpenShift(KKM_ID) != null && System.nanoTime() < deadline) Thread.sleep(POLL_MILLIS)
            assertNull(kassa.storage.findOpenShift(KKM_ID))
        }
    }

    private fun openShiftWithSale(kassa: EmbeddedSuperkassa) {
        TestCashRegister.registerKkm(kassa)
        kassa.storage.updateKkm(checkNotNull(kassa.storage.findKkm(KKM_ID)).copy(autoCloseShift = true))
        kassa.api.openShift(KKM_ID, ADMIN_PIN)
        kassa.api.createSellReceipt(KKM_ID, CASHIER_PIN, TestCashRegister.sale("sale-1"))
    }

    /** Часы кассы, которые проверка переводит вперёд; ходят между потоками. */
    class SteppedClock : ClockPort by systemClock() {
        @Volatile
        private var shift = Duration.ZERO

        override fun now(): Long = System.currentTimeMillis() + shift.inWholeMilliseconds

        fun advance(by: Duration) {
            shift += by
        }
    }

    private companion object {
        const val POLL_MILLIS = 20L
    }
}
