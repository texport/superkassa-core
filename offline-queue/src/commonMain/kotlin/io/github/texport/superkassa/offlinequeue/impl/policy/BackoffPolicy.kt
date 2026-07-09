package io.github.texport.superkassa.offlinequeue.impl.policy

/**
 * Интерфейс политики повторных попыток для обработки неуспешных команд очереди.
 * Используется для расчета времени задержки перед следующим запуском.
 */
internal fun interface BackoffPolicy {
    /**
     * Возвращает время (в миллисекундах epoch), когда команда может быть повторно обработана.
     *
     * @param now текущее системное время (в миллисекундах).
     * @param attempt порядковый номер следующей попытки (начиная с 1).
     * @return время следующего запуска в миллисекундах.
     */
    fun nextAttemptAt(now: Long, attempt: Int): Long
}
