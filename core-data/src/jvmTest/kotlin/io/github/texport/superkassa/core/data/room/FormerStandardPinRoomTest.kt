package io.github.texport.superkassa.core.data.room

import io.github.texport.superkassa.core.data.impl.adapter.security.Sha256PinHasherAdapter
import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.KKM
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.presentation.api.model.user.UserUpdateRequest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Кассы, заведённые прежде без пина в запросе, получили администратора
 * с пином «0000». Пинов по умолчанию больше нет, и особого пути для
 * такого пина тоже: это обычный пин пользователя. Вход по нему не ломается
 * и не ограничивается — администратор работает и сам меняет пин, когда решит.
 */
class FormerStandardPinRoomTest {
    private val kassa = RoomKassa().also {
        it.storage.createUser(KKM, "admin-0", "Ерлан", UserRole.ADMIN, Sha256PinHasherAdapter().hash(FORMER_PIN), 0L)
    }

    @AfterTest
    fun cleanUp() = kassa.close()

    @Test
    fun `администратор с прежним стандартным пином открывает смену`() {
        kassa.api.openShift(KKM, FORMER_PIN)

        assertEquals(true, kassa.storage.findOpenShift(KKM) != null)
    }

    @Test
    fun `администратор с прежним стандартным пином меняет его на свой`() {
        kassa.api.updateUser(KKM, "admin-0", FORMER_PIN, UserUpdateRequest(userPin = NEW_PIN))

        assertEquals("Ерлан", kassa.api.currentUser(KKM, NEW_PIN).name)
    }

    private companion object {
        const val FORMER_PIN = "0000"
        const val NEW_PIN = "5096"
    }
}
