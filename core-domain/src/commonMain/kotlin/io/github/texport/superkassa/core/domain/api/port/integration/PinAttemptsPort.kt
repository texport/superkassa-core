package io.github.texport.superkassa.core.domain.api.port.integration

import io.github.texport.superkassa.core.domain.api.model.auth.PinAttempts

/**
 * Счёт неверных пинов по кассе.
 *
 * Хранилище, которое реализует этот порт вместе со [StoragePort], держит
 * счёт в своей базе, и блокировка переживает перезапуск. Хранилище без
 * него получает счёт в памяти процесса.
 */
interface PinAttemptsPort {
    /**
     * Меняет счёт кассы одним шагом, без вклинивания параллельной попытки.
     *
     * @param kkmId касса.
     * @param change новый счёт из прежнего; для кассы без счёта прежний — пустой [PinAttempts].
     * @return прежний счёт.
     */
    fun getAndUpdate(kkmId: String, change: (PinAttempts) -> PinAttempts): PinAttempts

    /** Забывает счёт кассы: пин подтвердился или касса удалена. */
    fun clear(kkmId: String)
}
