package io.github.texport.superkassa.core.data.room

import io.github.texport.superkassa.core.data.impl.adapter.DefaultClockAdapter
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort

/**
 * Остановленные часы, которые проверка переводит вперёд и назад.
 *
 * Время само не идёт: граница блокировки проверяется с точностью
 * до миллисекунды, и медленный прогон её не сдвигает.
 */
internal class MovableClock : ClockPort by DefaultClockAdapter() {
    private var nowMs = System.currentTimeMillis()

    override fun now(): Long = nowMs

    fun move(ms: Long) {
        nowMs += ms
    }
}
