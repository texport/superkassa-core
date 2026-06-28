package kz.mybrain.superkassa.core.domain.usecase.user

import io.mockk.every
import io.mockk.mockk
import kz.mybrain.superkassa.core.domain.exception.ConflictException
import kz.mybrain.superkassa.core.domain.exception.NotFoundException
import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.model.auth.KkmUser
import kz.mybrain.superkassa.core.domain.model.auth.UserRole
import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.IdGeneratorPort
import kz.mybrain.superkassa.core.domain.port.PinHasherPort
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.usecase.auth.AuthorizeUserUseCase
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
    }

    @Test
    fun testCreateUserSuccess() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns KkmInfo(id = "kkm-1", createdAt = 0, updatedAt = 0, mode = "ACTIVE", state = "ACTIVE")
        every { authorizeUserUseCase.requireRole("kkm-1", "admin-pin", any()) } returns mockk()
        every { idGenerator.nextId() } returns "user-1"
        every { clock.now() } returns 1000L
        every { pinHasher.hash("user-pin") } returns "hash-1"
        every { storage.createUser("kkm-1", "user-1", "John", UserRole.ADMIN, "user-pin", "hash-1", 1000L) } returns true

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
        every { storage.createUser(any(), any(), any(), any(), any(), any(), any()) } returns false

        assertFailsWith<ConflictException> {
            createUser.execute("kkm-1", "admin-pin", "John", UserRole.ADMIN, "user-pin")
        }
    }

    @Test
    fun testUpdateUserSuccess() {
        val existing = KkmUser("user-1", "John", UserRole.CASHIER, "1111", 500L)
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns mockk()
        every { authorizeUserUseCase.requireRole("kkm-1", "admin-pin", any()) } returns mockk()
        every { storage.listUsers("kkm-1") } returns listOf(existing)
        every { pinHasher.hash("2222") } returns "hash-2"
        every { storage.updateUser("kkm-1", "user-1", "John New", UserRole.ADMIN, "2222", "hash-2") } returns true

        val user = updateUser.execute("kkm-1", "user-1", "admin-pin", "John New", UserRole.ADMIN, "2222")
        assertEquals("John New", user.name)
        assertEquals(UserRole.ADMIN, user.role)
        assertEquals("2222", user.pin)
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
        val target = KkmUser("user-2", "Cashier", UserRole.CASHIER, "1111", 500L)
        val other = KkmUser("user-1", "Cashier 2", UserRole.CASHIER, "2222", 600L)
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns mockk()
        every { authorizeUserUseCase.requireRole("kkm-1", "admin-pin", any()) } returns mockk()
        every { storage.listUsers("kkm-1") } returns listOf(target, other)
        every { storage.deleteUser("kkm-1", "user-2") } returns true

        deleteUser.execute("kkm-1", "user-2", "admin-pin")
    }

    @Test
    fun testDeleteUserRoleRequired() {
        val target = KkmUser("user-1", "Admin", UserRole.ADMIN, "1111", 500L)
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
        val target = KkmUser("user-2", "Cashier", UserRole.CASHIER, "1111", 500L)
        val other = KkmUser("user-1", "Cashier 2", UserRole.CASHIER, "2222", 600L)
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
        val existing = KkmUser("user-1", "John", UserRole.CASHIER, "1111", 500L)
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns mockk()
        every { authorizeUserUseCase.requireRole("kkm-1", "admin-pin", any()) } returns mockk()
        every { storage.listUsers("kkm-1") } returns listOf(existing)

        assertFailsWith<ValidationException> {
            updateUser.execute("kkm-1", "user-1", "admin-pin", "   ", UserRole.ADMIN, "2222")
        }
    }

    @Test
    fun testUpdateUserBlankPin() {
        val existing = KkmUser("user-1", "John", UserRole.CASHIER, "1111", 500L)
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns mockk()
        every { authorizeUserUseCase.requireRole("kkm-1", "admin-pin", any()) } returns mockk()
        every { storage.listUsers("kkm-1") } returns listOf(existing)

        assertFailsWith<ValidationException> {
            updateUser.execute("kkm-1", "user-1", "admin-pin", "John New", UserRole.ADMIN, "")
        }
    }

    @Test
    fun testUpdateUserConflict() {
        val existing = KkmUser("user-1", "John", UserRole.CASHIER, "1111", 500L)
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns mockk()
        every { authorizeUserUseCase.requireRole("kkm-1", "admin-pin", any()) } returns mockk()
        every { storage.listUsers("kkm-1") } returns listOf(existing)
        every { pinHasher.hash("2222") } returns "hash-2"
        every { storage.updateUser("kkm-1", "user-1", "John New", UserRole.ADMIN, "2222", "hash-2") } returns false

        assertFailsWith<ConflictException> {
            updateUser.execute("kkm-1", "user-1", "admin-pin", "John New", UserRole.ADMIN, "2222")
        }
    }

    @Test
    fun testUpdateUserPartialUpdatesAndNullFallbacks() {
        val existing = KkmUser("user-1", "John", UserRole.CASHIER, "1111", 500L)
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns mockk()
        every { authorizeUserUseCase.requireRole("kkm-1", "admin-pin", any()) } returns mockk()
        every { storage.listUsers("kkm-1") } returns listOf(existing)

        assertFailsWith<ValidationException> {
            updateUser.execute("kkm-1", "user-1", "admin-pin", null, null, null)
        }
    }

    @Test
    fun testUpdateUserOnlyOneFieldChanged() {
        val existing = KkmUser("user-1", "John", UserRole.CASHIER, "1111", 500L)
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns mockk()
        every { authorizeUserUseCase.requireRole("kkm-1", "admin-pin", any()) } returns mockk()
        every { storage.listUsers("kkm-1") } returns listOf(existing)
        every { pinHasher.hash("5555") } returns "hash-existing"
        every { storage.updateUser("kkm-1", "user-1", "John", UserRole.CASHIER, "5555", "hash-existing") } returns true

        val user = updateUser.execute("kkm-1", "user-1", "admin-pin", null, null, "5555")
        assertEquals("John", user.name)
        assertEquals(UserRole.CASHIER, user.role)
        assertEquals("5555", user.pin)
    }
}

