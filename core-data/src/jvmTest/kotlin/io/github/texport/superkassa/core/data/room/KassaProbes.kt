package io.github.texport.superkassa.core.data.room

import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryRequest
import io.github.texport.superkassa.core.domain.api.model.queue.QueueTask
import io.github.texport.superkassa.core.domain.api.port.integration.DeliveryPort
import io.github.texport.superkassa.core.domain.api.port.integration.PinAttemptsPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import java.util.concurrent.CopyOnWriteArrayList

/** Доставка чека покупателю, записанная вместо отправки. */
internal class DeliveryLog : DeliveryPort {
    private val delivered = CopyOnWriteArrayList<DeliveryRequest>()

    fun of(documentId: String): List<DeliveryRequest> = delivered.filter { it.documentId == documentId }

    override fun deliver(request: DeliveryRequest): Boolean {
        delivered += request
        return true
    }
}

/** Хранилище, которое считает постановки документов в очередь досылки. */
internal class CountingStorage(private val room: StoragePort) :
    StoragePort by room,
    PinAttemptsPort by (room as PinAttemptsPort) {
    private val enqueued = CopyOnWriteArrayList<String>()

    /** Сколько раз документ [documentId] ставили в очередь, считая отвергнутые хранилищем попытки. */
    fun enqueueAttempts(documentId: String): Int = enqueued.count { it == documentId }

    override fun enqueueQueueTask(dto: QueueTask): Boolean {
        enqueued += dto.payloadRef
        return room.enqueueQueueTask(dto)
    }
}

/** Пауза восстановления связи прошла, и очередь досылается. */
internal fun RoomKassa.reconnectAndResend(clock: MovableClock): Int {
    clock.move(RECONNECT_MILLIS)
    return api.queue.processOfflineBatch(RoomKassa.KKM, limit = 100)
}

private const val RECONNECT_MILLIS = 61_000L
