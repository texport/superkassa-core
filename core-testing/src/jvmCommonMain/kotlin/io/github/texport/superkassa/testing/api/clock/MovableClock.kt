package io.github.texport.superkassa.testing.api.clock

import io.github.texport.superkassa.core.data.api.systemClock
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import kotlin.time.Duration

/**
 * Часы кассы, которые стоят, пока проверка не переведёт их вперёд или назад.
 *
 * Время само не идёт: граница блокировки проверяется с точностью
 * до миллисекунды, и медленный прогон её не сдвигает. Пауза
 * восстановления связи и сутки смены проходят без ожидания.
 *
 * @param startMillis время кассы в начале, миллисекунды эпохи; по умолчанию — время машины.
 */
class MovableClock(startMillis: Long = System.currentTimeMillis()) : ClockPort by systemClock() {
    @Volatile
    private var nowMs = startMillis

    override fun now(): Long = nowMs

    /** Переводит часы на [ms] миллисекунд; отрицательное значение — назад. */
    fun move(ms: Long) {
        nowMs += ms
    }

    /** Переводит часы на [by]; отрицательное значение — назад. */
    fun move(by: Duration) = move(by.inWholeMilliseconds)
}
