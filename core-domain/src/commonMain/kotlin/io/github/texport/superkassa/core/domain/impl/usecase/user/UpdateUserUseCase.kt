package io.github.texport.superkassa.core.domain.impl.usecase.user

import io.github.texport.superkassa.core.domain.api.exception.ConflictException
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.domain.api.exception.ForbiddenException
import io.github.texport.superkassa.core.domain.api.exception.NotFoundException
import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.auth.KkmUser
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.port.internal.PinHasherPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.auth.ChosenPin

/**
 * Сценарий (Use Case) обновления данных существующего пользователя ККМ.
 *
 * Отвечает за изменение имени оператора, его роли или персонального ПИН-кода с проверкой прав,
 * повторное хеширование нового ПИН-кода при его изменении и валидацию на наличие коллизий по ПИН-кодам.
 *
 * @property storage Порт доступа к локальной БД для изменения параметров пользователей.
 * @property pinHasher Порт хеширования ПИН-кодов.
 * @property authorizeUserUseCase Сценарий авторизации и валидации ролей текущего оператора.
 */
class UpdateUserUseCase(
    private val storage: StoragePort,
    private val pinHasher: PinHasherPort,
    private val authorizeUserUseCase: AuthorizeUserUseCase
) {
    /**
     * Выполняет сценарий изменения учетных данных пользователя ККМ.
     *
     * @param kkmId Идентификатор кассы.
     * @param userId Идентификатор обновляемого пользователя.
     * @param pin ПИН-код администратора для авторизации операции.
     * @param name Новое имя пользователя (опционально, если не меняется).
     * @param role Новая роль пользователя (опционально, если не меняется).
     * @param userPin Новой персональный ПИН-код пользователя (опционально, если не меняется).
     * @return Обновленный объект пользователя [KkmUser].
     * @throws ValidationException Если не передано ни одного параметра для изменения, либо новое имя пусто, либо новый ПИН-код не проходит [ChosenPin].
     * @throws NotFoundException Если пользователь с указанным ID не найден на данной кассе.
     * @throws ConflictException Если новый ПИН-код конфликтует с ПИН-кодом другого пользователя кассы.
     */
    fun execute(
        kkmId: String,
        userId: String,
        pin: String,
        name: String?,
        role: UserRole?,
        userPin: String?
    ): KkmUser {
        authorizeUserUseCase.requireKkm(kkmId)
        // Через общий вход по пину: мимо него пин перебирался бы без блокировки.
        val caller = authorizeUserUseCase.identify(kkmId, pin)

        if (caller.role != UserRole.ADMIN) {
            if (caller.id != userId) {
                throw ForbiddenException(CoreStrings.userForbidden(), "USER_FORBIDDEN")
            }
            if (role != null && role != caller.role) {
                throw ValidationException(CoreStrings.cashierCannotChangeRole(), "CASHIER_CANNOT_CHANGE_ROLE")
            }
        }

        if (name == null && role == null && userPin == null) {
            throw ValidationException(CoreStrings.userUpdateEmpty(), "USER_UPDATE_EMPTY")
        }
        val existing = storage.listUsers(kkmId).firstOrNull { it.id == userId }
            ?: throw NotFoundException(CoreStrings.userNotFound(), "USER_NOT_FOUND")

        val updatedName = name ?: existing.name
        if (updatedName.isBlank()) {
            throw ValidationException(CoreStrings.userNameRequired(), "USER_NAME_REQUIRED")
        }

        val updatedRole = role ?: existing.role
        userPin?.let { ChosenPin.require(it) }

        // Пин не пересчитывается, когда его не меняют: узел хранит только хеш,
        // а прежний пин взять неоткуда. Хранилище понимает null как «не трогать».
        val success = storage.updateUser(
            kkmId = kkmId,
            userId = userId,
            name = updatedName,
            role = updatedRole,
            pinHash = userPin?.let { pinHasher.hash(it) }
        )
        if (!success) {
            throw ConflictException(CoreStrings.userPinConflict(), "USER_PIN_CONFLICT")
        }
        return KkmUser(
            id = userId,
            name = updatedName,
            role = updatedRole,
            createdAt = existing.createdAt
        )
    }
}
