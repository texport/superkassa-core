package io.github.texport.superkassa.core.data.room

import io.github.texport.superkassa.core.data.impl.adapter.DefaultClockAdapter
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import kotlin.time.Duration

/** Часы, которые идут только по команде проверки. */
internal class ManualClock(private var millis: Long = START) : ClockPort by DefaultClockAdapter() {
    override fun now(): Long = millis

    fun advance(by: Duration) {
        millis += by.inWholeMilliseconds
    }

    private companion object {
        /** 2026-09-01 09:00 по Алматы. */
        const val START = 1_788_235_200_000L
    }
}
