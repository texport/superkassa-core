package io.github.texport.superkassa.core.domain.impl.usecase.user

import io.mockk.every
import io.mockk.mockk
import io.github.texport.superkassa.core.domain.api.exception.ConflictException
import io.github.texport.superkassa.core.domain.api.exception.ForbiddenException
import io.github.texport.superkassa.core.domain.api.exception.NotFoundException
import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.auth.KkmUser
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.internal.IdGeneratorPort
import io.github.texport.superkassa.core.domain.api.port.internal.PinHasherPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserUseCasesTest {

    private val storage = mockk<StoragePort>(relaxed = true)
    private val idGenerator = mockk<IdGeneratorPort>()
    private val clock = mockk<ClockPort>()
    private val pinHasher = mockk<PinHasherPort>()
    private val authorizeUserUseCase = mockk<AuthorizeUserUseCase>()

    private val createUser = CreateUserUseCase(storage, idGenerator, clock, pinHasher, authorizeUserUseCase)
    private val updateUser = UpdateUserUseCase(storage, pinHasher, authorizeUserUseCase)
    private val deleteUser = DeleteUserUseCase(storage, authorizeUserUseCase)

    init {
        every { storage.findKkmForUpdate(any()) } answers { storage.findKkm(firstArg()) }
        every { authorizeUserUseCase.requireKkm(any(), any()) } answers { authorizeUserUseCase.requireKkm(firstArg()) }
        every { authorizeUserUseCase.requireRole(any(), any(), any(), any()) } answers { authorizeUserUseCase.requireRole(firstArg(), secondArg(), thirdArg()) }
        every { pinHasher.hash(any()) } answers { "hash-" + firstArg<String>() }
        every { storage.findUserByPin(any(), any()) } answers { KkmUser("admin", "Admin", UserRole.ADMIN, 0L) }
    }

    @Test
    fun testCreateUserSuccess() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns KkmInfo(id = "kkm-1", createdAt = 0, updatedAt = 0, mode = "ACTIVE", state = "ACTIVE")
        every { authorizeUserUseCase.requireRole("kkm-1", "admin-pin", any()) } returns mockk()
        every { idGenerator.nextId() } returns "user-1"
        every { clock.now() } returns 1000L
        every { pinHasher.hash("user-pin") } returns "hash-1"
        every { storage.createUser("kkm-1", "user-1", "John", UserRole.ADMIN, "hash-1", 1000L) } returns true

        val user = createUser.execute("kkm-1", "admin-pin", "John", UserRole.ADMIN, "user-pin")
        assertEquals("user-1", user.id)
        assertEquals("John", user.name)
        assertEquals(UserRole.ADMIN, user.role)
    }

    @Test
    fun testCreateUserBlankName() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns mockk()
        every { authorizeUserUseCase.requireRole("kkm-1", "admin-pin", any()) } returns mockk()

        assertFailsWith<ValidationException> {
            createUser.execute("kkm-1", "admin-pin", "", UserRole.ADMIN, "user-pin")
        }
    }

    @Test
    fun testCreateUserConflict() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns mockk()
        every { authorizeUserUseCase.requireRole("kkm-1", "admin-pin", any()) } returns mockk()
        every { idGenerator.nextId() } returns "user-1"
        every { clock.now() } returns 1000L
        every { pinHasher.hash("user-pin") } returns "hash-1"
        every { storage.createUser(any(), any(), any(), any(), any(), any()) } returns false

        assertFailsWith<ConflictException> {
            createUser.execute("kkm-1", "admin-pin", "John", UserRole.ADMIN, "user-pin")
        }
    }

    @Test
    fun testUpdateUserSuccess() {
        val existing = KkmUser("user-1", "John", UserRole.CASHIER, 500L)
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns mockk()
        every { authorizeUserUseCase.requireRole("kkm-1", "admin-pin", any()) } returns mockk()
        every { storage.listUsers("kkm-1") } returns listOf(existing)
        every { storage.updateUser("kkm-1", "user-1", "John New", UserRole.ADMIN, "hash-2222") } returns true

        val user = updateUser.execute("kkm-1", "user-1", "admin-pin", "John New", UserRole.ADMIN, "2222")
        assertEquals("John New", user.name)
        assertEquals(UserRole.ADMIN, user.role)
    }

    @Test
    fun testUpdateUserEmptyParameters() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns mockk()
        every { authorizeUserUseCase.requireRole("kkm-1", "admin-pin", any()) } returns mockk()

        assertFailsWith<ValidationException> {
            updateUser.execute("kkm-1", "user-1", "admin-pin", null, null, null)
        }
    }

    @Test
    fun testDeleteUserSuccess() {
        val target = KkmUser("user-2", "Cashier", UserRole.CASHIER, 500L)
        val other = KkmUser("user-1", "Cashier 2", UserRole.CASHIER, 600L)
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns mockk()
        every { authorizeUserUseCase.requireRole("kkm-1", "admin-pin", any()) } returns mockk()
        every { storage.listUsers("kkm-1") } returns listOf(target, other)
        every { storage.deleteUser("kkm-1", "user-2") } returns true

        deleteUser.execute("kkm-1", "user-2", "admin-pin")
    }

    @Test
    fun testDeleteUserRoleRequired() {
        val target = KkmUser("user-1", "Admin", UserRole.ADMIN, 500L)
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns mockk()
        every { authorizeUserUseCase.requireRole("kkm-1", "admin-pin", any()) } returns mockk()
        every { storage.listUsers("kkm-1") } returns listOf(target)

        assertFailsWith<ValidationException> {
            deleteUser.execute("kkm-1", "user-1", "admin-pin")
        }
    }

    @Test
    fun testCreateUserBlankPin() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns mockk()
        every { authorizeUserUseCase.requireRole("kkm-1", "admin-pin", any()) } returns mockk()

        assertFailsWith<ValidationException> {
            createUser.execute("kkm-1", "admin-pin", "John", UserRole.ADMIN, "")
        }
    }

    @Test
    fun testDeleteUserNotFoundInList() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns mockk()
        every { authorizeUserUseCase.requireRole("kkm-1", "admin-pin", any()) } returns mockk()
        every { storage.listUsers("kkm-1") } returns emptyList()

        assertFailsWith<NotFoundException> {
            deleteUser.execute("kkm-1", "user-2", "admin-pin")
        }
    }

    @Test
    fun testDeleteUserDbFailure() {
        val target = KkmUser("user-2", "Cashier", UserRole.CASHIER, 500L)
        val other = KkmUser("user-1", "Cashier 2", UserRole.CASHIER, 600L)
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns mockk()
        every { authorizeUserUseCase.requireRole("kkm-1", "admin-pin", any()) } returns mockk()
        every { storage.listUsers("kkm-1") } returns listOf(target, other)
        every { storage.deleteUser("kkm-1", "user-2") } returns false

        assertFailsWith<NotFoundException> {
            deleteUser.execute("kkm-1", "user-2", "admin-pin")
        }
    }

    @Test
    fun testUpdateUserNotFound() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns mockk()
        every { authorizeUserUseCase.requireRole("kkm-1", "admin-pin", any()) } returns mockk()
        every { storage.listUsers("kkm-1") } returns emptyList()

        assertFailsWith<NotFoundException> {
            updateUser.execute("kkm-1", "user-1", "admin-pin", "John New", UserRole.ADMIN, "2222")
        }
    }

    @Test
    fun testUpdateUserBlankName() {
        val existing = KkmUser("user-1", "John", UserRole.CASHIER, 500L)
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns mockk()
        every { authorizeUserUseCase.requireRole("kkm-1", "admin-pin", any()) } returns mockk()
        every { storage.listUsers("kkm-1") } returns listOf(existing)

        assertFailsWith<ValidationException> {
            updateUser.execute("kkm-1", "user-1", "admin-pin", "   ", UserRole.ADMIN, "2222")
        }
    }

    @Test
    fun testUpdateUserBlankPin() {
        val existing = KkmUser("user-1", "John", UserRole.CASHIER, 500L)
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns mockk()
        every { authorizeUserUseCase.requireRole("kkm-1", "admin-pin", any()) } returns mockk()
        every { storage.listUsers("kkm-1") } returns listOf(existing)

        assertFailsWith<ValidationException> {
            updateUser.execute("kkm-1", "user-1", "admin-pin", "John New", UserRole.ADMIN, "")
        }
    }

    @Test
    fun testUpdateUserConflict() {
        val existing = KkmUser("user-1", "John", UserRole.CASHIER, 500L)
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns mockk()
        every { authorizeUserUseCase.requireRole("kkm-1", "admin-pin", any()) } returns mockk()
        every { storage.listUsers("kkm-1") } returns listOf(existing)
        every { storage.updateUser("kkm-1", "user-1", "John New", UserRole.ADMIN, "hash-2222") } returns false

        assertFailsWith<ConflictException> {
            updateUser.execute("kkm-1", "user-1", "admin-pin", "John New", UserRole.ADMIN, "2222")
        }
    }

    @Test
    fun testUpdateUserPartialUpdatesAndNullFallbacks() {
        val existing = KkmUser("user-1", "John", UserRole.CASHIER, 500L)
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns mockk()
        every { authorizeUserUseCase.requireRole("kkm-1", "admin-pin", any()) } returns mockk()
        every { storage.listUsers("kkm-1") } returns listOf(existing)

        assertFailsWith<ValidationException> {
            updateUser.execute("kkm-1", "user-1", "admin-pin", null, null, null)
        }
    }

    @Test
    fun testUpdateUserOnlyOneFieldChanged() {
        val existing = KkmUser("user-1", "John", UserRole.CASHIER, 500L)
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns mockk()
        every { authorizeUserUseCase.requireRole("kkm-1", "admin-pin", any()) } returns mockk()
        every { storage.listUsers("kkm-1") } returns listOf(existing)
        every { storage.updateUser("kkm-1", "user-1", "John", UserRole.CASHIER, "hash-5555") } returns true

        val user = updateUser.execute("kkm-1", "user-1", "admin-pin", null, null, "5555")
        assertEquals("John", user.name)
        assertEquals(UserRole.CASHIER, user.role)
    }

    @Test
    fun testCreateUserDefaultPin() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns mockk()
        every { authorizeUserUseCase.requireRole("kkm-1", "admin-pin", any()) } returns mockk()

        assertFailsWith<ValidationException> {
            createUser.execute("kkm-1", "admin-pin", "John", UserRole.ADMIN, "0000")
        }
        assertFailsWith<ValidationException> {
            createUser.execute("kkm-1", "admin-pin", "John", UserRole.ADMIN, "1111")
        }
    }

    @Test
    fun testUpdateUserDefaultPin() {
        val existing = KkmUser("user-1", "John", UserRole.CASHIER, 500L)
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns mockk()
        every { authorizeUserUseCase.requireRole("kkm-1", "admin-pin", any()) } returns mockk()
        every { storage.listUsers("kkm-1") } returns listOf(existing)

        assertFailsWith<ValidationException> {
            updateUser.execute("kkm-1", "user-1", "admin-pin", "John New", UserRole.ADMIN, "0000")
        }
        assertFailsWith<ValidationException> {
            updateUser.execute("kkm-1", "user-1", "admin-pin", "John New", UserRole.ADMIN, "1111")
        }
    }

    @Test
    fun testCashierCanUpdateOwnPinButNotRole() {
        val existing = KkmUser("cashier-1", "Cashier", UserRole.CASHIER, 500L)
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns mockk()
        every { storage.findUserByPin("kkm-1", "hash-2222") } returns existing
        every { storage.listUsers("kkm-1") } returns listOf(existing)
        every { storage.updateUser("kkm-1", "cashier-1", "Cashier New", UserRole.CASHIER, any()) } returns true

        val res = updateUser.execute("kkm-1", "cashier-1", "2222", "Cashier New", UserRole.CASHIER, "3333")
        assertEquals("Cashier New", res.name)

        assertFailsWith<ValidationException> {
            updateUser.execute("kkm-1", "cashier-1", "2222", "Cashier New", UserRole.ADMIN, "3333")
        }

        val other = KkmUser("admin-1", "Admin", UserRole.ADMIN, 500L)
        every { storage.listUsers("kkm-1") } returns listOf(existing, other)
        assertFailsWith<ForbiddenException> {
            updateUser.execute("kkm-1", "admin-1", "2222", "Admin New", UserRole.ADMIN, "4444")
        }
    }
}

