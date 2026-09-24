package io.github.texport.superkassa.core.domain.api.model.queue

/**
 * Статус отправки задачи из очереди.
 */
enum class QueueDispatchStatus {
    SENT,
    FAILED,

    /**
     * Отвергнуто окончательно: повтор ничего не изменит.
     *
     * Спецификация делит ответы ОФД по коду: 0 — принято, 254 и 255 —
     * повторить, всё прочее — документ негоден. Сюда же относится случай,
     * когда запрос не удалось собрать и он до ОФД не дошёл вовсе.
     */
    REJECTED
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
 * @property bfdResultCode Код отказа БФД; хранится в задаче рядом с текстом.
 */
data class QueueDispatchResult(
    val status: QueueDispatchStatus,
    val errorMessage: String? = null,
    val retryAt: Long? = null,
    val errorRu: String? = null,
    val errorKk: String? = null,
    val errorEn: String? = null,
    val bfdResultCode: Int? = null
)
