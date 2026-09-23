package io.github.texport.superkassa.core.data.room

import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.ADMIN_PIN
import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.CASHIER_PIN
import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.KKM
import io.github.texport.superkassa.core.domain.api.exception.SuperkassaException
import io.github.texport.superkassa.core.presentation.api.model.user.UserUpdateRequest
import io.github.texport.superkassa.coredatabase.api.getDatabaseBuilder
import io.github.texport.superkassa.coredatabase.api.openRoomStorage
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Перебор пина упирается во временную блокировку кассы: пять неверных
 * пинов подряд запирают кассу на полминуты, каждая следующая неудача —
 * вдвое дольше, но не дольше четверти часа. Блокировка лежит в базе
 * и переживает перезапуск, а проходит сама — навсегда владелец не заперт.
 */
class PinLockoutRoomTest {
    private val clock = MovableClock()
    private val dir: File = createTempDirectory("pin-lockout-").toFile()
    private val kassa = RoomKassa(clock = clock)

    @AfterTest
    fun cleanUp() {
        kassa.close()
        dir.deleteRecursively()
    }

    @Test
    fun `пятый неверный пин запирает кассу, и верный пин тоже не пускает`() {
        repeat(4) { assertEquals("USER_NOT_FOUND", refusal { kassa.api.currentUser(KKM, WRONG_PIN) }.code) }

        val locked = refusal { kassa.api.currentUser(KKM, WRONG_PIN) }

        assertEquals("PIN_LOCKED", locked.code)
        assertEquals("PIN_LOCKED", refusal { kassa.api.currentUser(KKM, ADMIN_PIN) }.code)
        assertEquals("PIN_LOCKED", refusal { kassa.api.listUsers(KKM, ADMIN_PIN) }.code)
        assertEquals("PIN_LOCKED", refusal { kassa.api.updateUser(KKM, "admin-1", ADMIN_PIN, rename()) }.code)
    }

    @Test
    fun `отказ называет оставшееся время на трёх языках`() {
        lockOut()

        val message = refusal { kassa.api.currentUser(KKM, ADMIN_PIN) }.trilingualMessage

        assertTrue("30 с" in message.ru, message.ru)
        assertTrue("30 секунд" in message.kk, message.kk)
        assertTrue("30 seconds" in message.en, message.en)
    }

    @Test
    fun `блокировка проходит сама, верный пин обнуляет счёт`() {
        lockOut()
        clock.move(FIRST_LOCK_MS)

        assertEquals("Айгерим", kassa.api.currentUser(KKM, ADMIN_PIN).name)
        repeat(4) { assertEquals("USER_NOT_FOUND", refusal { kassa.api.currentUser(KKM, WRONG_PIN) }.code) }
        assertEquals("Нурлан", kassa.api.currentUser(KKM, CASHIER_PIN).name)
    }

    @Test
    fun `каждая следующая блокировка вдвое дольше, но не дольше четверти часа`() {
        lockOut()
        val expected = listOf(60_000L, 120_000L, 240_000L, 480_000L, 900_000L, 900_000L)
        clock.move(FIRST_LOCK_MS)

        for (lock in expected) {
            assertEquals("PIN_LOCKED", refusal { kassa.api.currentUser(KKM, WRONG_PIN) }.code)
            clock.move(lock - 1)
            assertEquals("PIN_LOCKED", refusal { kassa.api.currentUser(KKM, ADMIN_PIN) }.code)
            clock.move(1)
        }
        assertEquals("Айгерим", kassa.api.currentUser(KKM, ADMIN_PIN).name)
    }

    @Test
    fun `часы, переведённые назад, не продлевают блокировку дольше четверти часа`() {
        lockOut()
        clock.move(-DAY_MS)

        assertEquals("PIN_LOCKED", refusal { kassa.api.currentUser(KKM, ADMIN_PIN) }.code)
        clock.move(LONGEST_LOCK_MS)

        assertEquals("Айгерим", kassa.api.currentUser(KKM, ADMIN_PIN).name)
    }

    @Test
    fun `блокировка переживает перезапуск кассы на файловой базе`() {
        val path = File(dir, "kassa.db").path
        val first = RoomKassa(room = openRoomStorage(getDatabaseBuilder(path)), clock = clock)
        repeat(5) { refusal { first.api.currentUser(KKM, WRONG_PIN) } }
        first.close()

        val restarted = RoomKassa(room = openRoomStorage(getDatabaseBuilder(path)), clock = clock)
        try {
            assertEquals("PIN_LOCKED", refusal { restarted.api.currentUser(KKM, ADMIN_PIN) }.code)
        } finally {
            restarted.close()
        }
    }

    private fun lockOut() = repeat(5) { refusal { kassa.api.currentUser(KKM, WRONG_PIN) } }

    private fun refusal(call: () -> Any): SuperkassaException = assertFailsWith<SuperkassaException> { call() }

    private fun rename() = UserUpdateRequest(name = "Айгерим Б.")

    private companion object {
        const val WRONG_PIN = "5555"
        const val FIRST_LOCK_MS = 30_000L
        const val LONGEST_LOCK_MS = 900_000L
        const val DAY_MS = 86_400_000L
    }
}
