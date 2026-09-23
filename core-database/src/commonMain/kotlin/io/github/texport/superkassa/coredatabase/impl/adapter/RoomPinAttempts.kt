package io.github.texport.superkassa.coredatabase.impl.adapter

import io.github.texport.superkassa.core.domain.api.model.auth.PinAttempts
import io.github.texport.superkassa.core.domain.api.port.integration.PinAttemptsPort
import io.github.texport.superkassa.coredatabase.impl.dao.PinAttemptDao
import io.github.texport.superkassa.coredatabase.impl.entity.PinAttemptEntity
import kotlinx.coroutines.runBlocking

/**
 * Счёт неверных пинов в базе кассы.
 *
 * Чтение и запись счёта идут под одним замком: две попытки подряд
 * не прочитают один и тот же счёт, и ни одна не потеряется.
 */
internal class RoomPinAttempts(private val dao: PinAttemptDao) : PinAttemptsPort {
    private val gate = ReentrantGate()

    override fun getAndUpdate(kkmId: String, change: (PinAttempts) -> PinAttempts): PinAttempts {
        gate.lock()
        try {
            return runBlocking {
                val previous = dao.find(kkmId)?.let { PinAttempts(it.failures, it.lockedUntil) } ?: PinAttempts()
                val next = change(previous)
                dao.save(PinAttemptEntity(kkmId, next.failures, next.lockedUntil))
                previous
            }
        } finally {
            gate.unlock()
        }
    }

    override fun clear(kkmId: String) = runBlocking { dao.delete(kkmId) }
}
