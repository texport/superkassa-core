package kz.mybrain.superkassa.offline_queue.application.policy

/**
 * Реализация политики экспоненциального отката (exponential backoff) с верхним пределом задержки.
 * Вычисляет задержку как `baseDelayMs * 2^attempt`, ограничивая ее максимальным значением `maxDelayMs`.
 *
 * @property baseDelayMs базовая задержка перед первой повторной попыткой (по умолчанию 1000 мс).
 * @property maxDelayMs максимальный предел задержки между попытками (по умолчанию 60000 мс).
 */
class DefaultBackoffPolicy(
    private val baseDelayMs: Long = 1000,
    private val maxDelayMs: Long = 60000
) : BackoffPolicy {
    /**
     * Вычисляет время следующего запуска команды на основе текущего времени и номера попытки.
     *
     * @param now текущее системное время в миллисекундах.
     * @param attempt порядковый номер следующей попытки (начиная с 1).
     * @return время следующего запуска в миллисекундах.
     */
    override fun nextAttemptAt(now: Long, attempt: Int): Long {
        val multiplier = 1L shl attempt.coerceAtMost(10)
        val delay = (baseDelayMs * multiplier).coerceAtMost(maxDelayMs)
        return now + delay
    }
}
