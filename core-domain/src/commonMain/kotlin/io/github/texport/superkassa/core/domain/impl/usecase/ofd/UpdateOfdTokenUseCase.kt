package io.github.texport.superkassa.core.domain.impl.usecase.ofd

import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.api.port.internal.TokenCodecPort
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.exception.ForbiddenException
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState

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
        authorizeUserUseCase.requireRole(
            kkmId,
            pin,
            setOf(io.github.texport.superkassa.core.domain.api.model.auth.UserRole.ADMIN)
        )

        // Разбираем токен с помощью кодека
        val parsed = tokenCodec.parseToken(token)

        // Кодируем токен в Base64 и сохраняем информацию в базу данных ККМ
        val saved = storage.updateKkmToken(
            id = kkmId,
            tokenEncryptedBase64 = tokenCodec.encodeToken(parsed),
            updatedAt = clock.now()
        )
        if (saved) {
            releaseTokenBlock(kkmId)
        }
        return saved
    }

    /**
     * Снимает блокировку, наложенную из-за неверного токена.
     *
     * Спецификация CPCR, код 2: «Отправка данных невозможна, необходимо
     * произвести сброс токена. Устройство блокируется до момента ввода
     * корректного токена». Раньше касса принимала верный токен и оставалась
     * заблокированной навсегда — выхода из этого состояния не было вовсе.
     *
     * Блокировки по другим причинам вводом токена не снимаются: касса,
     * снятая с учёта или заблокированная сервером, так не оживает.
     */
    private fun releaseTokenBlock(kkmId: String) {
        val kkm = storage.findKkmForUpdate(kkmId) ?: return
        // Причина блокировки, а не состояние: токен вводят в режиме
        // программирования, и в этот момент касса числится не BLOCKED,
        // а PROGRAMMING. Пока проверялось состояние, замена токена
        // из настроек причину не снимала.
        if (kkm.blockReasonCode != INVALID_TOKEN_BLOCK) {
            return
        }
        if (kkm.state == KkmState.PROGRAMMING.name) {
            storage.updateKkm(kkm.copy(updatedAt = clock.now(), blockReasonCode = null))
            return
        }
        val hasOpenShift = storage.findOpenShift(kkmId) != null
        storage.updateKkm(
            kkm.copy(
                updatedAt = clock.now(),
                state = if (hasOpenShift) KkmState.ACTIVE.name else KkmState.IDLE.name,
                blockReasonCode = null
            )
        )
    }

    private companion object {
        /** Блокировка по ответу ОФД «неверный токен»: код ответа плюс тысяча. */
        const val INVALID_TOKEN_BLOCK = 1002
    }
}
