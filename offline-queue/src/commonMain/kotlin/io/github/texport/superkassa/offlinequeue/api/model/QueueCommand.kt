package io.github.texport.superkassa.offlinequeue.api.model

/**
 * Элемент очереди для отправки команд в ОФД.
 * Содержит полезную нагрузку, метаданные о времени создания, статус и счетчик попыток отправки.
 *
 * @property id уникальный идентификатор команды.
 * @property cashboxId уникальный идентификатор кассы, к которой привязана команда.
 * @property lane линия очереди, в которой обрабатывается команда.
 * @property type тип команды (чек, X/Z отчет и т.д.).
 * @property payloadRef ссылка на полезную нагрузку команды в хранилище.
 * @property createdAt время создания команды (миллисекунды epoch).
 * @property status текущий статус выполнения команды в очереди.
 * @property attempt количество выполненных попыток обработки.
 * @property nextAttemptAt время следующей попытки отправки в миллисекундах (или null).
 * @property lastError текст ошибки последней попытки выполнения (или null).
 */
data class QueueCommand(
    val id: String,
    val cashboxId: String,
    val lane: QueueLane,
    val type: QueueCommandType,
    val payloadRef: String,
    val createdAt: Long,
    val status: QueueStatus,
    val attempt: Int,
    val nextAttemptAt: Long? = null,
    val lastError: String? = null
)
