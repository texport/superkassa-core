package io.github.texport.superkassa.core.data.api

import io.github.texport.superkassa.core.data.impl.adapter.DefaultClockAdapter
import io.github.texport.superkassa.core.data.impl.adapter.time.SystemTimeGuard
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.integration.TimeValidatorPort

/** Часы системы: время кассы — время машины. */
fun systemClock(): ClockPort = DefaultClockAdapter()

/**
 * Проверка часов кассы: диапазон, перевод назад, скачок вперёд и эталон в сети.
 *
 * Один экземпляр на кассу: проверка помнит прошлый замер и по нему
 * замечает перевод часов.
 */
fun systemTimeGuard(): TimeValidatorPort = SystemTimeGuard()
