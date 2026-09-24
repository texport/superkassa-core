package io.github.texport.superkassa.core.domain.impl.usecase.delivery

import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryFailure
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryOutcome
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryRequest
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryTask
import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.model.settings.PrintDeliverySettings
import io.github.texport.superkassa.core.domain.api.port.integration.DocumentConvertPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.api.port.internal.ReceiptRenderPort
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.string.api.TrilingualMessage

/**
 * Что уходит в канал по задаче доставки: чек рисуется и переводится
 * в нужный вид в момент отправки, в фоне, а не при пробитии.
 *
 * @param print текущие настройки печати: из них берётся ширина ленты.
 */
class DeliveryRequests(
    private val storage: StoragePort,
    private val print: () -> PrintDeliverySettings?,
    private val convert: DocumentConvertPort,
    private val render: ReceiptRenderPort
) {
    /**
     * Запрос по задаче [task] или отказ, если отправлять нечего.
     *
     * Документа нет или у ссылки нет адреса — отказ окончательный:
     * повтор нашёл бы то же самое.
     */
    fun prepare(task: DeliveryTask): Prepared {
        val (document, receipt) = storage.findFiscalDocumentWithReceiptPayload(task.documentId)
            ?: return refused(DOCUMENT_MISSING, CoreStrings.deliveryDocumentMissing())
        if (task.payloadType == ReceiptDeliveryPlan.LINK) {
            val url = document.receiptUrl ?: return refused(LINK_MISSING, CoreStrings.deliveryLinkMissing())
            return Prepared.Ready(request(task, payloadUrl = url))
        }
        return Prepared.Ready(request(task, payloadBytes = bytes(task.payloadType, html(document, receipt))))
    }

    private fun html(document: FiscalDocumentSnapshot, receipt: ReceiptRequest): String {
        val kkm = storage.findKkm(document.cashboxId) ?: KkmInfo(
            id = document.cashboxId,
            createdAt = document.createdAt,
            updatedAt = document.createdAt,
            mode = "ACTIVE",
            state = "ACTIVE"
        )
        return render.renderHtml(receipt, document, kkm)
    }

    private fun bytes(payloadType: String, html: String): ByteArray = when (payloadType) {
        ReceiptDeliveryPlan.ESC_POS -> convert.htmlToEscPos(html, paperWidth())
        PDF -> convert.htmlToPdf(html)
        IMAGE -> convert.htmlToImage(html)
        else -> html.encodeToByteArray()
    }

    /** Лента 48 и 80 мм — как задана; всё прочее печатается на 58 мм. */
    private fun paperWidth(): Int = when (val width = print()?.paperWidthMm) {
        NARROW_PAPER, WIDE_PAPER -> width
        else -> DEFAULT_PAPER
    }

    private fun request(task: DeliveryTask, payloadUrl: String? = null, payloadBytes: ByteArray? = null) =
        DeliveryRequest(
            kkmId = task.kkmId,
            documentId = task.documentId,
            channel = task.channel,
            destination = task.destination,
            payloadType = task.payloadType,
            payloadUrl = payloadUrl,
            payloadBytes = payloadBytes
        )

    private fun refused(code: String, message: TrilingualMessage) =
        Prepared.Refused(DeliveryOutcome.failed(DeliveryFailure(code, message), retryable = false))

    /** Подготовленный запрос или отказ до отправки. */
    sealed interface Prepared {
        /** Запрос готов к отправке в канал. */
        data class Ready(val request: DeliveryRequest) : Prepared

        /** Отправлять нечего: итог известен без канала. */
        data class Refused(val outcome: DeliveryOutcome) : Prepared
    }

    /** Коды отказов до отправки. */
    companion object {
        /** Документа задачи нет в базе кассы. */
        const val DOCUMENT_MISSING: String = "DELIVERY_DOCUMENT_MISSING"

        /** Задача ссылки, а ссылки на чек БФД не дал. */
        const val LINK_MISSING: String = "DELIVERY_LINK_MISSING"

        private const val PDF = "PDF"
        private const val IMAGE = "IMAGE"
        private const val NARROW_PAPER = 48
        private const val WIDE_PAPER = 80
        private const val DEFAULT_PAPER = 58
    }
}
