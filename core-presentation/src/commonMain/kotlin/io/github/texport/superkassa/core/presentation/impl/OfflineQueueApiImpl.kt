package io.github.texport.superkassa.core.presentation.impl

import io.github.texport.superkassa.core.domain.port.OfflineQueuePort
import io.github.texport.superkassa.core.domain.usecase.queue.GetQueueStatusUseCase
import io.github.texport.superkassa.core.presentation.api.OfflineQueueApi
import io.github.texport.superkassa.core.presentation.api.model.QueueStatusRequest
import io.github.texport.superkassa.core.presentation.api.model.QueueStatusResponse

/**
 * Внутренняя реализация API управления оффлайн-очередью [OfflineQueueApi].
 *
 * @param queuePort Порт управления фоновыми задачами оффлайн-очереди.
 * @param getQueueStatusUseCase Сценарий получения статуса очереди.
 */
internal class OfflineQueueApiImpl(
    private val queuePort: OfflineQueuePort,
    private val getQueueStatusUseCase: GetQueueStatusUseCase
) : OfflineQueueApi {

    override fun canSendDirectly(kkmId: String): Boolean {
        return queuePort.canSendDirectly(kkmId)
    }

    override fun getQueueStatus(request: QueueStatusRequest): QueueStatusResponse {
        val status = getQueueStatusUseCase.execute(request.kkmId)
        return QueueStatusResponse(
            hasPendingItems = status.hasPendingItems,
            pendingCount = status.pendingCount
        )
    }

    override fun processOfflineBatch(kkmId: String, limit: Int): Int {
        return queuePort.processOfflineBatch(kkmId, limit)
    }
}
