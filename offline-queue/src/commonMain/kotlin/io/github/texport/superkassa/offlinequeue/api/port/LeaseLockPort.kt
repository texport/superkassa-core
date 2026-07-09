package io.github.texport.superkassa.offlinequeue.api.port

/**
 * Блокировка аренды, привязанная к владельцу, для обработки очереди одной кассы.
 * Обеспечивает потокобезопасность и исключает параллельную обработку одной кассы разными процессами.
 */
interface LeaseLockPort {
    /**
     * Пытается захватить блокировку кассы до указанного времени `leaseUntil`.
     *
     * @param cashboxId уникальный идентификатор кассы.
     * @param ownerId идентификатор узла или процесса, запрашивающего блокировку.
     * @param leaseUntil время окончания аренды блокировки (в миллисекундах).
     * @param now текущее системное время (в миллисекундах).
     * @return true, если блокировка успешно захвачена, иначе false.
     */
    fun tryAcquire(cashboxId: String, ownerId: String, leaseUntil: Long, now: Long): Boolean

    /**
     * Продлевает текущую аренду блокировки для указанного владельца.
     *
     * @param cashboxId уникальный идентификатор кассы.
     * @param ownerId идентификатор владельца блокировки.
     * @param leaseUntil новое время окончания аренды (в миллисекундах).
     * @param now текущее системное время (в миллисекундах).
     * @return true, если аренда успешно продлена, иначе false.
     */
    fun renew(cashboxId: String, ownerId: String, leaseUntil: Long, now: Long): Boolean

    /**
     * Освобождает блокировку кассы для указанного владельца.
     *
     * @param cashboxId уникальный идентификатор кассы.
     * @param ownerId идентификатор владельца блокировки.
     * @return true, если блокировка успешно освобождена, иначе false.
     */
    fun release(cashboxId: String, ownerId: String): Boolean
}
