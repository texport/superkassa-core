package kz.mybrain.superkassa.offline_queue.application.service

import kz.mybrain.superkassa.offline_queue.application.model.DispatchResult
import kz.mybrain.superkassa.offline_queue.domain.model.QueueCommand

/**
 * Dispatches one queue command through an application-provided transport or use case.
 */
fun interface QueueCommandHandler {
    /**
     * Handles one command. Throwing is treated as `DispatchStatus.FAILED`.
     * The `renewLock` lambda allows the handler to renew its lease during long-running tasks.
     * Returns true if renewal succeeded, false if lock was lost or expired.
     */
    fun handle(command: QueueCommand, renewLock: () -> Boolean): DispatchResult
}
