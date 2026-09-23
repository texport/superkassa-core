package io.github.texport.superkassa.core.data.room

import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryRequest
import io.github.texport.superkassa.core.domain.api.model.queue.QueueTask
import io.github.texport.superkassa.core.domain.api.port.integration.DeliveryPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.presentation.api.model.receipt.ParentTicketRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptItemRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptSellReturnRequest
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
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

/**
 * Хранилище, которое считает постановки документов в очередь досылки.
 *
 * Обёртка, как у приложения: счёта неверных пинов она не несёт, и ядро
 * получает его отдельно, из базы Room.
 */
internal class CountingStorage(private val room: StoragePort) : StoragePort by room {
    private val enqueued = CopyOnWriteArrayList<String>()

    /** Сколько раз документ [documentId] ставили в очередь, считая отвергнутые хранилищем попытки. */
    fun enqueueAttempts(documentId: String): Int = enqueued.count { it == documentId }

    override fun enqueueQueueTask(dto: QueueTask): Boolean {
        enqueued += dto.payloadRef
        return room.enqueueQueueTask(dto)
    }
}

/** Возврат одной строкой без ставки, как его собирает касса при возврате суммой. */
internal fun RoomKassa.refundByAmount(saleId: String, saleTotal: String, amount: String): String {
    val sale = document(saleId)
    val basis = ParentTicketRequest(
        parentTicketNumber = checkNotNull(sale.docNo),
        parentTicketDateTime = Instant.ofEpochMilli(sale.createdAt).truncatedTo(ChronoUnit.SECONDS)
            .atOffset(ZoneOffset.UTC).toLocalDateTime().toString(),
        kgdKkmId = RoomKassa.KGD_NUMBER,
        parentTicketTotal = Decimal.parse(saleTotal),
        parentTicketIsOffline = false
    )
    val byAmount = ReceiptItemRequest(name = "Возврат", price = Decimal.parse(amount), quantity = Decimal.parse("1"))
    return api.createSellReturnReceipt(
        RoomKassa.KKM, RoomKassa.CASHIER_PIN,
        ReceiptSellReturnRequest(
            idempotencyKey = "return-$amount", items = listOf(byAmount), payments = listOf(RoomKassa.cash(amount)), parentTicket = basis
        )
    ).documentId
}

/** Пауза восстановления связи прошла, и очередь досылается. */
internal fun RoomKassa.reconnectAndResend(clock: MovableClock): Int {
    clock.move(RECONNECT_MILLIS)
    return api.queue.processOfflineBatch(RoomKassa.KKM, limit = 100)
}

private const val RECONNECT_MILLIS = 61_000L
