package io.github.texport.superkassa.core.presentation.impl

import io.github.texport.superkassa.core.domain.api.port.internal.OfflineQueuePort
import io.github.texport.superkassa.core.domain.impl.usecase.queue.GetQueueStatusUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.queue.ListQueueItemsUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.queue.RetryFailedQueueItemsUseCase
import io.github.texport.superkassa.core.presentation.api.OfflineQueueApi
import io.github.texport.superkassa.core.presentation.api.model.queue.*

/**
 * Внутренняя реализация API управления оффлайн-очередью [OfflineQueueApi].
 *
 * @param queuePort Порт управления фоновыми задачами оффлайн-очереди.
 * @param getQueueStatusUseCase Сценарий получения статуса очереди.
 * @param listQueueItemsUseCase Сценарий получения списка элементов очереди.
 * @param retryFailedQueueItemsUseCase Сценарий повторной отправки неудавшихся задач.
 */
internal class OfflineQueueApiImpl(
    private val queuePort: OfflineQueuePort,
    private val getQueueStatusUseCase: GetQueueStatusUseCase,
    private val listQueueItemsUseCase: ListQueueItemsUseCase,
    private val retryFailedQueueItemsUseCase: RetryFailedQueueItemsUseCase
) : OfflineQueueApi {

    @Throws(Exception::class)
    override fun canSendDirectly(kkmId: String): Boolean {
        return queuePort.canSendDirectly(kkmId)
    }

    @Throws(Exception::class)
    override fun getQueueStatus(request: QueueStatusRequest): QueueStatusResponse {
        val status = getQueueStatusUseCase.execute(request.kkmId)
        return QueueStatusResponse(
            hasPendingItems = status.hasPendingItems,
            pendingCount = status.pendingCount
        )
    }

    @Throws(Exception::class)
    override fun processOfflineBatch(kkmId: String, limit: Int): Int {
        return queuePort.processOfflineBatch(kkmId, limit)
    }

    @Throws(Exception::class)
    override fun listQueue(kkmId: String, pin: String): List<QueueItemResponse> {
        return listQueueItemsUseCase.execute(kkmId, pin).map {
            QueueItemResponse(
                id = it.id,
                lane = it.lane,
                type = it.type,
                status = it.status,
                attempt = it.attempt,
                nextAttemptAt = it.nextAttemptAt,
                lastError = it.lastError,
                errorRu = it.errorRu,
                errorKk = it.errorKk,
                errorEn = it.errorEn
            )
        }
    }

    @Throws(Exception::class)
    override fun retryFailed(kkmId: String, pin: String): Int {
        return retryFailedQueueItemsUseCase.execute(kkmId, pin)
    }
}
