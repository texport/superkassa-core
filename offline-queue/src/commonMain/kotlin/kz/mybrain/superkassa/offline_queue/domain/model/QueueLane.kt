package kz.mybrain.superkassa.offline_queue.domain.model

/**
 * Линия обработки очереди команд.
 */
enum class QueueLane {
    /**
     * Автономная линия обработки команд (для работы в офлайн-режиме).
     */
    OFFLINE
}
