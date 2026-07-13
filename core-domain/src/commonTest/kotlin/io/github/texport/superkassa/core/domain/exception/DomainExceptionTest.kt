package io.github.texport.superkassa.core.domain.api.exception


import io.github.texport.superkassa.core.string.api.TrilingualMessage
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DomainExceptionTest {

    private class TestSuperkassaException(
        code: String,
        status: Int,
        trilingualMessage: TrilingualMessage,
        cause: Throwable? = null
    ) : SuperkassaException(code, status, trilingualMessage, cause)

    @Test
    fun testSuperkassaExceptionFormattingAndCause() {
        val msg = TrilingualMessage(ru = "Ошибка", kk = "Қате", en = "Error")
        val cause = IllegalArgumentException("Root cause")
        val ex = TestSuperkassaException("TEST_CODE", 418, msg, cause)

        assertEquals("TEST_CODE", ex.code)
        assertEquals(418, ex.status)
        assertEquals(msg, ex.trilingualMessage)
        assertSame(cause, ex.cause)
        assertEquals("RU: Ошибка | KK: Қате | EN: Error", ex.message)
    }

    @Test
    fun testConflictException() {
        val msg = TrilingualMessage(ru = "Конфликт", kk = "Қайшылық", en = "Conflict")
        val ex1 = ConflictException(msg)
        assertEquals("CONFLICT", ex1.code)
        assertEquals(409, ex1.status)
        assertEquals(msg, ex1.trilingualMessage)

        val ex2 = ConflictException(msg, "CUSTOM_CONFLICT")
        assertEquals("CUSTOM_CONFLICT", ex2.code)
    }

    @Test
    fun testValidationException() {
        val msg = TrilingualMessage(ru = "Валидация", kk = "Валидация", en = "Validation")
        val ex1 = ValidationException(msg)
        assertEquals("BAD_REQUEST", ex1.code)
        assertEquals(400, ex1.status)
        assertEquals(msg, ex1.trilingualMessage)

        val ex2 = ValidationException(msg, "CUSTOM_BAD_REQUEST")
        assertEquals("CUSTOM_BAD_REQUEST", ex2.code)
    }

    @Test
    fun testNotFoundException() {
        val msg = TrilingualMessage(ru = "Не найдено", kk = "Табылмады", en = "Not found")
        val ex1 = NotFoundException(msg)
        assertEquals("NOT_FOUND", ex1.code)
        assertEquals(404, ex1.status)
        assertEquals(msg, ex1.trilingualMessage)

        val ex2 = NotFoundException(msg, "CUSTOM_NOT_FOUND")
        assertEquals("CUSTOM_NOT_FOUND", ex2.code)
    }

    @Test
    fun testSettingsFrozenException() {
        val text = "Settings frozen"
        val ex1 = SettingsFrozenException(text)
        assertEquals("SETTINGS_FROZEN", ex1.code)
        assertEquals(403, ex1.status)
        assertEquals(text, ex1.messageText)
        assertEquals(text, ex1.trilingualMessage.ru)
        assertEquals(text, ex1.trilingualMessage.kk)
        assertEquals(text, ex1.trilingualMessage.en)

        val ex2 = SettingsFrozenException(text, "CUSTOM_FROZEN")
        assertEquals("CUSTOM_FROZEN", ex2.code)
    }

    @Test
    fun testStorageException() {
        val msg = TrilingualMessage(ru = "БД", kk = "БД", en = "DB")
        val cause = RuntimeException("DB error")
        val ex1 = StorageException(msg, cause = cause)
        assertEquals("STORAGE_ERROR", ex1.code)
        assertEquals(500, ex1.status)
        assertEquals(msg, ex1.trilingualMessage)
        assertSame(cause, ex1.cause)

        val ex2 = StorageException(msg, "CUSTOM_STORAGE_ERROR")
        assertEquals("CUSTOM_STORAGE_ERROR", ex2.code)
        assertNull(ex2.cause)
    }

    @Test
    fun testForbiddenException() {
        val msg = TrilingualMessage(ru = "Запрещено", kk = "Тыйым салынған", en = "Forbidden")
        val ex1 = ForbiddenException(msg)
        assertEquals("FORBIDDEN", ex1.code)
        assertEquals(403, ex1.status)
        assertEquals(msg, ex1.trilingualMessage)

        val ex2 = ForbiddenException(msg, "CUSTOM_FORBIDDEN")
        assertEquals("CUSTOM_FORBIDDEN", ex2.code)
    }

    @Test
    fun testTrilingualMessageFormat() {
        val template = TrilingualMessage(
            ru = "Ошибка %s в %d",
            kk = "%s қатесі %d-де",
            en = "Error %s in %d"
        )
        val formatted = template.format("DB", 5)
        assertEquals("Ошибка DB в 5", formatted.ru)
        assertEquals("DB қатесі 5-де", formatted.kk)
        assertEquals("Error DB in 5", formatted.en)
    }

    @Test
    fun testCoreStringsRegistry() {
        // badRequest
        assertEquals("Некорректный запрос", CoreStrings.badRequest().ru)
        assertEquals("Қате сұраныс", CoreStrings.badRequest().kk)
        assertEquals("Bad request", CoreStrings.badRequest().en)

        // kkmNotFound
        assertEquals("ККМ не найдена", CoreStrings.kkmNotFound().ru)

        // kkmExists
        assertEquals("ККМ уже зарегистрирована", CoreStrings.kkmExists().ru)

        // shiftNotOpen
        assertEquals("Смена не открыта", CoreStrings.shiftNotOpen().ru)

        // shiftAlreadyOpen
        assertEquals("Смена уже открыта", CoreStrings.shiftAlreadyOpen().ru)

        // shiftTooLong
        assertEquals("Смена превышает 24 часа", CoreStrings.shiftTooLong().ru)

        // systemTimeInvalid
        assertEquals("Неверное системное время", CoreStrings.systemTimeInvalid().ru)

        // ofdProviderUnknown
        val unknownProvider = CoreStrings.ofdProviderUnknown("123")
        assertTrue(unknownProvider.ru.contains("123"))
        assertTrue(unknownProvider.kk.contains("123"))
        assertTrue(unknownProvider.en.contains("123"))

        // ofdEnvironmentUnknown
        val unknownEnv = CoreStrings.ofdEnvironmentUnknown("PROD")
        assertTrue(unknownEnv.ru.contains("PROD"))
        assertTrue(unknownEnv.kk.contains("PROD"))
        assertTrue(unknownEnv.en.contains("PROD"))

        // ofdProviderRequired
        assertEquals("ОФД обязателен", CoreStrings.ofdProviderRequired().ru)

        // ofdProviderTagInvalid
        val tagInvalid = CoreStrings.ofdProviderTagInvalid("TAG")
        assertTrue(tagInvalid.ru.contains("TAG"))

        // ofdTokenRequired
        assertEquals("Токен ОФД обязателен", CoreStrings.ofdTokenRequired().ru)

        // ofdTokenInvalid
        val tokenInvalid = CoreStrings.ofdTokenInvalid("TOKEN")
        assertTrue(tokenInvalid.ru.contains("TOKEN"))

        // kkmRegistrationRequired
        assertEquals("Регистрационный номер ККМ обязателен", CoreStrings.kkmRegistrationRequired().ru)

        // kkmFactoryRequired
        assertEquals("Заводской номер ККМ обязателен", CoreStrings.kkmFactoryRequired().ru)

        // kkmSystemIdRequired
        assertEquals("systemId ККМ обязателен", CoreStrings.kkmSystemIdRequired().ru)

        // kkmSystemIdInvalid
        val sysIdInvalid = CoreStrings.kkmSystemIdInvalid("SYS1")
        assertTrue(sysIdInvalid.ru.contains("SYS1"))

        // kkmSystemIdExists
        val sysIdExists = CoreStrings.kkmSystemIdExists("SYS2")
        assertTrue(sysIdExists.ru.contains("SYS2"))

        // userPinRequired
        assertEquals("PIN пользователя обязателен", CoreStrings.userPinRequired().ru)

        // userNotFound
        assertEquals("Пользователь не найден или неверный PIN", CoreStrings.userNotFound().ru)

        // userForbidden
        assertEquals("У пользователя нет прав на операцию", CoreStrings.userForbidden().ru)

        // userNameRequired
        assertEquals("Имя пользователя обязательно", CoreStrings.userNameRequired().ru)

        // userRoleRequired
        val roleRequired = CoreStrings.userRoleRequired(UserRole.ADMIN.name)
        assertTrue(roleRequired.ru.contains("ADMIN"))

        // userPinConflict
        assertEquals("PIN уже используется", CoreStrings.userPinConflict().ru)

        // userUpdateEmpty
        assertEquals("Нужно передать хотя бы одно поле для обновления пользователя", CoreStrings.userUpdateEmpty().ru)

        // kkmDeleteRequiresProgramming
        assertEquals("ККМ должна быть в режиме PROGRAMMING", CoreStrings.kkmDeleteRequiresProgramming().ru)

        // kkmDeleteShiftOpen
        assertEquals("Смена не закрыта", CoreStrings.kkmDeleteShiftOpen().ru)

        // kkmDeleteQueueNotEmpty
        assertEquals("Очередь не пустая", CoreStrings.kkmDeleteQueueNotEmpty().ru)

        // kkmSyncShiftOpen
        assertEquals("Смена не закрыта", CoreStrings.kkmSyncShiftOpen().ru)

        // kkmSyncQueueNotEmpty
        assertEquals("Очередь не пустая", CoreStrings.kkmSyncQueueNotEmpty().ru)

        // kkmAutonomousTooLong
        assertEquals("Автономный режим превышает 72 часа", CoreStrings.kkmAutonomousTooLong().ru)

        // kkmBlocked
        assertEquals("ККМ заблокирована по требованию ОФД", CoreStrings.kkmBlocked().ru)

        // kkmSettingsRequiresProgramming
        assertEquals("ККМ должна быть в режиме PROGRAMMING", CoreStrings.kkmSettingsRequiresProgramming().ru)

        // kkmInProgramming
        assertEquals("ККМ в режиме программирования", CoreStrings.kkmInProgramming().ru)

        // unauthorized
        assertEquals("Не авторизован", CoreStrings.unauthorized().ru)

        // ofdRequestFailed
        val ofdFailedWithDetails = CoreStrings.ofdRequestFailed("connection timeout")
        assertTrue(ofdFailedWithDetails.ru.contains("connection timeout"))
        assertTrue(ofdFailedWithDetails.kk.contains("connection timeout"))
        assertTrue(ofdFailedWithDetails.en.contains("connection timeout"))

        val ofdFailedNoDetails = CoreStrings.ofdRequestFailed(null)
        assertEquals("Ошибка ОФД", ofdFailedNoDetails.ru)
        assertEquals("ОФД қатесі", ofdFailedNoDetails.kk)
        assertEquals("OFD request failed", ofdFailedNoDetails.en)

        val ofdFailedBlankDetails = CoreStrings.ofdRequestFailed("   ")
        assertEquals("Ошибка ОФД", ofdFailedBlankDetails.ru)

        // measureUnitCodeInvalid
        val measureInvalid = CoreStrings.measureUnitCodeInvalid("M1")
        assertTrue(measureInvalid.ru.contains("M1"))

        // measureUnitNotFound
        val measureNotFound = CoreStrings.measureUnitNotFound("M2")
        assertTrue(measureNotFound.ru.contains("M2"))

        // documentNotFound
        assertEquals("Документ не найден или содержимое чека недоступно", CoreStrings.documentNotFound().ru)

        // okvedRequired
        assertEquals("ОКВЭД обязателен", CoreStrings.okvedRequired().ru)
    }
}
