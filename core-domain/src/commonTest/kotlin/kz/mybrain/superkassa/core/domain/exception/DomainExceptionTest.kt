package kz.mybrain.superkassa.core.domain.exception

import kz.mybrain.superkassa.core.domain.model.auth.UserRole
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
    fun testErrorMessagesRegistry() {
        // badRequest
        assertEquals("Некорректный запрос", ErrorMessages.badRequest().ru)
        assertEquals("Қате сұраныс", ErrorMessages.badRequest().kk)
        assertEquals("Bad request", ErrorMessages.badRequest().en)

        // kkmNotFound
        assertEquals("ККМ не найдена", ErrorMessages.kkmNotFound().ru)

        // kkmExists
        assertEquals("ККМ уже зарегистрирована", ErrorMessages.kkmExists().ru)

        // shiftNotOpen
        assertEquals("Смена не открыта", ErrorMessages.shiftNotOpen().ru)

        // shiftAlreadyOpen
        assertEquals("Смена уже открыта", ErrorMessages.shiftAlreadyOpen().ru)

        // shiftTooLong
        assertEquals("Смена превышает 24 часа", ErrorMessages.shiftTooLong().ru)

        // systemTimeInvalid
        assertEquals("Неверное системное время", ErrorMessages.systemTimeInvalid().ru)

        // ofdProviderUnknown
        val unknownProvider = ErrorMessages.ofdProviderUnknown("123")
        assertTrue(unknownProvider.ru.contains("123"))
        assertTrue(unknownProvider.kk.contains("123"))
        assertTrue(unknownProvider.en.contains("123"))

        // ofdEnvironmentUnknown
        val unknownEnv = ErrorMessages.ofdEnvironmentUnknown("PROD")
        assertTrue(unknownEnv.ru.contains("PROD"))
        assertTrue(unknownEnv.kk.contains("PROD"))
        assertTrue(unknownEnv.en.contains("PROD"))

        // ofdProviderRequired
        assertEquals("ОФД обязателен", ErrorMessages.ofdProviderRequired().ru)

        // ofdProviderTagInvalid
        val tagInvalid = ErrorMessages.ofdProviderTagInvalid("TAG")
        assertTrue(tagInvalid.ru.contains("TAG"))

        // ofdTokenRequired
        assertEquals("Токен ОФД обязателен", ErrorMessages.ofdTokenRequired().ru)

        // ofdTokenInvalid
        val tokenInvalid = ErrorMessages.ofdTokenInvalid("TOKEN")
        assertTrue(tokenInvalid.ru.contains("TOKEN"))

        // kkmRegistrationRequired
        assertEquals("Регистрационный номер ККМ обязателен", ErrorMessages.kkmRegistrationRequired().ru)

        // kkmFactoryRequired
        assertEquals("Заводской номер ККМ обязателен", ErrorMessages.kkmFactoryRequired().ru)

        // kkmSystemIdRequired
        assertEquals("systemId ККМ обязателен", ErrorMessages.kkmSystemIdRequired().ru)

        // kkmSystemIdInvalid
        val sysIdInvalid = ErrorMessages.kkmSystemIdInvalid("SYS1")
        assertTrue(sysIdInvalid.ru.contains("SYS1"))

        // kkmSystemIdExists
        val sysIdExists = ErrorMessages.kkmSystemIdExists("SYS2")
        assertTrue(sysIdExists.ru.contains("SYS2"))

        // userPinRequired
        assertEquals("PIN пользователя обязателен", ErrorMessages.userPinRequired().ru)

        // userNotFound
        assertEquals("Пользователь не найден или неверный PIN", ErrorMessages.userNotFound().ru)

        // userForbidden
        assertEquals("У пользователя нет прав на операцию", ErrorMessages.userForbidden().ru)

        // userNameRequired
        assertEquals("Имя пользователя обязательно", ErrorMessages.userNameRequired().ru)

        // userRoleRequired
        val roleRequired = ErrorMessages.userRoleRequired(UserRole.ADMIN)
        assertTrue(roleRequired.ru.contains("ADMIN"))

        // userPinConflict
        assertEquals("PIN уже используется", ErrorMessages.userPinConflict().ru)

        // userUpdateEmpty
        assertEquals("Нужно передать хотя бы одно поле для обновления пользователя", ErrorMessages.userUpdateEmpty().ru)

        // kkmDeleteRequiresProgramming
        assertEquals("ККМ должна быть в режиме PROGRAMMING", ErrorMessages.kkmDeleteRequiresProgramming().ru)

        // kkmDeleteShiftOpen
        assertEquals("Смена не закрыта", ErrorMessages.kkmDeleteShiftOpen().ru)

        // kkmDeleteQueueNotEmpty
        assertEquals("Очередь не пустая", ErrorMessages.kkmDeleteQueueNotEmpty().ru)

        // kkmSyncShiftOpen
        assertEquals("Смена не закрыта", ErrorMessages.kkmSyncShiftOpen().ru)

        // kkmSyncQueueNotEmpty
        assertEquals("Очередь не пустая", ErrorMessages.kkmSyncQueueNotEmpty().ru)

        // kkmAutonomousTooLong
        assertEquals("Автономный режим превышает 72 часа", ErrorMessages.kkmAutonomousTooLong().ru)

        // kkmBlocked
        assertEquals("ККМ заблокирована по требованию ОФД", ErrorMessages.kkmBlocked().ru)

        // kkmSettingsRequiresProgramming
        assertEquals("ККМ должна быть в режиме PROGRAMMING", ErrorMessages.kkmSettingsRequiresProgramming().ru)

        // kkmInProgramming
        assertEquals("ККМ в режиме программирования", ErrorMessages.kkmInProgramming().ru)

        // unauthorized
        assertEquals("Не авторизован", ErrorMessages.unauthorized().ru)

        // ofdRequestFailed
        val ofdFailedWithDetails = ErrorMessages.ofdRequestFailed("connection timeout")
        assertTrue(ofdFailedWithDetails.ru.contains("connection timeout"))
        assertTrue(ofdFailedWithDetails.kk.contains("connection timeout"))
        assertTrue(ofdFailedWithDetails.en.contains("connection timeout"))

        val ofdFailedNoDetails = ErrorMessages.ofdRequestFailed(null)
        assertEquals("Ошибка ОФД", ofdFailedNoDetails.ru)
        assertEquals("ОФД қатесі", ofdFailedNoDetails.kk)
        assertEquals("OFD request failed", ofdFailedNoDetails.en)

        val ofdFailedBlankDetails = ErrorMessages.ofdRequestFailed("   ")
        assertEquals("Ошибка ОФД", ofdFailedBlankDetails.ru)

        // measureUnitCodeInvalid
        val measureInvalid = ErrorMessages.measureUnitCodeInvalid("M1")
        assertTrue(measureInvalid.ru.contains("M1"))

        // measureUnitNotFound
        val measureNotFound = ErrorMessages.measureUnitNotFound("M2")
        assertTrue(measureNotFound.ru.contains("M2"))

        // documentNotFound
        assertEquals("Документ не найден или содержимое чека недоступно", ErrorMessages.documentNotFound().ru)

        // okvedRequired
        assertEquals("ОКВЭД обязателен", ErrorMessages.okvedRequired().ru)
    }
}
