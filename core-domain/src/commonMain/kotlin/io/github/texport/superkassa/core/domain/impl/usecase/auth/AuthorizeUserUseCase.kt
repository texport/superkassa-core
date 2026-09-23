package io.github.texport.superkassa.core.domain.impl.usecase.auth

import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.domain.api.exception.ForbiddenException
import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.exception.NotFoundException
import io.github.texport.superkassa.core.domain.api.exception.PinLockedException
import io.github.texport.superkassa.core.domain.api.model.auth.KkmUser
import io.github.texport.superkassa.core.domain.api.model.auth.StandardPin
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.port.internal.PinHasherPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort

import io.github.texport.superkassa.core.domain.impl.logging.getLogger

/**
 * Сценарий проверки прав доступа (авторизации) пользователя ККМ.
 *
 * Предоставляет методы для валидации ПИН-кода кассира/администратора,
 * сопоставления его с ролью пользователя и контроля уровня доступа к различным функциям ККМ.
 *
 * Пин проверяется только на незапертой кассе: неверные пины подряд
 * запирают её на время, см. [PinGuard].
 *
 * @property storage Порт для доступа к хранилищу данных (пользователи, ККМ).
 * @property pinHasher Порт для безопасного хеширования ПИН-кодов перед сравнением с БД.
 * @property pinGuard Счёт неверных пинов; один на сборку ядра.
 */
class AuthorizeUserUseCase(
    private val storage: StoragePort,
    private val pinHasher: PinHasherPort,
    private val pinGuard: PinGuard = PinGuard.of(storage)
) {
    private val logger = getLogger(AuthorizeUserUseCase::class)

    /**
     * Проверяет, существует ли пользователь с указанным ПИН-кодом на данной кассе и обладает ли он нужной ролью.
     *
     * @param kkmId Уникальный идентификатор ККМ.
     * @param pin ПИН-код пользователя для проверки.
     * @param allowed Множество разрешенных ролей [UserRole] для выполнения операции.
     * @param allowDefaultPin Разрешить использование стандартных/дефолтных ПИН-кодов ("0000", "1111").
     * @throws ValidationException Если передан пустой ПИН-код или стандартный ПИН-код, когда они запрещены.
     * @throws ForbiddenException Если пользователь не найден или его роль не входит в список разрешенных.
     */
    fun execute(kkmId: String, pin: String, allowed: Set<UserRole>, allowDefaultPin: Boolean = false) {
        logger.info(
            "AuthorizeUserUseCase.execute: validating authorization for kkmId='{}', allowedRoles={}",
            kkmId,
            allowed
        )
        val user = identify(kkmId, pin, allowDefaultPin)
        if (!allowed.contains(user.role)) {
            logger.warn(
                "AuthorizeUserUseCase.execute FAILED: user role='{}' forbidden for kkmId='{}'",
                user.role,
                kkmId
            )
            throw ForbiddenException(CoreStrings.userForbidden(), "USER_FORBIDDEN")
        }
        logger.info(
            "AuthorizeUserUseCase.execute SUCCESS: user userId='{}', role='{}' authorized for kkmId='{}'",
            user.id,
            user.role,
            kkmId
        )
    }

    /**
     * Узнаёт, кто стоит за ПИН-кодом, не спрашивая о роли.
     *
     * Нужен там, где роль не проверяется, а выясняется: рабочее место
     * показывает кассиру только доступные ему разделы, а не встречает
     * его отказами на тех, куда ему нельзя.
     *
     * @param kkmId Идентификатор кассы.
     * @param pin ПИН-код пользователя.
     * @param allowDefaultPin Разрешить стандартные ПИН-коды.
     * @return Пользователь кассы [KkmUser].
     * @throws ValidationException Если ПИН-код пуст или стандартный, когда они запрещены.
     * @throws ForbiddenException Если пользователя с таким ПИН-кодом на кассе нет.
     * @throws PinLockedException Если касса заперта после неверных пинов подряд.
     */
    fun identify(kkmId: String, pin: String, allowDefaultPin: Boolean = false): KkmUser {
        if (pin.isBlank()) {
            logger.warn("AuthorizeUserUseCase.identify FAILED: empty PIN provided for kkmId='{}'", kkmId)
            throw ValidationException(CoreStrings.userPinRequired(), "PIN_REQUIRED")
        }
        if (!allowDefaultPin && StandardPin.isStandard(pin)) {
            logger.warn("AuthorizeUserUseCase.identify FAILED: default PIN used when forbidden for kkmId='{}'", kkmId)
            throw ValidationException(CoreStrings.defaultPinNotAllowed(), "DEFAULT_PIN_NOT_ALLOWED")
        }
        val user = pinGuard.check(kkmId) { storage.findUserByPin(kkmId, pinHasher.hash(pin)) }
        if (user == null) {
            logger.warn("AuthorizeUserUseCase.identify FAILED: no user found for kkmId='{}'", kkmId)
            throw ForbiddenException(CoreStrings.userNotFound(), "USER_NOT_FOUND")
        }
        return user
    }

    /**
     * Требует наличие кассы с указанным идентификатором в БД.
     *
     * @param kkmId Идентификатор кассы.
     * @param forUpdate Флаг пессимистической блокировки.
     * @return Информация о ККМ [KkmInfo].
     * @throws NotFoundException Если касса с указанным [kkmId] не найдена.
     */
    fun requireKkm(kkmId: String, forUpdate: Boolean = false): KkmInfo {
        val kkm = if (forUpdate) storage.findKkmForUpdate(kkmId) else storage.findKkm(kkmId)
        return kkm ?: throw NotFoundException(CoreStrings.kkmNotFound(), "KKM_NOT_FOUND")
    }

    /**
     * Требует наличие определенной роли для выполнения операции на указанной кассе.
     *
     * Вспомогательный метод, делегирующий вызов [execute].
     *
     * @param kkmId Идентификатор кассы.
     * @param pin ПИН-код пользователя.
     * @param allowed Список разрешенных ролей.
     * @param allowDefaultPin Флаг разрешения использования стандартных ПИН-кодов.
     * @throws ValidationException Если ПИН-код не передан.
     * @throws ForbiddenException Если доступ запрещен.
     */
    fun requireRole(kkmId: String, pin: String, allowed: Set<UserRole>, allowDefaultPin: Boolean = false) {
        execute(kkmId, pin, allowed, allowDefaultPin)
    }
}
