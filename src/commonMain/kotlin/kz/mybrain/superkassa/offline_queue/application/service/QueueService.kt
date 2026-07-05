package kz.mybrain.superkassa.offline_queue.application.service

import kz.mybrain.superkassa.offline_queue.application.logging.getLogger
import kz.mybrain.superkassa.offline_queue.application.model.DispatchResult
import kz.mybrain.superkassa.offline_queue.application.model.DispatchStatus
import kz.mybrain.superkassa.offline_queue.application.model.QueueDispatchException
import kz.mybrain.superkassa.offline_queue.application.model.QueueErrorMessages
import kz.mybrain.superkassa.offline_queue.application.policy.BackoffPolicy
import kz.mybrain.superkassa.offline_queue.application.policy.SystemTimeProvider
import kz.mybrain.superkassa.offline_queue.application.policy.TimeProvider
import kz.mybrain.superkassa.offline_queue.domain.model.QueueCommand
import kz.mybrain.superkassa.offline_queue.domain.model.QueueLane
import kz.mybrain.superkassa.offline_queue.domain.model.QueueStatus
import kz.mybrain.superkassa.offline_queue.domain.port.LeaseLockPort
import kz.mybrain.superkassa.offline_queue.domain.port.QueueStoragePort

/**
 * Coordinates offline queue command dispatch under a cashbox lease lock.
 */
class QueueService(
    private val storage: QueueStoragePort,
    private val lockPort: LeaseLockPort,
    private val handler: QueueCommandHandler,
    private val backoffPolicy: BackoffPolicy,
    private val ownerId: String,
    private val leaseMs: Long = 15000,
    private val maxAttempts: Int = Int.MAX_VALUE,
    private val timeProvider: TimeProvider = SystemTimeProvider
) {
    init {
        require(leaseMs > 0) { "leaseMs must be positive" }
        require(maxAttempts > 0) { "maxAttempts must be positive" }
    }

    private val logger = getLogger(QueueService::class)

    /**
     * Enqueues a command to the durable storage queue.
     *
     * @param command the command to enqueue.
     * @return true if enqueued successfully, false otherwise.
     */
    fun enqueue(command: QueueCommand): Boolean {
        logger.info(
            "Queue enqueue. cashboxId={}, lane={}, type={}, id={}",
            command.cashboxId,
            command.lane,
            command.type,
            command.id
        )
        return storage.enqueue(command)
    }

    /**
     * Checks if there are any pending commands in the queue for the specified cashbox.
     *
     * @param cashboxId the target cashbox identifier.
     * @return true if pending offline commands exist, false otherwise.
     */
    fun hasOfflineQueue(cashboxId: String): Boolean {
        return storage.hasPendingCommands(cashboxId, QueueLane.OFFLINE)
    }

    /**
     * Clears all commands from the queue for the specified cashbox.
     *
     * @param cashboxId the target cashbox identifier.
     * @return true if the queue was cleared successfully, false otherwise.
     */
    fun clearQueue(cashboxId: String): Boolean {
        logger.info("Queue clear. cashboxId={}", cashboxId)
        return storage.deleteByCashbox(cashboxId)
    }

    /**
     * Processes the next pending command for the cashbox under a lease lock.
     *
     * @param cashboxId the target cashbox identifier.
     * @param lane the queue lane to process.
     * @return true if a command was successfully processed and updated, false otherwise.
     */
    fun processNext(cashboxId: String, lane: QueueLane): Boolean {
        val now = timeProvider.now()
        val leaseUntil = now + leaseMs
        if (!lockPort.tryAcquire(cashboxId, ownerId, leaseUntil, now)) {
            logger.info("Queue lock busy. cashboxId={}, ownerId={}", cashboxId, ownerId)
            return false
        }
        var success = false
        try {
            val next = storage.nextPending(cashboxId, lane, now)
            if (next != null) {
                success = processCommand(next, now)
            }
        } finally {
            if (!lockPort.release(cashboxId, ownerId)) {
                logger.warn("Queue lock release failed. cashboxId={}, ownerId={}", cashboxId, ownerId)
            }
        }
        return success
    }

    private fun processCommand(next: QueueCommand, now: Long): Boolean {
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
            logger.error("Invalid queue command handler result. id=${next.id}", e)
            DispatchResult(DispatchStatus.FAILED, errorMessage = e.error.compact(), error = e.error)
        } catch (e: Exception) {
            logger.error("Unhandled exception in queue command handler. id=${next.id}", e)
            val reason = e.message ?: (e::class.simpleName ?: "Exception")
            val error = QueueErrorMessages.handlerException(reason)
            DispatchResult(DispatchStatus.FAILED, errorMessage = error.compact(), error = error)
        }
        return applyResult(next, result, now)
    }

    /**
     * Processes a batch of pending commands for the cashbox, up to the specified limit.
     *
     * @param cashboxId the target cashbox identifier.
     * @param lane the queue lane to process.
     * @param limit the maximum number of commands to process.
     * @return the number of successfully processed commands.
     */
    fun processBatch(cashboxId: String, lane: QueueLane, limit: Int): Int {
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
