package kz.mybrain.superkassa.core.data.adapter

import kz.mybrain.superkassa.core.domain.model.queue.OfflineQueueCommandRequest
import kz.mybrain.superkassa.core.domain.port.OfflineQueuePort
import kz.mybrain.superkassa.offline_queue.application.policy.DefaultBackoffPolicy
import kz.mybrain.superkassa.offline_queue.application.service.QueueCommandHandler
import kz.mybrain.superkassa.offline_queue.application.service.QueueService
import kz.mybrain.superkassa.offline_queue.domain.model.QueueCommand
import kz.mybrain.superkassa.offline_queue.domain.model.QueueCommandType
import kz.mybrain.superkassa.offline_queue.domain.model.QueueLane
import kz.mybrain.superkassa.offline_queue.domain.model.QueueStatus
import kz.mybrain.superkassa.offline_queue.domain.port.LeaseLockPort
import kz.mybrain.superkassa.offline_queue.domain.port.QueueStoragePort

/**
 * Адаптер оффлайн-очереди на базе библиотеки superkassa-offline-queue.
 * Настраивается и создается как Spring-бин в superkassa-server.
 */
class OfflineQueueAdapter(
    private val storage: QueueStoragePort,
    lockPort: LeaseLockPort,
    handler: QueueCommandHandler,
    ownerId: String
) : OfflineQueuePort {
    private val queueService = QueueService(
        storage = storage,
        lockPort = lockPort,
        handler = handler,
        backoffPolicy = DefaultBackoffPolicy(),
        ownerId = ownerId
    )

    /**
     * Возвращает true, если у ККМ нет накопленной очереди оффлайн-заданий,
     * то есть новые команды можно слать в ОФД напрямую, не ставя в очередь.
     * @param kkmId ID кассы.
     */
    override fun canSendDirectly(kkmId: String): Boolean = !queueService.hasOfflineQueue(kkmId)

    /**
     * Помещает команду в оффлайн-очередь для последующей фоновой отправки.
     * @param command Данные команды (тип, ссылка на документ, ID кассы).
     * @return true, если команда успешно добавлена в очередь.
     */
    override fun enqueueOffline(command: OfflineQueueCommandRequest): Boolean {
        return queueService.enqueue(
            QueueCommand(
                id = commandId(command),
                cashboxId = command.kkmId,
                lane = QueueLane.OFFLINE,
                type = mapType(command.type),
                payloadRef = command.payloadRef,
                createdAt = System.currentTimeMillis(),
                status = QueueStatus.PENDING,
                attempt = 0
            )
        )
    }

    /**
     * Очищает всю оффлайн-очередь для данной кассы (например, при сбросе настроек или перерегистрации).
     * @param kkmId ID кассы.
     * @return true, если удаление завершилось успехом.
     */
    override fun deleteQueuedCommands(kkmId: String): Boolean {
        return storage.deleteByCashbox(kkmId)
    }

    /**
     * Запускает фоновую выгрузку накопленных оффлайн-чеков партиями в ОФД.
     * @param kkmId ID кассы.
     * @param limit Максимальное количество чеков за одну сессию выгрузки.
     * @return Количество успешно отправленных чеков.
     */
    override fun processOfflineBatch(kkmId: String, limit: Int): Int =
        queueService.processBatch(kkmId, QueueLane.OFFLINE, limit)

    /**
     * Генерирует детерминированный составной идентификатор для команды в очереди.
     */
    private fun commandId(command: OfflineQueueCommandRequest): String {
        return "${command.kkmId}:${command.type}:${command.payloadRef}"
    }

    /**
     * Сопоставляет строковые типы команд домена с перечислением QueueCommandType оффлайн-очереди.
     */
    private fun mapType(type: String): QueueCommandType {
        return when (type) {
            "COMMAND_TICKET" -> QueueCommandType.TICKET
            "COMMAND_REPORT" -> QueueCommandType.REPORT_X
            "COMMAND_CLOSE_SHIFT" -> QueueCommandType.CLOSE_SHIFT
            "COMMAND_MONEY_PLACEMENT" -> QueueCommandType.MONEY_PLACEMENT
            "COMMAND_INFO" -> QueueCommandType.INFO
            "COMMAND_SYSTEM" -> QueueCommandType.SYSTEM
            else -> QueueCommandType.TICKET
        }
    }
}
