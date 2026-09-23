package io.github.texport.superkassa.importnode.impl.verify

import io.github.texport.superkassa.core.domain.api.model.common.CounterScopes
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.model.zxreport.ZxReportInput
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.helper.zxreport.ZxReportBuilder
import io.github.texport.superkassa.core.domain.impl.usecase.shift.RecalculateShiftCountersUseCase

/**
 * X-отчёт открытой смены до переноса и после.
 *
 * Узел для этого не поднимается: отчёт собирает сборщик ядра из счётчиков
 * смены — тех, что лежали у узла, и тех, что касса читает из своей базы.
 * Время отчёта общее, чтобы сравнивались суммы, а не часы.
 *
 * Отдельно — пересчёт смены ядром по перенесённым документам: так ядро
 * собирает X-отчёт для ОФД. Он показывает, что документы и чеки
 * перенеслись так, что из них выходят те же суммы.
 */
internal class XReportCheck(private val port: StoragePort) {

    /** Совпадение X-отчёта по счётчикам и по пересчёту документов. */
    data class Outcome(val stored: Boolean, val recalculated: Boolean)

    fun compare(shift: ShiftInfo, nodeCounters: Map<String, Long>): Outcome {
        val before = report(shift, nodeCounters)
        val stored = port.loadCounters(shift.kkmId, CounterScopes.SHIFT, shift.id)
        val recalculated = RecalculateShiftCountersUseCase(port).rebuildShiftCounters(shift.kkmId, shift)
        return Outcome(stored = report(shift, stored) == before, recalculated = report(shift, recalculated) == before)
    }

    private fun report(shift: ShiftInfo, counters: Map<String, Long>): ZxReportInput = ZxReportBuilder.build(
        counters = counters,
        dateTimeMillis = shift.openedAt,
        shiftNumber = Math.toIntExact(shift.shiftNo),
        openShiftTimeMillis = shift.openedAt,
        closeShiftTimeMillis = null
    )
}
