package io.github.texport.superkassa.core.domain.impl.usecase.receipt

import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.helper.ReceiptDeliveryHelper
import io.github.texport.superkassa.core.domain.impl.logging.getLogger
import io.github.texport.superkassa.core.domain.impl.usecase.delivery.ReceiptDeliveryPlan

/**
 * Сценарий доставки чека покупателю после того, как БФД его принял.
 *
 * Доставка ставится задачами в хранилище и уходит в фоне: кассир получает
 * ответ сразу, а медленный провайдер держит только фон. Хранилище без
 * задач доставки — доставка сразу, в потоке пробития, как прежде.
 *
 * Отказ доставки — будь то запись задачи, рисование, принтер или канал —
 * не выдаёт себя за отказ чека: кассир пробил бы его повторно, и у
 * покупателя стало бы два фискальных документа на одну покупку.
 *
 * @property helper доставка сразу — для хранилища без задач.
 * @property plan какие задачи ставит чек.
 */
class DeliverReceiptUseCase(
    private val helper: ReceiptDeliveryHelper,
    private val storage: StoragePort,
    private val plan: ReceiptDeliveryPlan,
    private val clock: ClockPort
) {
    private val logger = getLogger(DeliverReceiptUseCase::class)

    /**
     * Ставит доставку фискального чека покупателю.
     *
     * @param kkmId Идентификатор кассового аппарата (ККМ).
     * @param documentId Уникальный идентификатор фискального документа.
     * @param receipt Исходный запрос чека.
     * @param docSnapshot Снимок состояния фискального документа из базы данных.
     * @param receiptUrl Ссылка на электронный чек на сервере ОФД (при наличии).
     * @param responseBin Бинарные данные ответа ОФД: нужны только доставке сразу.
     */
    fun execute(
        kkmId: String,
        documentId: String,
        receipt: ReceiptRequest,
        docSnapshot: FiscalDocumentSnapshot,
        receiptUrl: String?,
        responseBin: ByteArray?
    ) {
        val tasks = plan.tasksFor(kkmId, documentId, hasLink = receiptUrl != null, now = clock.now())
        val failure = runCatching { storage.addDeliveryTasks(tasks) }.exceptionOrNull() ?: return
        if (failure is UnsupportedOperationException) {
            helper.deliverReceipt(kkmId, documentId, receipt, docSnapshot, receiptUrl, responseBin)
            return
        }
        logger.warn("Receipt delivery was not queued: documentId={}, reason={}", documentId, failure::class.simpleName)
    }
}
