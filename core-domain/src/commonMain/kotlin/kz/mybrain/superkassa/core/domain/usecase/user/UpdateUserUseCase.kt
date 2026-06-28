package kz.mybrain.superkassa.core.domain.usecase.user

import kz.mybrain.superkassa.core.domain.exception.ConflictException
import kz.mybrain.superkassa.core.domain.exception.ErrorMessages
import kz.mybrain.superkassa.core.domain.exception.NotFoundException
import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.model.auth.KkmUser
import kz.mybrain.superkassa.core.domain.model.auth.UserRole
import kz.mybrain.superkassa.core.domain.port.PinHasherPort
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.usecase.auth.AuthorizeUserUseCase

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
     * @throws ValidationException Если не передано ни одного параметра для изменения, либо новые значения имени/ПИН-кода пусты.
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
        authorizeUserUseCase.requireRole(kkmId, pin, setOf(UserRole.ADMIN), allowDefaultPin = true)
        if (userPin != null && (userPin == "0000" || userPin == "1111")) {
            throw ValidationException(ErrorMessages.defaultPinNotAllowed(), "DEFAULT_PIN_NOT_ALLOWED")
        }
        if (name == null && role == null && userPin == null) {
            throw ValidationException(ErrorMessages.userUpdateEmpty(), "USER_UPDATE_EMPTY")
        }
        val existing = storage.listUsers(kkmId).firstOrNull { it.id == userId }
            ?: throw NotFoundException(ErrorMessages.userNotFound(), "USER_NOT_FOUND")

        val updatedName = name ?: existing.name
        if (updatedName.isBlank()) {
            throw ValidationException(ErrorMessages.userNameRequired(), "USER_NAME_REQUIRED")
        }

        val updatedRole = role ?: existing.role
        val updatedPin = userPin ?: existing.pin
        if (updatedPin.isNullOrBlank()) {
            throw ValidationException(ErrorMessages.userPinRequired(), "USER_PIN_REQUIRED")
        }

        val success = storage.updateUser(
            kkmId = kkmId,
            userId = userId,
            name = updatedName,
            role = updatedRole,
            pin = updatedPin,
            pinHash = pinHasher.hash(updatedPin)
        )
        if (!success) {
            throw ConflictException(ErrorMessages.userPinConflict(), "USER_PIN_CONFLICT")
        }
        return KkmUser(
            id = userId,
            name = updatedName,
            role = updatedRole,
            pin = updatedPin,
            createdAt = existing.createdAt
        )
    }
}
