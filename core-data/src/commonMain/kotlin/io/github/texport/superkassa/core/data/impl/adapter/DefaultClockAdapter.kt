package io.github.texport.superkassa.core.data.impl.adapter

import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Реализация системных часов по умолчанию для Kotlin Multiplatform.
 *
 * Использует [kotlin.time.Clock.System] и [kotlinx.datetime.TimeZone] для вычисления времени
 * без использования платформа-зависимых вызовов System.currentTimeMillis() или java.time.
 */
internal class DefaultClockAdapter : ClockPort {

    /**
     * Возвращает текущее системное время в миллисекундах от эпохи Unix UTC.
     *
     * @return штамп времени UTC в миллисекундах.
     */
    override fun now(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()

    /**
     * Возвращает текущий календарный год в системном часовом поясе.
     *
     * @return текущий год (например, 2026).
     */
    override fun currentYear(): Int {
        val instant = kotlin.time.Clock.System.now()
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return local.year
    }

    /**
     * Преобразует календарные компоненты даты и времени в UNIX timestamp в миллисекундах.
     *
     * @param year год (например 2026)
     * @param month месяц (1..12)
     * @param day день месяца (1..31)
     * @param hour час (0..23)
     * @param minute минута (0..59)
     * @param second секунда (0..59)
     * @return штамп времени в миллисекундах в текущем часовом поясе.
     */
    override fun parseDateTimeToMillis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int
    ): Long {
        val ldt = kotlinx.datetime.LocalDateTime(year, month, day, hour, minute, second)
        val instant = ldt.toInstant(TimeZone.currentSystemDefault())
        return instant.toEpochMilliseconds()
    }
}
