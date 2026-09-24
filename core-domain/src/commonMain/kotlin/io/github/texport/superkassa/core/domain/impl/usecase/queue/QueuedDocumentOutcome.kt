package io.github.texport.superkassa.core.domain.impl.usecase.queue

import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.queue.QueueTask
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.usecase.receipt.DeliverReceiptUseCase

/**
 * Что становится с документом, когда БФД ответил на его досылку.
 *
 * Принятый документ отмечается отправленным и получает ссылку БФД, а чек
 * уходит покупателю; отвергнутый — отмечается кодом и причиной отказа.
 * У служебных команд документа нет, и по ссылке ничего не находится.
 *
 * @property deliverReceipt доставка чека покупателю.
 */
class QueuedDocumentOutcome(
    private val storage: StoragePort,
    private val clock: ClockPort,
    private val deliverReceipt: DeliverReceiptUseCase
) {
    /** БФД принял документ задачи [command]; [isReceipt] — это чек покупателю. */
    fun accepted(command: QueueTask, result: OfdCommandResult, isReceipt: Boolean) {
        val doc = storage.findFiscalDocumentById(command.payloadRef) ?: return
        storage.updateReceiptStatus(
            documentId = command.payloadRef,
            fiscalSign = result.fiscalSign,
            autonomousSign = doc.autonomousSign ?: result.autonomousSign,
            ofdStatus = "SENT",
            ofdErrorCode = null,
            deliveredAt = clock.now(),
            // Признак автономности снимать нельзя: он говорит не о том, доставлен
            // ли документ, а о том, что он был фискализирован в разрыве связи.
            isAutonomous = doc.isAutonomous,
            ofdErrorText = null
        )
        // Досланный из очереди чек получает ссылку только теперь: без неё
        // перепечатанный чек выходил без QR-кода проверки.
        result.receiptUrl?.let { storage.saveReceiptUrl(command.payloadRef, it) }
        if (isReceipt) deliver(command, result)
    }

    /** БФД отверг документ задачи [command] кодом [code] с причиной [reason]. */
    fun rejected(command: QueueTask, code: Int, reason: String?) {
        val doc = storage.findFiscalDocumentById(command.payloadRef) ?: return
        storage.updateReceiptStatus(
            documentId = command.payloadRef,
            fiscalSign = null,
            autonomousSign = doc.autonomousSign,
            ofdStatus = "FAILED",
            ofdErrorCode = code,
            deliveredAt = null,
            // Признак автономности снимать нельзя: он говорит о том, что
            // документ был оформлен в разрыве связи, а не о его доставке.
            isAutonomous = doc.isAutonomous,
            ofdErrorText = reason?.takeIf { it.isNotBlank() }
        )
    }

    /**
     * Досланный чек покупатель получает так же, как пробитый онлайн.
     *
     * Прежде доставку ставил только ответ на пробитие, и чек, оформленный
     * в разрыве связи, покупателю не уходил никогда. Задачи доставки
     * именуются документом, каналом и видом, поэтому повторная досылка
     * того же чека второй доставки не ставит.
     */
    private fun deliver(command: QueueTask, result: OfdCommandResult) {
        val (document, receipt) = storage.findFiscalDocumentWithReceiptPayload(command.payloadRef) ?: return
        deliverReceipt.execute(
            kkmId = command.cashboxId,
            documentId = command.payloadRef,
            receipt = receipt,
            docSnapshot = document,
            receiptUrl = result.receiptUrl ?: document.receiptUrl,
            responseBin = result.responseBin
        )
    }
}
