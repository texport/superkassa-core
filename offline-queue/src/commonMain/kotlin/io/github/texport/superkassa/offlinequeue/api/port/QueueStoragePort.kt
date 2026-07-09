package io.github.texport.superkassa.offlinequeue.api.port

import io.github.texport.superkassa.offlinequeue.api.model.QueueCommand
import io.github.texport.superkassa.offlinequeue.api.model.QueueLane
import io.github.texport.superkassa.offlinequeue.api.model.QueueStatus

/**
 * Интерфейс хранилища для надежного хранения команд очереди.
 * Предоставляет методы для сохранения, получения, изменения статуса и очистки команд.
 */
interface QueueStoragePort {
    /**
     * Сохраняет новую команду в очереди. Реализации должны обеспечивать
     * идемпотентность по идентификатору команды `command.id`.
     *
     * @param command объект сохраняемой команды.
     * @return true, если команда успешно добавлена (или уже существовала), иначе false.
     */
    fun enqueue(command: QueueCommand): Boolean

    /**
     * Возвращает одну команду, готовую для обработки на момент времени `now`.
     *
     * @param cashboxId уникальный идентификатор кассы.
     * @param lane линия очереди для поиска команды.
     * @param now текущее системное время (в миллисекундах) для проверки времени следующей попытки.
     * @return объект команды, готовой к выполнению, или null, если команд нет.
     */
    fun nextPending(cashboxId: String, lane: QueueLane, now: Long): QueueCommand?

    /**
     * Обновляет статус команды, количество попыток, возможную ошибку и время следующего запуска.
     *
     * @param id уникальный идентификатор обновляемой команды.
     * @param status новый статус команды в очереди.
     * @param attempt общее количество выполненных попыток.
     * @param lastError текст последней ошибки (или null).
     * @param nextAttemptAt время следующего запуска в миллисекундах (или null для успешных/окончательно проваленных).
     * @return true, если статус успешно обновлен, иначе false.
     */
    fun updateStatus(
        id: String,
        status: QueueStatus,
        attempt: Int,
        lastError: String?,
        nextAttemptAt: Long?
    ): Boolean

    /**
     * Помечает команду как находящуюся в процессе обработки (IN_PROGRESS) непосредственно перед ее выполнением.
     *
     * @param id уникальный идентификатор команды.
     * @param now текущее системное время начала обработки (в миллисекундах).
     * @return true, если статус команды успешно обновлен, иначе false.
     */
    fun markInProgress(id: String, now: Long): Boolean

    /**
     * Возвращает список команд для кассы и линии очереди с поддержкой пагинации.
     *
     * @param cashboxId уникальный идентификатор кассы.
     * @param lane линия очереди.
     * @param limit максимальное количество возвращаемых команд.
     * @param offset смещение для пагинации (по умолчанию 0).
     * @return список найденных команд.
     */
    fun listByCashbox(cashboxId: String, lane: QueueLane, limit: Int, offset: Int = 0): List<QueueCommand>

    /**
     * Удаляет все команды очереди для указанной кассы.
     *
     * @param cashboxId уникальный идентификатор кассы.
     * @return true, если команды успешно удалены, иначе false.
     */
    fun deleteByCashbox(cashboxId: String): Boolean

    /**
     * Проверяет, существуют ли невыполненные команды в очереди для указанной кассы и линии.
     *
     * @param cashboxId уникальный идентификатор кассы.
     * @param lane линия очереди.
     * @return true, если невыполненные (или ожидающие повтора) команды существуют, иначе false.
     */
    fun hasPendingCommands(cashboxId: String, lane: QueueLane): Boolean
}
