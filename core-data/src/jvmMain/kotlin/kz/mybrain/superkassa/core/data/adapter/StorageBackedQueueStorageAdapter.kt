package kz.mybrain.superkassa.core.data.adapter

import kz.mybrain.superkassa.core.domain.model.queue.QueueTask
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.offline_queue.domain.model.QueueCommand
import kz.mybrain.superkassa.offline_queue.domain.model.QueueCommandType
import kz.mybrain.superkassa.offline_queue.domain.model.QueueLane
import kz.mybrain.superkassa.offline_queue.domain.model.QueueStatus
import kz.mybrain.superkassa.offline_queue.domain.port.QueueStoragePort

/**
 * Реализация QueueStoragePort через StoragePort.
 * Используется библиотекой оффлайн-очереди для работы с задачами в базе данных.
 * Настраивается и создается как Spring-бин в superkassa-server.
 */
class StorageBackedQueueStorageAdapter(
    private val storage: StoragePort
) : QueueStoragePort {
    /**
     * Сохраняет новую или измененную команду очереди в базу данных.
     */
    override fun enqueue(command: QueueCommand): Boolean {
        return storage.enqueueQueueTask(
            QueueTask(
                id = command.id,
                cashboxId = command.cashboxId,
                lane = command.lane.name,
                type = command.type.name,
                payloadRef = command.payloadRef,
                createdAt = command.createdAt,
                status = command.status.name,
                attempt = command.attempt,
                nextAttemptAt = command.nextAttemptAt,
                lastError = command.lastError
            )
        )
    }

    /**
     * Выбирает следующую готовую к обработке (PENDING) оффлайн-задачу для указанной кассы.
     */
    override fun nextPending(cashboxId: String, lane: QueueLane, now: Long): QueueCommand? {
        return storage.nextPendingQueueTask(cashboxId, lane.name, now)?.let { toCommand(it) }
    }

    /**
     * Обновляет статус выполнения задачи в очереди (увеличивает счетчик попыток, проставляет ошибки).
     */
    override fun updateStatus(
        id: String,
        status: QueueStatus,
        attempt: Int,
        lastError: String?,
        nextAttemptAt: Long?
    ): Boolean {
        return storage.updateQueueTaskStatus(id, status.name, attempt, lastError, nextAttemptAt)
    }

    /**
     * Помечает задачу как выполняемую (IN_PROGRESS) и выставляет метку времени старта.
     */
    override fun markInProgress(id: String, now: Long): Boolean {
        return storage.markQueueTaskInProgress(id, now)
    }

    /**
     * Возвращает список задач в очереди для кассы с поддержкой пагинации.
     */
    override fun listByCashbox(
        cashboxId: String,
        lane: QueueLane,
        limit: Int,
        offset: Int
    ): List<QueueCommand> {
        return storage.listQueueTasksByCashbox(cashboxId, lane.name, limit, offset)
            .map { toCommand(it) }
    }

    /**
     * Удаляет все задачи очереди для указанной кассы.
     */
    override fun deleteByCashbox(cashboxId: String): Boolean {
        return storage.deleteQueueTasksByCashbox(cashboxId)
    }

    /**
     * Маппит доменный DTO QueueTask в структуру QueueCommand оффлайн-очереди.
     */
    private fun toCommand(dto: QueueTask): QueueCommand =
        QueueCommand(
            id = dto.id,
            cashboxId = dto.cashboxId,
            lane = QueueLane.valueOf(dto.lane),
            type = QueueCommandType.valueOf(dto.type),
            payloadRef = dto.payloadRef,
            createdAt = dto.createdAt,
            status = QueueStatus.valueOf(dto.status),
            attempt = dto.attempt,
            nextAttemptAt = dto.nextAttemptAt,
            lastError = dto.lastError
        )
}
