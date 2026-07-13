package io.github.texport.superkassa.core.domain.impl.usecase.queue

import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort

/**
 * Сценарий (Use Case) для получения агрегированного статуса оффлайн-очереди ККМ.
 *
 * Вычисляет наличие неотправленных задач и их общее количество.
 *
 * @property storage Порт для доступа к хранилищу.
 */
class GetQueueStatusUseCase(
    private val storage: StoragePort
) {
    /**
     * Результат выполнения сценария.
     *
     * @property hasPendingItems Наличие неотправленных задач.
     * @property pendingCount Количество неотправленных задач.
     */
    data class QueueStatus(
        val hasPendingItems: Boolean,
        val pendingCount: Int
    )

    /**
     * Выполняет получение статуса оффлайн-очереди.
     *
     * @param kkmId Уникальный идентификатор ККМ.
     * @return Объект [QueueStatus] с информацией о состоянии очереди.
     */
    fun execute(kkmId: String): QueueStatus {
        val tasks = storage.listQueueTasksByCashbox(kkmId, "OFFLINE", limit = 500)
        val pendingCount = tasks.count { it.status == "PENDING" || it.status == "FAILED" }
        return QueueStatus(
            hasPendingItems = pendingCount > 0,
            pendingCount = pendingCount
        )
    }
}
