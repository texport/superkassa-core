package kz.mybrain.superkassa.offline_queue.application.model

/**
 * Handler-facing result of a queue command dispatch attempt.
 */
enum class DispatchStatus {
    /**
     * Command was dispatched successfully and should be marked as sent.
     */
    SENT,

    /**
     * Command dispatch failed and should be retried or left terminal according to queue policy.
     */
    FAILED
}
