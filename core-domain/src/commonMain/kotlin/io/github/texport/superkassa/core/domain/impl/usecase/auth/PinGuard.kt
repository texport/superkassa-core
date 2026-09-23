package io.github.texport.superkassa.core.domain.impl.usecase.auth

import io.github.texport.superkassa.core.domain.api.exception.PinLockedException
import io.github.texport.superkassa.core.domain.api.port.integration.PinAttemptsPort
import io.github.texport.superkassa.core.domain.impl.logging.getLogger
import kotlin.time.Clock

/**
 * Пускает проверку пина только на незапертую кассу и ведёт счёт неудач.
 *
 * Попытка засчитывается неудачной до проверки, а не после: параллельные
 * попытки не проскакивают между «не заперта» и «записали ошибку», и за
 * блокировку не проходит ни одна лишняя. Верный пин счёт обнуляет.
 *
 * Один на сборку ядра: все входы по пину — фасад, печать, доставка —
 * делят один счёт. Где он ведётся, решает сборка и передаёт явно: база
 * кассы на Room держит его на диске, узел — в памяти процесса.
 *
 * @param attempts счёт по кассам.
 * @param now текущее время, epoch ms.
 */
class PinGuard(
    private val attempts: PinAttemptsPort,
    private val now: () -> Long = ::systemNow
) {
    private val logger = getLogger(PinGuard::class)

    /**
     * Проверяет пин через [verify], если касса не заперта.
     *
     * @return то, что нашёл [verify], или null, если пин неверен.
     * @throws PinLockedException если касса заперта или эта ошибка её заперла.
     */
    fun <T : Any> check(kkmId: String, verify: () -> T?): T? {
        val at = now()
        val before = attempts.getAndUpdate(kkmId) { it.charged(at) }
        val after = before.charged(at)
        if (before.isLockedAt(at)) throw locked(kkmId, after.lockedUntil - at)
        val found = verify()
        if (found != null) {
            attempts.clear(kkmId)
            return found
        }
        if (after.isLockedAt(at)) throw locked(kkmId, after.lockedUntil - at)
        return null
    }

    private fun locked(kkmId: String, remainingMs: Long): PinLockedException {
        val seconds = (remainingMs + MS_IN_SECOND - 1) / MS_IN_SECOND
        logger.warn("PIN entry locked for kkmId='{}', {} s left", kkmId, seconds)
        return PinLockedException(seconds)
    }

    companion object {
        private const val MS_IN_SECOND = 1000L

        private fun systemNow(): Long = Clock.System.now().toEpochMilliseconds()
    }
}
