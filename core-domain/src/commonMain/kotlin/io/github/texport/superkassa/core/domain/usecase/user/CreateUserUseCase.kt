package io.github.texport.superkassa.core.domain.usecase.user

import io.github.texport.superkassa.core.domain.exception.ConflictException
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.domain.exception.ValidationException
import io.github.texport.superkassa.core.domain.model.auth.KkmUser
import io.github.texport.superkassa.core.domain.model.auth.UserRole
import io.github.texport.superkassa.core.domain.port.ClockPort
import io.github.texport.superkassa.core.domain.port.IdGeneratorPort
import io.github.texport.superkassa.core.domain.port.PinHasherPort
import io.github.texport.superkassa.core.domain.port.StoragePort
import io.github.texport.superkassa.core.domain.usecase.auth.AuthorizeUserUseCase

/**
 * Сценарий (Use Case) создания нового пользователя кассового аппарата (ККМ).
 *
 * Отвечает за валидацию данных нового пользователя (имя, ПИН-код), хеширование ПИН-кода,
 * генерацию уникального идентификатора пользователя и сохранение записи в локальную БД.
 * Требует авторизации с ролью администратора ККМ.
 *
 * @property storage Порт доступа к локальной БД для работы с пользователями.
 * @property idGenerator Порт генерации уникальных идентификаторов.
 * @property clock Порт для работы с системным временем.
 * @property pinHasher Порт хеширования ПИН-кодов пользователей.
 * @property authorizeUserUseCase Сценарий авторизации и проверки прав текущего оператора.
 */
class CreateUserUseCase(
    private val storage: StoragePort,
    private val idGenerator: IdGeneratorPort,
    private val clock: ClockPort,
    private val pinHasher: PinHasherPort,
    private val authorizeUserUseCase: AuthorizeUserUseCase
) {
    /**
     * Выполняет сценарий создания нового пользователя ККМ.
     *
     * @param kkmId Идентификатор кассы.
     * @param pin ПИН-код администратора, выполняющего операцию.
     * @param name Имя создаваемого пользователя (кассира/администратора).
     * @param role Роль создаваемого пользователя (например, [UserRole.CASHIER]).
     * @param userPin Персональный ПИН-код создаваемого пользователя.
     * @return Созданный объект пользователя [KkmUser].
     * @throws ValidationException Если имя или ПИН-код нового пользователя пусты.
     * @throws ConflictException Если пользователь с таким ПИН-кодом уже существует в рамках кассы.
     */
    fun execute(kkmId: String, pin: String, name: String, role: UserRole, userPin: String): KkmUser {
        authorizeUserUseCase.requireKkm(kkmId)
        authorizeUserUseCase.requireRole(kkmId, pin, setOf(UserRole.ADMIN))
        if (name.isBlank()) {
            throw ValidationException(CoreStrings.userNameRequired(), "USER_NAME_REQUIRED")
        }
        if (userPin.isBlank()) {
            throw ValidationException(CoreStrings.userPinRequired(), "USER_PIN_REQUIRED")
        }
        if (userPin == "0000" || userPin == "1111") {
            throw ValidationException(CoreStrings.defaultPinNotAllowed(), "DEFAULT_PIN_NOT_ALLOWED")
        }
        val now = clock.now()
        val userId = idGenerator.nextId()
        val created = storage.createUser(
            kkmId = kkmId,
            userId = userId,
            name = name,
            role = role,
            pin = userPin,
            pinHash = pinHasher.hash(userPin),
            createdAt = now
        )
        if (!created) {
            throw ConflictException(CoreStrings.userPinConflict(), "USER_PIN_CONFLICT")
        }
        return KkmUser(id = userId, name = name, role = role, pin = userPin, createdAt = now)
    }
}
