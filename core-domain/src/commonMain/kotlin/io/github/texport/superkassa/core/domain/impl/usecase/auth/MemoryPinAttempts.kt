package io.github.texport.superkassa.core.domain.impl.usecase.auth

import io.github.texport.superkassa.core.domain.api.model.auth.PinAttempts
import io.github.texport.superkassa.core.domain.api.port.integration.PinAttemptsPort
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Счёт неверных пинов в памяти процесса — для хранилища, которое своего не ведёт.
 *
 * Перезапуск его обнуляет; хранилище кассы на Room держит счёт в базе.
 */
class MemoryPinAttempts : PinAttemptsPort {
    private val lock = Mutex()
    private val byKkm = mutableMapOf<String, PinAttempts>()

    override fun getAndUpdate(kkmId: String, change: (PinAttempts) -> PinAttempts): PinAttempts = runBlocking {
        lock.withLock {
            val previous = byKkm[kkmId] ?: PinAttempts()
            byKkm[kkmId] = change(previous)
            previous
        }
    }

    override fun clear(kkmId: String) = runBlocking {
        lock.withLock { byKkm.remove(kkmId) }
        Unit
    }
}
