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
     * Возвращает команды для кассы и линии очереди, отфильтрованные по набору статусов.
     * Реализация (БД) не должна применять никакой бизнес-логики (например, проверку времени retryAt),
     * а должна вернуть все элементы, чей статус совпадает с одним из переданных.
     *
     * @param cashboxId уникальный идентификатор кассы.
     * @param lane линия очереди для поиска команд.
     * @param statuses набор статусов для фильтрации.
     * @return список найденных команд.
     */
    fun getCommandsByStatus(cashboxId: String, lane: QueueLane, statuses: Set<QueueStatus>): List<QueueCommand>

    /**
     * Обновляет статус команды, количество попыток, возможную ошибку и время следующего запуска.
     *
     * @param id уникальный идентификатор обновляемой команды.
     * @param status новый статус команды в очереди.
     * @param attempt общее количество выполненных попыток.
     * @param lastError текст последней ошибки (или null).
     * @param nextAttemptAt время следующего запуска в миллисекундах (или null для успешных/окончательно проваленных).
     * @param lastErrorCode код отказа получателя при последней попытке (или null).
     * @return true, если статус успешно обновлен, иначе false.
     */
    fun updateStatus(
        id: String,
        status: QueueStatus,
        attempt: Int,
        lastError: String?,
        nextAttemptAt: Long?,
        lastErrorCode: Int? = null
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
}
