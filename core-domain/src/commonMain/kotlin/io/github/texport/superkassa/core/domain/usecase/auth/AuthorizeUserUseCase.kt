package io.github.texport.superkassa.core.domain.usecase.auth

import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.domain.exception.ForbiddenException
import io.github.texport.superkassa.core.domain.exception.ValidationException
import io.github.texport.superkassa.core.domain.exception.NotFoundException
import io.github.texport.superkassa.core.domain.model.auth.UserRole
import io.github.texport.superkassa.core.domain.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.port.PinHasherPort
import io.github.texport.superkassa.core.domain.port.StoragePort

/**
 * Сценарий проверки прав доступа (авторизации) пользователя ККМ.
 *
 * Предоставляет методы для валидации ПИН-кода кассира/администратора,
 * сопоставления его с ролью пользователя и контроля уровня доступа к различным функциям ККМ.
 *
 * @property storage Порт для доступа к хранилищу данных (пользователи, ККМ).
 * @property pinHasher Порт для безопасного хеширования ПИН-кодов перед сравнением с БД.
 */
class AuthorizeUserUseCase(
    private val storage: StoragePort,
    private val pinHasher: PinHasherPort
) {
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
        if (pin.isBlank()) {
            throw ValidationException(CoreStrings.userPinRequired(), "PIN_REQUIRED")
        }
        if (!allowDefaultPin && (pin == "0000" || pin == "1111")) {
            throw ValidationException(CoreStrings.defaultPinNotAllowed(), "DEFAULT_PIN_NOT_ALLOWED")
        }
        val pinHash = pinHasher.hash(pin)
        val user = storage.findUserByPin(kkmId, pinHash)
            ?: throw ForbiddenException(CoreStrings.userNotFound(), "USER_NOT_FOUND")
        if (!allowed.contains(user.role)) {
            throw ForbiddenException(CoreStrings.userForbidden(), "USER_FORBIDDEN")
        }
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
