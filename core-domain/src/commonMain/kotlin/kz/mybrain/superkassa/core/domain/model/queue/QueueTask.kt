package kz.mybrain.superkassa.core.domain.model.queue

/**
 * Задача в очереди отложенной отправки команд ОФД.
 * Core-модель, не зависит от внешнего модуля offline_queue.
 *
 * @property id Уникальный идентификатор задачи.
 * @property cashboxId Идентификатор кассы.
 * @property lane Очередь (канал/сегмент) обработки.
 * @property type Тип выполняемой команды.
 * @property payloadRef Ссылка на связанные данные (полезную нагрузку) во внутреннем хранилище.
 * @property createdAt Время создания задачи (в миллисекундах).
 * @property status Текущий статус выполнения задачи.
 * @property attempt Номер текущей попытки выполнения.
 * @property nextAttemptAt Время следующей запланированной попытки (в миллисекундах, null если не запланирована).
 * @property lastError Сообщение о последней возникшей ошибке (null если ошибок не было).
 */
data class QueueTask(
    val id: String,
    val cashboxId: String,
    val lane: String,
    val type: String,
    val payloadRef: String,
    val createdAt: Long,
    val status: String,
    val attempt: Int,
    val nextAttemptAt: Long?,
    val lastError: String?
)
