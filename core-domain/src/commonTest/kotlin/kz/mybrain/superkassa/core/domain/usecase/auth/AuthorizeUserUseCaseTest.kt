package kz.mybrain.superkassa.core.domain.usecase.auth

import io.mockk.every
import io.mockk.mockk
import kz.mybrain.superkassa.core.domain.exception.ForbiddenException
import kz.mybrain.superkassa.core.domain.exception.NotFoundException
import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.model.auth.KkmUser
import kz.mybrain.superkassa.core.domain.model.auth.UserRole
import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kz.mybrain.superkassa.core.domain.port.PinHasherPort
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AuthorizeUserUseCaseTest {

    private val storage = mockk<StoragePort>()
    private val pinHasher = mockk<PinHasherPort>()
    private val authorizeUser = AuthorizeUserUseCase(storage, pinHasher)

    @Test
    fun testExecuteSuccess() {
        val user = KkmUser("user-1", "Admin", UserRole.ADMIN, "1234", 100L)
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
    fun testExecuteDefaultPinBlocked() {
        assertFailsWith<ValidationException> {
            authorizeUser.execute("kkm-1", "0000", setOf(UserRole.ADMIN))
        }
        assertFailsWith<ValidationException> {
            authorizeUser.execute("kkm-1", "1111", setOf(UserRole.ADMIN))
        }
    }

    @Test
    fun testExecuteDefaultPinAllowed() {
        val user = KkmUser("user-1", "Admin", UserRole.ADMIN, "0000", 100L)
        every { pinHasher.hash("0000") } returns "hash-1"
        every { storage.findUserByPin("kkm-1", "hash-1") } returns user

        authorizeUser.execute("kkm-1", "0000", setOf(UserRole.ADMIN), allowDefaultPin = true)
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
        val user = KkmUser("user-1", "Cashier", UserRole.CASHIER, "1234", 100L)
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
        val user = KkmUser("user-1", "Admin", UserRole.ADMIN, "1234", 100L)
        every { pinHasher.hash("1234") } returns "hash-1"
        every { storage.findUserByPin("kkm-1", "hash-1") } returns user

        authorizeUser.requireRole("kkm-1", "1234", setOf(UserRole.ADMIN))
    }

}
