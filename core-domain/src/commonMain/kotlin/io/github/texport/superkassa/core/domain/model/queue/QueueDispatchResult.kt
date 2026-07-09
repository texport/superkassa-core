package io.github.texport.superkassa.core.domain.model.queue

/**
 * Статус отправки задачи из очереди.
 */
enum class QueueDispatchStatus {
    SENT,
    FAILED
}

/**
 * Результат выполнения сценария отправки задачи из очереди ОФД.
 *
 * @property status Статус отправки (SENT или FAILED).
 * @property errorMessage Сообщение об ошибке (если отправка не удалась).
 * @property retryAt Время следующей попытки (timestamp в миллисекундах).
 * @property errorRu Текст ошибки на русском языке.
 * @property errorKk Текст ошибки на казахском языке.
 * @property errorEn Текст ошибки на английском языке.
 */
data class QueueDispatchResult(
    val status: QueueDispatchStatus,
    val errorMessage: String? = null,
    val retryAt: Long? = null,
    val errorRu: String? = null,
    val errorKk: String? = null,
    val errorEn: String? = null
)
