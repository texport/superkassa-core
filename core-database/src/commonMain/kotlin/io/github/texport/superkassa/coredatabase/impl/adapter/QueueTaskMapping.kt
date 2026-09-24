package io.github.texport.superkassa.coredatabase.impl.adapter

import io.github.texport.superkassa.core.domain.api.model.queue.QueueTask
import io.github.texport.superkassa.offlinequeue.api.model.QueueCommand
import io.github.texport.superkassa.offlinequeue.api.model.QueueCommandType
import io.github.texport.superkassa.offlinequeue.api.model.QueueLane
import io.github.texport.superkassa.offlinequeue.api.model.QueueStatus

/**
 * Команда очереди в том виде, в каком её видит порт хранилища ядра.
 *
 * Очередь ядра ходит в хранилище через порт кассы, а не через порт
 * очереди, поэтому одна и та же таблица должна отвечать обоим.
 */
internal fun QueueCommand.toTask(): QueueTask = QueueTask(
    id = id,
    cashboxId = cashboxId,
    lane = lane.name,
    type = type.name,
    payloadRef = payloadRef,
    createdAt = createdAt,
    status = status.name,
    attempt = attempt,
    nextAttemptAt = nextAttemptAt,
    lastError = lastError,
    lastErrorCode = lastErrorCode
)

/** Обратное преобразование: задача порта хранилища — команда очереди. */
internal fun QueueTask.toCommand(): QueueCommand = QueueCommand(
    id = id,
    cashboxId = cashboxId,
    lane = QueueLane.valueOf(lane),
    type = QueueCommandType.valueOf(type),
    payloadRef = payloadRef,
    createdAt = createdAt,
    status = QueueStatus.valueOf(status),
    attempt = attempt,
    nextAttemptAt = nextAttemptAt,
    lastError = lastError,
    lastErrorCode = lastErrorCode
)

/**
 * Ошибка последней попытки: текст причины и код отказа получателя.
 *
 * @property text трёхъязычный текст причины строкой.
 * @property code код отказа БФД, если он был.
 */
internal data class QueueError(val text: String?, val code: Int?)

/** Статусы задачи строками порта — статусы очереди. */
internal fun Set<String>.toQueueStatuses(): Set<QueueStatus> = mapTo(mutableSetOf()) { QueueStatus.valueOf(it) }
