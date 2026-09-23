package io.github.texport.superkassa.core.domain.impl.usecase.auth

import io.github.texport.superkassa.core.domain.impl.usecase.auth.MemoryPinAttempts
import io.github.texport.superkassa.core.domain.impl.usecase.auth.PinGuard
import io.mockk.every
import io.mockk.mockk
import io.github.texport.superkassa.core.domain.api.exception.ForbiddenException
import io.github.texport.superkassa.core.domain.api.exception.NotFoundException
import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.auth.KkmUser
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.port.internal.PinHasherPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AuthorizeUserUseCaseTest {

    private val storage = mockk<StoragePort>()
    private val pinHasher = mockk<PinHasherPort>()
    private val authorizeUser = AuthorizeUserUseCase(storage, pinHasher, PinGuard(MemoryPinAttempts()))

    @Test
    fun testExecuteSuccess() {
        val user = KkmUser("user-1", "Admin", UserRole.ADMIN, 100L)
        every { pinHasher.hash("1234") } returns "hash-1"
        every { storage.findUserByPin("kkm-1", "hash-1") } returns user

        authorizeUser.execute("kkm-1", "1234", setOf(UserRole.ADMIN))
    }

    @Test
    fun testExecuteBlankPin() {
        assertFailsWith<ValidationException> {
            authorizeUser.execute("kkm-1", "", setOf(UserRole.ADMIN))
        }
    }

    @Test
    fun testFormerStandardPinSignsInLikeAnyOther() {
        // Кассы, заведённые прежде со стандартным пином, не теряют вход:
        // такой пин теперь обычный пин пользователя, без особого пути.
        val user = KkmUser("user-1", "Admin", UserRole.ADMIN, 100L)
        every { pinHasher.hash("0000") } returns "hash-0000"
        every { storage.findUserByPin("kkm-1", "hash-0000") } returns user

        authorizeUser.execute("kkm-1", "0000", setOf(UserRole.ADMIN, UserRole.CASHIER))
        assertEquals(user, authorizeUser.identify("kkm-1", "0000"))
    }

    @Test
    fun testExecuteUserNotFound() {
        every { pinHasher.hash("1234") } returns "hash-1"
        every { storage.findUserByPin("kkm-1", "hash-1") } returns null

        assertFailsWith<ForbiddenException> {
            authorizeUser.execute("kkm-1", "1234", setOf(UserRole.ADMIN))
        }
    }

    @Test
    fun testExecuteUserForbidden() {
        val user = KkmUser("user-1", "Cashier", UserRole.CASHIER, 100L)
        every { pinHasher.hash("1234") } returns "hash-2"
        every { storage.findUserByPin("kkm-1", "hash-2") } returns user

        assertFailsWith<ForbiddenException> {
            authorizeUser.execute("kkm-1", "1234", setOf(UserRole.ADMIN))
        }
    }

    @Test
    fun testRequireKkmSuccess() {
        val kkm = KkmInfo(id = "kkm-1", createdAt = 0, updatedAt = 0, mode = "ACTIVE", state = "ACTIVE")
        every { storage.findKkm("kkm-1") } returns kkm

        val result = authorizeUser.requireKkm("kkm-1")
        assertEquals(kkm, result)
    }

    @Test
    fun testRequireKkmForUpdateSuccess() {
        val kkm = KkmInfo(id = "kkm-1", createdAt = 0, updatedAt = 0, mode = "ACTIVE", state = "ACTIVE")
        every { storage.findKkmForUpdate("kkm-1") } returns kkm

        val result = authorizeUser.requireKkm("kkm-1", forUpdate = true)
        assertEquals(kkm, result)
    }

    @Test
    fun testRequireKkmNotFound() {
        every { storage.findKkm("kkm-1") } returns null

        assertFailsWith<NotFoundException> {
            authorizeUser.requireKkm("kkm-1")
        }
    }

    @Test
    fun testRequireRoleSuccess() {
        val user = KkmUser("user-1", "Admin", UserRole.ADMIN, 100L)
        every { pinHasher.hash("1234") } returns "hash-1"
        every { storage.findUserByPin("kkm-1", "hash-1") } returns user

        authorizeUser.requireRole("kkm-1", "1234", setOf(UserRole.ADMIN))
    }

}
