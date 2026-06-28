package kz.mybrain.superkassa.core.domain.usecase.kkm

import kz.mybrain.superkassa.core.domain.exception.ConflictException
import kz.mybrain.superkassa.core.domain.exception.ErrorMessages
import kz.mybrain.superkassa.core.domain.exception.NotFoundException
import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kz.mybrain.superkassa.core.domain.model.kkm.KkmState
import kz.mybrain.superkassa.core.domain.port.OfflineQueuePort
import kz.mybrain.superkassa.core.domain.port.StoragePort

/**
 * Сценарий удаления (вывода из эксплуатации) ККМ.
 *
 * Удаление ККМ разрешено только при выполнении следующих условий:
 * 1. Касса находится в состоянии [KkmState.PROGRAMMING].
 * 2. Смена на кассе полностью закрыта (нет открытых смен).
 * 3. Очередь автономных команд/документов пуста и может отправлять запросы напрямую.
 *
 * @property storage Порт для доступа к хранилищу данных ККМ.
 * @property queue Порт для проверки и управления очередью команд ККМ.
 */
class DecommissionKkmUseCase(
    private val storage: StoragePort,
    private val queue: OfflineQueuePort
) {
    /**
     * Выполняет процедуру вывода кассы из эксплуатации (полное удаление).
     *
     * @param kkm Информация об удаляемой ККМ.
     * @return `true`, если касса была успешно удалена.
     * @throws ValidationException Если касса находится не в режиме программирования.
     * @throws ConflictException Если открыта смена или очередь команд не пуста.
     * @throws NotFoundException Если удаляемая касса не найдена в хранилище.
     */
    fun execute(kkm: KkmInfo): Boolean {
        // Проверка: касса должна быть в режиме программирования
        if (kkm.state != KkmState.PROGRAMMING.name) {
            throw ValidationException(
                ErrorMessages.kkmDeleteRequiresProgramming(),
                "KKM_DELETE_REQUIRES_PROGRAMMING"
            )
        }
        // Проверка: смена должна быть закрыта
        val openShift = storage.findOpenShift(kkm.id)
        if (openShift != null) {
            throw ConflictException(ErrorMessages.kkmDeleteShiftOpen(), "KKM_DELETE_SHIFT_OPEN")
        }
        // Проверка: очередь не должна содержать неотправленных в ОФД документов
        if (!queue.canSendDirectly(kkm.id)) {
            throw ConflictException(
                ErrorMessages.kkmDeleteQueueNotEmpty(),
                "KKM_DELETE_QUEUE_NOT_EMPTY"
            )
        }
        // Выполняем полное удаление из хранилища данных
        val deleted = storage.deleteKkmCompletely(kkm.id)
        if (!deleted) {
            throw NotFoundException(trilingualMessage = ErrorMessages.kkmNotFound(), code = "KKM_NOT_FOUND")
        }
        // Очищаем очередь команд для удаленной кассы
        queue.deleteQueuedCommands(kkm.id)
        return true
    }
}
