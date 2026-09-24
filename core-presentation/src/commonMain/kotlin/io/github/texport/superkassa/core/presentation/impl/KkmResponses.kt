package io.github.texport.superkassa.core.presentation.impl

import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.queue.QueueStatusRequest
import io.github.texport.superkassa.core.presentation.impl.mapper.KkmMapper

/**
 * Касса в ответе — одна и та же для всех команд: сама касса, её открытая
 * смена, очередь досылки и последняя ошибка досылки.
 *
 * Прежде полной её собирали только чтение кассы и список касс, а заведение,
 * настройки, название и программирование отдавали кассу без смены и очереди:
 * после переименования экран показывал закрытую смену и пустую очередь.
 * Сбой чтения очереди ответ не роняет: касса отдаётся без её сведений.
 */
internal fun SuperkassaApiImpl.kkmResponse(kkm: KkmInfo): KkmResponse {
    val openShift = storage.findOpenShift(kkm.id)
    val queueStatus = try {
        queue.getQueueStatus(QueueStatusRequest(kkm.id))
    } catch (_: Exception) {
        null
    }
    val lastError = try {
        storage.listQueueTasksByCashbox(kkm.id, "OFFLINE", LAST_TASKS).firstOrNull { it.status == "FAILED" }?.lastError
    } catch (_: Exception) {
        null
    }
    return KkmMapper.toResponse(kkm).copy(
        isShiftOpen = openShift != null,
        shiftOpenedAt = openShift?.openedAt,
        offlineQueueCount = queueStatus?.pendingCount ?: 0,
        stuckQueueCount = queueStatus?.rejectedCount ?: 0,
        lastSyncError = lastError
    )
}

/** Сколько последних задач досылки просматривается в поисках ошибки. */
private const val LAST_TASKS = 20
