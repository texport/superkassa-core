package kz.mybrain.superkassa.core.domain.usecase.ofd

import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.port.TokenCodecPort
import kz.mybrain.superkassa.core.domain.usecase.auth.AuthorizeUserUseCase
import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.exception.ForbiddenException

/**
 * Сценарий (Use Case) обновления токена авторизации ОФД.
 *
 * Позволяет обновить токен доступа к ОФД, используемый для авторизации запросов.
 * Новое значение токена кодируется/шифруется и сохраняется в ККМ.
 * Доступно только Администраторам.
 *
 * @property storage Порт для доступа к локальному хранилищу данных ККМ.
 * @property clock Порт для работы с системным временем.
 * @property tokenCodec Кодек для разбора и шифрования токенов ОФД.
 * @property authorizeUserUseCase Сценарий проверки прав доступа и состояния ККМ.
 */
@Suppress("unused")
class UpdateOfdTokenUseCase(
    private val storage: StoragePort,
    private val clock: ClockPort,
    private val tokenCodec: TokenCodecPort,
    private val authorizeUserUseCase: AuthorizeUserUseCase
) {
    /**
     * Выполняет процедуру обновления токена ОФД.
     *
     * @param kkmId Уникальный идентификатор ККМ.
     * @param pin PIN-код администратора для проверки прав.
     * @param token Строковое представление нового токена ОФД.
     * @return [Boolean] true, если токен успешно сохранен в базе данных, иначе false.
     * @throws ValidationException при неверном пин-коде или ошибке разбора токена.
     * @throws ForbiddenException при отсутствии прав администратора.
     */
    fun execute(kkmId: String, pin: String, token: String): Boolean {
        // Проверяем наличие роли Администратора
        authorizeUserUseCase.requireRole(kkmId, pin, setOf(kz.mybrain.superkassa.core.domain.model.auth.UserRole.ADMIN))

        // Разбираем токен с помощью кодека
        val parsed = tokenCodec.parseToken(token)

        // Кодируем токен в Base64 и сохраняем информацию в базу данных ККМ
        return storage.updateKkmToken(
            id = kkmId,
            tokenEncryptedBase64 = tokenCodec.encodeToken(parsed),
            updatedAt = clock.now()
        )
    }
}
