package kz.mybrain.superkassa.core.data.adapter

import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.offline_queue.domain.port.LeaseLockPort

/**
 * Реализация LeaseLockPort через StoragePort.
 * Используется библиотекой оффлайн-очереди для координации блокировок.
 * Настраивается и создается как Spring-бин в superkassa-server.
 */
class StorageBackedLeaseLockAdapter(
    private val storage: StoragePort
) : LeaseLockPort {
    /**
     * Пытается захватить распределенную блокировку на обработку очереди чеков ККМ.
     * Предотвращает одновременную выгрузку чеков из нескольких конкурентных процессов или серверов.
     * @param cashboxId ID кассы.
     * @param ownerId Уникальный ID инстанса/процесса кассы, запрашивающего блокировку.
     * @param leaseUntil Время UTC в миллисекундах, до которого удерживается блокировка.
     * @param now Текущее системное время UTC в миллисекундах.
     * @return true, если блокировка успешно захвачена.
     */
    override fun tryAcquire(
        cashboxId: String,
        ownerId: String,
        leaseUntil: Long,
        now: Long
    ): Boolean {
        return storage.tryAcquireQueueLock(cashboxId, ownerId, leaseUntil, now)
    }

    /**
     * Продлевает текущую активную блокировку на обработку очереди.
     */
    override fun renew(cashboxId: String, ownerId: String, leaseUntil: Long, now: Long): Boolean {
        return storage.renewQueueLock(cashboxId, ownerId, leaseUntil, now)
    }

    /**
     * Освобождает блокировку, позволяя другим инстансам начать выгрузку.
     */
    override fun release(cashboxId: String, ownerId: String): Boolean {
        return storage.releaseQueueLock(cashboxId, ownerId)
    }
}
