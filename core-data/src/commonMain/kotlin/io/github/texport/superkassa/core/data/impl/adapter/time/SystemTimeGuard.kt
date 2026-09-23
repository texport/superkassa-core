package io.github.texport.superkassa.core.data.impl.adapter.time

import io.github.texport.superkassa.core.domain.api.model.common.TimeValidationResult
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.integration.TimeValidatorPort
import io.github.texport.superkassa.core.domain.impl.logging.getLogger
import io.github.texport.superkassa.core.string.api.CoreStrings
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.abs
import kotlin.time.TimeSource

/**
 * Проверка часов кассы перед фискальной операцией.
 *
 * Отказывает, если время вне разумного диапазона, если часы переведены
 * назад относительно монотонных, и если они расходятся с эталоном в сети.
 * Скачок вперёд без эталона тоже отказ: сон машины от перевода стрелок
 * отличает только эталон. Правило одно для узла и для приложения —
 * иначе касса в приложении принимала бы часы, которые узел отверг бы.
 *
 * @param reference источник эталонного времени; `null` — эталона нет.
 */
@OptIn(ExperimentalAtomicApi::class)
internal class SystemTimeGuard(
    private val reference: () -> Long? = ::fetchReferenceTime
) : TimeValidatorPort {
    private val logger = getLogger(SystemTimeGuard::class)
    private val anchor = AtomicReference<Anchor?>(null)
    private val cache = AtomicReference(ReferenceCache())

    override fun validate(clock: ClockPort): TimeValidationResult {
        val now = clock.now()
        if (now !in MIN_ALLOWED_MS..MAX_ALLOWED_MS) return refusal("RANGE")
        val skew = skewSinceLastCheck(now)
        if (skew < -MAX_MONOTONIC_SKEW_MS) return refusal("MONOTONIC_SKEW")
        val referenceNow = referenceAt(now)
        if (referenceNow != null && abs(now - referenceNow) > MAX_REFERENCE_SKEW_MS) return refusal("REFERENCE_SKEW")
        if (skew > MAX_MONOTONIC_SKEW_MS && referenceNow == null) return refusal("MONOTONIC_SKEW")
        return TimeValidationResult(ok = true)
    }

    /** Разрыв настенных часов с монотонными со времени прошлой проверки; базис сдвигается всегда. */
    private fun skewSinceLastCheck(now: Long): Long {
        val previous = anchor.exchange(Anchor(now, TimeSource.Monotonic.markNow())) ?: return 0L
        val skew = now - (previous.wallMs + previous.mark.elapsedNow().inWholeMilliseconds)
        if (abs(skew) > MAX_MONOTONIC_SKEW_MS) logger.warn("System clock jumped by {} ms", skew)
        return skew
    }

    /** Эталонное время на момент [now]: из свежего замера либо из нового, но не чаще раза в минуту. */
    private fun referenceAt(now: Long): Long? {
        val current = cache.load()
        val fetchedAt = current.fetchedAtMs
        if (current.referenceMs != null && fetchedAt != null && now - fetchedAt <= REFERENCE_TTL_MS) {
            return current.referenceMs + (now - fetchedAt)
        }
        val lastAttempt = current.lastAttemptMs
        if (lastAttempt != null && now - lastAttempt <= RETRY_COOL_DOWN_MS) return null
        cache.store(current.copy(lastAttemptMs = now))
        val fetched = reference()
        cache.store(ReferenceCache(fetched, fetched?.let { now }, now))
        return fetched
    }

    private fun refusal(reason: String): TimeValidationResult =
        TimeValidationResult(ok = false, reason = reason, trilingualMessage = CoreStrings.systemTimeInvalid())

    private class Anchor(val wallMs: Long, val mark: TimeSource.Monotonic.ValueTimeMark)

    private data class ReferenceCache(
        val referenceMs: Long? = null,
        val fetchedAtMs: Long? = null,
        val lastAttemptMs: Long? = null
    )

    private companion object {
        const val MIN_ALLOWED_MS = 1_577_836_800_000L // 2020-01-01T00:00:00Z
        const val MAX_ALLOWED_MS = 4_102_444_800_000L // 2100-01-01T00:00:00Z
        const val MAX_MONOTONIC_SKEW_MS = 2 * 60 * 1000L
        const val MAX_REFERENCE_SKEW_MS = 5 * 60 * 1000L
        const val REFERENCE_TTL_MS = 10 * 60 * 1000L
        const val RETRY_COOL_DOWN_MS = 60 * 1000L
    }
}

/**
 * Эталонное время из заголовка `Date` известных сайтов, в миллисекундах.
 *
 * @return время первого ответившего сайта либо `null`, если не ответил никто.
 */
internal expect fun fetchReferenceTime(): Long?
