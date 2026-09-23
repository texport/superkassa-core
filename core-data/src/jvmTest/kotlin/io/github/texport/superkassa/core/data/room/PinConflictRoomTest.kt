package io.github.texport.superkassa.core.data.room

import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.ADMIN_PIN
import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.CASHIER_PIN
import io.github.texport.superkassa.core.data.room.RoomKassa.Companion.KKM
import io.github.texport.superkassa.core.domain.api.exception.ConflictException
import io.github.texport.superkassa.core.presentation.api.model.user.UserCreateRequest
import io.github.texport.superkassa.core.presentation.api.model.user.UserRole
import io.github.texport.superkassa.core.presentation.api.model.user.UserUpdateRequest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Пин на кассе принадлежит одному пользователю, как у узла: второй
 * кассир с тем же пином не заводится, и чужой пин себе не ставится.
 * Иначе вход по пину пускал бы под тем, кто первым попался в списке.
 */
class PinConflictRoomTest {
    private val kassa = RoomKassa()

    @AfterTest
    fun cleanUp() = kassa.close()

    @Test
    fun `второй кассир с занятым пином не заводится`() {
        val refusal = assertFailsWith<ConflictException> {
            kassa.api.createUser(KKM, ADMIN_PIN, UserCreateRequest("Асель", UserRole.CASHIER, CASHIER_PIN))
        }

        assertEquals("USER_PIN_CONFLICT", refusal.code)
        assertEquals("Такой пин на этой кассе уже занят. Задайте другой.", refusal.trilingualMessage.ru)
        assertEquals(2, kassa.api.listUsers(KKM, ADMIN_PIN).size)
        assertEquals("Нурлан", kassa.api.currentUser(KKM, CASHIER_PIN).name)
    }

    @Test
    fun `чужой пин себе не ставится, а свой прежний — можно`() {
        val refusal = assertFailsWith<ConflictException> {
            kassa.api.updateUser(KKM, "cashier-1", CASHIER_PIN, UserUpdateRequest(userPin = ADMIN_PIN))
        }

        assertEquals("USER_PIN_CONFLICT", refusal.code)
        assertEquals("Айгерим", kassa.api.currentUser(KKM, ADMIN_PIN).name)
        kassa.api.updateUser(KKM, "cashier-1", CASHIER_PIN, UserUpdateRequest(userPin = CASHIER_PIN))
        assertEquals("Нурлан", kassa.api.currentUser(KKM, CASHIER_PIN).name)
    }
}
