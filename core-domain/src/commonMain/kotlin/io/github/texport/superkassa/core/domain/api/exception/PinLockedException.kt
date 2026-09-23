package io.github.texport.superkassa.core.domain.api.exception

import io.github.texport.superkassa.core.string.api.CoreStrings

/**
 * Касса заперта после неверных пинов подряд, и никакой пин сейчас не принимается.
 *
 * @property retryAfterSeconds сколько секунд осталось до конца блокировки.
 */
class PinLockedException(val retryAfterSeconds: Long) :
    SuperkassaException("PIN_LOCKED", TOO_MANY_REQUESTS, CoreStrings.pinLocked(retryAfterSeconds)) {

    private companion object {
        const val TOO_MANY_REQUESTS = 429
    }
}
