package kz.mybrain.superkassa.core.domain.usecase.queue

import kz.mybrain.superkassa.core.domain.exception.ConflictException
import kz.mybrain.superkassa.core.domain.exception.ErrorMessages
import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.model.kkm.KkmMode
import kz.mybrain.superkassa.core.domain.model.kkm.KkmState
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.usecase.auth.AuthorizeUserUseCase
import kz.mybrain.superkassa.offline_queue.domain.model.QueueLane
import kz.mybrain.superkassa.offline_queue.domain.model.QueueStatus
import kz.mybrain.superkassa.offline_queue.domain.port.QueueStoragePort

/**
 * Сценарий (Use Case) повторной отправки неудавшихся задач из локальной очереди ОФД.
 *
 * Переводит все задачи в очереди со статусом [QueueStatus.FAILED] обратно в статус [QueueStatus.PENDING],
 * чтобы инициировать повторный цикл их обработки и отправки.
 * Для выполнения этой операции ККМ должна находиться в режиме программирования ([KkmMode.PROGRAMMING])
 * и смена кассы должна быть закрыта.
 *
 * @property storage Порт для доступа к хранилищу данных ККМ и смен.
 * @property queueStorage Порт для работы с хранилищем очереди задач отправки.
 * @property authorizeUserUseCase Сценарий авторизации пользователей и проверки ролей/ККМ.
 */
class RetryFailedQueueItemsUseCase(
    private val storage: StoragePort,
    private val queueStorage: QueueStoragePort,
    private val authorizeUserUseCase: AuthorizeUserUseCase
) {
    /**
     * Выполняет сценарий повтора отправки для всех зависших/ошибочных задач в офлайн-очереди ККМ.
     *
     * Требует авторизации с ролью [kz.mybrain.superkassa.core.domain.model.auth.UserRole.ADMIN].
     *
     * @param kkmId Идентификатор кассового аппарата (ККМ).
     * @param pin ПИН-код администратора для авторизации.
     * @return Количество успешно обновленных задач в очереди.
     * @throws ValidationException Если ККМ находится не в режиме программирования.
     * @throws ConflictException Если на ККМ открыта смена.
     * @throws kz.mybrain.superkassa.core.domain.exception.NotFoundException Если ККМ не найдена.
     */
    fun execute(kkmId: String, pin: String): Int {
        val kkm = authorizeUserUseCase.requireKkm(kkmId)
        authorizeUserUseCase.requireRole(kkmId, pin, setOf(kz.mybrain.superkassa.core.domain.model.auth.UserRole.ADMIN))

        if (kkm.mode != KkmMode.PROGRAMMING.name || kkm.state != KkmState.PROGRAMMING.name) {
            throw ValidationException(
                ErrorMessages.kkmSettingsRequiresProgramming(),
                "KKM_NOT_IN_PROGRAMMING_FOR_QUEUE"
            )
        }

        val openShift = storage.findOpenShift(kkmId)
        if (openShift != null) {
            throw ConflictException(
                ErrorMessages.kkmDeleteShiftOpen(),
                "QUEUE_MANAGEMENT_SHIFT_OPEN"
            )
        }

        return storage.inTransaction {
            val offline = queueStorage.listByCashbox(kkmId, QueueLane.OFFLINE, limit = 100, offset = 0)
            val failed = offline.filter { it.status == QueueStatus.FAILED }
            if (failed.isEmpty()) return@inTransaction 0
            var updated = 0
            failed.forEach { cmd ->
                val ok = queueStorage.updateStatus(
                    id = cmd.id,
                    status = QueueStatus.PENDING,
                    attempt = cmd.attempt,
                    lastError = null,
                    nextAttemptAt = null
                )
                if (ok) updated++
            }
            updated
        }
    }
}
