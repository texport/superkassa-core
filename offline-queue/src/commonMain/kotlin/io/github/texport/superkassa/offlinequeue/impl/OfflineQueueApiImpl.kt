package io.github.texport.superkassa.offlinequeue.impl

import io.github.texport.superkassa.offlinequeue.api.OfflineQueueApi
import io.github.texport.superkassa.offlinequeue.api.model.DispatchResult
import io.github.texport.superkassa.offlinequeue.api.model.DispatchStatus
import io.github.texport.superkassa.offlinequeue.api.model.QueueCommand
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.offlinequeue.api.model.QueueLane
import io.github.texport.superkassa.offlinequeue.api.model.QueueStatus
import io.github.texport.superkassa.offlinequeue.api.port.LeaseLockPort
import io.github.texport.superkassa.offlinequeue.api.port.QueueCommandHandlerPort
import io.github.texport.superkassa.offlinequeue.api.port.QueueStoragePort
import io.github.texport.superkassa.offlinequeue.impl.policy.BackoffPolicy
import io.github.texport.superkassa.offlinequeue.impl.policy.SystemTimeProvider
import io.github.texport.superkassa.offlinequeue.impl.policy.TimeProvider

/**
 * Координирует отправку команд из оффлайн-очереди под распределенной блокировкой аренды кассы.
 */
internal class OfflineQueueApiImpl(
    private val storage: QueueStoragePort,
    private val lockPort: LeaseLockPort,
    private val handler: QueueCommandHandlerPort,
    private val backoffPolicy: BackoffPolicy,
    private val ownerId: String,
    private val leaseMs: Long = 15000,
    private val maxAttempts: Int = Int.MAX_VALUE,
    private val timeProvider: TimeProvider = SystemTimeProvider
) : OfflineQueueApi {
    init {
        require(leaseMs > 0) { "leaseMs must be positive" }
        require(maxAttempts > 0) { "maxAttempts must be positive" }
    }

    private val logger = getLogger(OfflineQueueApiImpl::class)

    /**
     * Помещает команду в надежное хранилище очереди.
     *
     * @param command помещаемая команда.
     * @return true, если успешно помещено в очередь, иначе false.
     */
    override fun enqueue(command: QueueCommand): Boolean {
        logger.debug(
            "Queue enqueue. cashboxId={}, lane={}, type={}, id={}",
            command.cashboxId,
            command.lane,
            command.type,
            command.id
        )
        return storage.enqueue(command)
    }

    /**
     * Проверяет наличие невыполненных команд в очереди для указанной кассы.
     *
     * @param cashboxId идентификатор кассы.
     * @return true, если есть ожидающие оффлайн-команды, иначе false.
     */
    override fun hasOfflineQueue(cashboxId: String): Boolean {
        return storage.hasPendingCommands(cashboxId, QueueLane.OFFLINE)
    }

    /**
     * Очищает все команды очереди для указанной кассы.
     *
     * @param cashboxId идентификатор кассы.
     * @return true, если очередь успешно очищена, иначе false.
     */
    override fun clearQueue(cashboxId: String): Boolean {
        logger.info("Queue clear. cashboxId={}", cashboxId)
        return storage.deleteByCashbox(cashboxId)
    }

    /**
     * Обрабатывает следующую ожидающую команду для кассы под блокировкой аренды.
     *
     * @param cashboxId идентификатор кассы.
     * @param lane канал/линия очереди для обработки.
     * @return true, если команда была успешно обработана и обновлена, иначе false.
     */
    override fun processNext(cashboxId: String, lane: QueueLane): Boolean {
        logger.trace("Entering processNext. cashboxId={}, lane={}", cashboxId, lane)
        val now = timeProvider.now()
        val leaseUntil = now + leaseMs
        if (!lockPort.tryAcquire(cashboxId, ownerId, leaseUntil, now)) {
            logger.debug("Queue lock busy. cashboxId={}, ownerId={}", cashboxId, ownerId)
            return false
        }
        var success = false
        try {
            val next = storage.nextPending(cashboxId, lane, now)
            if (next != null) {
                logger.trace("Processing next pending command. id={}", next.id)
                success = processCommand(next, now)
            } else {
                logger.trace("No pending commands for cashboxId={}, lane={}", cashboxId, lane)
            }
        } finally {
            if (!lockPort.release(cashboxId, ownerId)) {
                logger.warn("Queue lock release failed. cashboxId={}, ownerId={}", cashboxId, ownerId)
            }
        }
        return success
    }

    private fun processCommand(next: QueueCommand, now: Long): Boolean {
        logger.trace("Marking command in progress. id={}", next.id)
        if (!storage.markInProgress(next.id, now)) {
            logger.warn("Queue command could not be marked in progress. id={}", next.id)
            return false
        }

        val renewLock = {
            val renewNow = timeProvider.now()
            val extendedLeaseUntil = renewNow + leaseMs
            lockPort.renew(next.cashboxId, ownerId, extendedLeaseUntil, renewNow)
        }

        val result = try {
            handler.handle(next, renewLock)
        } catch (e: QueueDispatchException) {
            logger.warn("Invalid queue command handler result. id=${next.id}: {}", e.message)
            DispatchResult(DispatchStatus.FAILED, errorMessage = e.error.compact(), error = e.error)
        } catch (e: Exception) {
            logger.error("Unhandled exception in queue command handler. id=${next.id}", e)
            val reason = e.message ?: (e::class.simpleName ?: "Exception")
            val error = CoreStrings.handlerException(reason)
            DispatchResult(DispatchStatus.FAILED, errorMessage = error.compact(), error = error)
        }
        return applyResult(next, result, now)
    }

    /**
     * Обрабатывает пачку ожидающих команд для кассы до достижения указанного лимита.
     *
     * @param cashboxId идентификатор кассы.
     * @param lane канал/линия очереди.
     * @param limit максимальное количество обрабатываемых команд.
     * @return количество успешно обработанных команд.
     */
    override fun processBatch(cashboxId: String, lane: QueueLane, limit: Int): Int {
        require(limit >= 0) { "limit must be non-negative" }
        var processed = 0
        while (processed < limit) {
            val ok = processNext(cashboxId, lane)
            if (!ok) break
            processed++
        }
        return processed
    }

    private fun applyResult(command: QueueCommand, result: DispatchResult, now: Long): Boolean {
        val attempt = command.attempt + 1
        val dispatchStatus = result.dispatchStatus
        val updated = when (dispatchStatus) {
            DispatchStatus.SENT -> {
                storage.updateStatus(command.id, QueueStatus.SENT, attempt, null, null)
                    .also { if (it) logger.info("Queue command sent. id={}", command.id) }
            }
            DispatchStatus.FAILED -> {
                val retryAt = result.retryAt ?: nextRetryAt(now, attempt)
                storage.updateStatus(
                    command.id,
                    QueueStatus.FAILED,
                    attempt,
                    result.error?.compact(),
                    retryAt
                )
                    .also { if (it) logger.warn("Queue command failed. id={}, retryAt={}", command.id, retryAt) }
            }
        }
        if (!updated) {
            logger.warn("Queue command status update failed. id={}, status={}", command.id, dispatchStatus)
        }
        return updated
    }

    private fun nextRetryAt(now: Long, attempt: Int): Long? {
        return if (attempt >= maxAttempts) {
            null
        } else {
            backoffPolicy.nextAttemptAt(now, attempt)
        }
    }
}
