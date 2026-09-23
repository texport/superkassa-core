package io.github.texport.superkassa.importnode.api

import kotlinx.serialization.Serializable

/**
 * Итог переноса: что перенесено по каждой кассе и что осталось у узла.
 *
 * Токена, пинов и содержимого документов здесь нет — только числа.
 *
 * @property kkms сверка по каждой кассе.
 * @property notTransferred непустые таблицы и колонки узла, которым в базе кассы
 *   нет места и которые фискального не несут: имя → число строк.
 * @property settingsTransferred перенесены ли настройки узла.
 */
@Serializable
data class NodeImportReport(
    val kkms: List<KkmImportReport>,
    val notTransferred: Map<String, Long>,
    val settingsTransferred: Boolean
)

/**
 * Сверка одной кассы после переноса.
 *
 * @property lastDocumentNumber наибольший номер документа ОФД.
 * @property lastPrintedDocumentNumber наибольший номер печатного документа.
 * @property openShiftNumber номер открытой смены; `null` — смена закрыта.
 * @property openShiftCashTiyn наличные открытой смены по счётчику, в тиынах.
 * @property queueByStatus задачи очереди по состояниям.
 * @property xReportMatches X-отчёт по счётчикам открытой смены совпал до и после.
 * @property recalculationMatches пересчёт смены ядром по перенесённым документам
 *   даёт тот же X-отчёт, что и счётчики. Расхождение не отменяет перенос:
 *   его показал бы и сам узел, пересчитав смену.
 */
@Serializable
data class KkmImportReport(
    val kkmId: String,
    val documents: Int,
    val lastDocumentNumber: Long?,
    val lastPrintedDocumentNumber: Long?,
    val shifts: Int,
    val openShiftNumber: Long?,
    val openShiftCashTiyn: Long?,
    val counters: Int,
    val queueByStatus: Map<String, Int>,
    val users: Int,
    val idempotencyKeys: Int,
    val xReportMatches: Boolean?,
    val recalculationMatches: Boolean?
)
