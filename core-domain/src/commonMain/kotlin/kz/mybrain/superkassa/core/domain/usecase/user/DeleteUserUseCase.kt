package kz.mybrain.superkassa.core.domain.usecase.user

import kz.mybrain.superkassa.core.domain.exception.ErrorMessages
import kz.mybrain.superkassa.core.domain.exception.NotFoundException
import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.model.auth.UserRole
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.usecase.auth.AuthorizeUserUseCase

/**
 * Сценарий (Use Case) удаления пользователя ККМ.
 *
 * Отвечает за безопасное удаление пользователя из БД кассы. Предотвращает удаление
 * последнего пользователя с определенной ролью (например, на кассе должен оставаться
 * как минимум один администратор). Требует прав администратора.
 *
 * @property storage Порт доступа к локальной БД для управления пользователями кассы.
 * @property authorizeUserUseCase Сценарий авторизации оператора ККМ.
 */
class DeleteUserUseCase(
    private val storage: StoragePort,
    private val authorizeUserUseCase: AuthorizeUserUseCase
) {
    /**
     * Выполняет сценарий удаления пользователя с ККМ.
     *
     * @param kkmId Идентификатор кассы.
     * @param userId Идентификатор удаляемого пользователя.
     * @param pin ПИН-код администратора, совершающего удаление.
     * @throws NotFoundException Если удаляемый пользователь не найден на данной кассе.
     * @throws ValidationException Если удаление данного пользователя нарушит требование обязательного наличия ролей.
     */
    fun execute(kkmId: String, userId: String, pin: String) {
        authorizeUserUseCase.requireKkm(kkmId)
        authorizeUserUseCase.requireRole(kkmId, pin, setOf(UserRole.ADMIN))

        val users = storage.listUsers(kkmId)
        val target = users.firstOrNull { it.id == userId }
            ?: throw NotFoundException(ErrorMessages.userNotFound(), "USER_NOT_FOUND")

        ensureRolePresence(kkmId, target.role, userId)

        val success = storage.deleteUser(kkmId, userId)
        if (!success) {
            throw NotFoundException(ErrorMessages.userNotFound(), "USER_NOT_FOUND")
        }
    }

    /**
     * Гарантирует, что после удаления пользователя на кассе останется хотя бы один пользователь с данной ролью.
     *
     * @param kkmId Идентификатор кассы.
     * @param role Проверяемая роль.
     * @param excludeUserId Идентификатор удаляемого пользователя, который исключается из подсчета.
     * @throws ValidationException Если не остается ни одного пользователя с указанной ролью.
     */
    private fun ensureRolePresence(kkmId: String, role: UserRole, excludeUserId: String) {
        val users = storage.listUsers(kkmId).filter { it.id != excludeUserId }
        val remaining = users.count { it.role == role }
        if (remaining == 0) {
            throw ValidationException(ErrorMessages.userRoleRequired(role), "USER_ROLE_REQUIRED")
        }
    }
}
