package kz.mybrain.superkassa.core.application.service

import kz.mybrain.superkassa.core.application.model.CoreSettings
import kz.mybrain.superkassa.core.data.ofd.OfdResponseUtils
import kz.mybrain.superkassa.core.domain.model.FiscalDocumentSnapshot
import kz.mybrain.superkassa.core.domain.model.ReceiptRequest
import kz.mybrain.superkassa.core.domain.port.DeliveryPort
import kz.mybrain.superkassa.core.domain.port.DocumentConvertPort
import kz.mybrain.superkassa.core.domain.port.ReceiptRenderPort
import kotlinx.serialization.json.JsonObject

import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.model.ReceiptBranding

/**
 * Вспомогательный класс для доставки и повторной отправки фискальных чеков
 * по различным каналам (печать, электронная почта, мессенджеры).
 */
class ReceiptDeliveryHelper(
    private val storage: StoragePort,
    private val delivery: DeliveryPort,
    private val coreSettings: CoreSettings,
    private val documentConvertPort: DocumentConvertPort,
    private val receiptRenderPort: ReceiptRenderPort
) {
    fun deliverReceipt(
        kkmId: String,
        documentId: String,
        receipt: ReceiptRequest,
        docSnapshot: FiscalDocumentSnapshot,
        responseJson: JsonObject?,
        responseBin: ByteArray?
    ) {
        val kkm = storage.findKkm(kkmId) ?: kz.mybrain.superkassa.core.domain.model.KkmInfo(
            id = kkmId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            mode = "ACTIVE",
            state = "ACTIVE"
        )
        val html = receiptRenderPort.renderHtml(receipt, docSnapshot, kkm)
        val receiptUrl = OfdResponseUtils.extractReceiptUrl(responseJson)
        val del = coreSettings.delivery

        if (del == null) {
            if (responseBin != null) {
                delivery.deliver(
                    kz.mybrain.superkassa.core.domain.model.DeliveryRequest(
                        kkmId = kkmId,
                        documentId = documentId,
                        channel = "PRINT",
                        payloadType = "BINARY",
                        payloadBytes = responseBin
                    )
                )
            }
            return
        }

        val printSetting = del.print
        if (printSetting != null && printSetting.enabled) {
            val conn = printSetting.connection
            val host = conn?.host
            val port = conn?.port
            if (host != null && port != null) {
                val paperWidth = when (printSetting.paperWidthMm) {
                    48 -> 48
                    80 -> 80
                    else -> 58
                }
                val escPos = documentConvertPort.htmlToEscPos(html, paperWidth)
                delivery.deliver(
                    kz.mybrain.superkassa.core.domain.model.DeliveryRequest(
                        kkmId = kkmId,
                        documentId = documentId,
                        channel = "PRINT",
                        payloadType = "ESC_POS",
                        payloadBytes = escPos
                    )
                )
            }
        }

        del.channels.filter { it.enabled }.forEach { ch ->
            val dest = ch.destination ?: return@forEach
            deliverToChannel(kkmId, documentId, html, receiptUrl, ch, dest)
        }
    }

    private fun deliverToChannel(
        kkmId: String,
        documentId: String,
        html: String,
        receiptUrl: String?,
        ch: kz.mybrain.superkassa.core.application.model.DeliveryChannelSettings,
        dest: String
    ) {
        when (ch.payloadType.uppercase()) {
            "LINK" -> {
                if (receiptUrl != null) {
                    delivery.deliver(
                        kz.mybrain.superkassa.core.domain.model.DeliveryRequest(
                            kkmId = kkmId,
                            documentId = documentId,
                            channel = ch.channel,
                            destination = dest,
                            payloadType = "LINK",
                            payloadUrl = receiptUrl
                        )
                    )
                }
            }
            "DOCUMENT" -> {
                val bytes = convertDocument(html, ch.documentFormat)
                delivery.deliver(
                    kz.mybrain.superkassa.core.domain.model.DeliveryRequest(
                        kkmId = kkmId,
                        documentId = documentId,
                        channel = ch.channel,
                        destination = dest,
                        payloadType = ch.documentFormat.uppercase(),
                        payloadBytes = bytes
                    )
                )
            }
            "BOTH" -> {
                if (receiptUrl != null) {
                    delivery.deliver(
                        kz.mybrain.superkassa.core.domain.model.DeliveryRequest(
                            kkmId = kkmId,
                            documentId = documentId,
                            channel = ch.channel,
                            destination = dest,
                            payloadType = "LINK",
                            payloadUrl = receiptUrl
                        )
                    )
                }
                val bytes = convertDocument(html, ch.documentFormat)
                delivery.deliver(
                    kz.mybrain.superkassa.core.domain.model.DeliveryRequest(
                        kkmId = kkmId,
                        documentId = documentId,
                        channel = ch.channel,
                        destination = dest,
                        payloadType = ch.documentFormat.uppercase(),
                        payloadBytes = bytes
                    )
                )
            }
        }
    }

    private fun convertDocument(html: String, format: String): ByteArray {
        return when (format.uppercase()) {
            "PDF" -> documentConvertPort.htmlToPdf(html)
            "IMAGE" -> documentConvertPort.htmlToImage(html)
            else -> html.toByteArray(Charsets.UTF_8)
        }
    }

    fun retryDelivery(
        kkmId: String,
        documentId: String,
        receipt: ReceiptRequest,
        docSnapshot: FiscalDocumentSnapshot
    ): List<Pair<String, Boolean>> {
        val kkm = storage.findKkm(kkmId) ?: kz.mybrain.superkassa.core.domain.model.KkmInfo(
            id = kkmId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            mode = "ACTIVE",
            state = "ACTIVE"
        )
        val html = receiptRenderPort.renderHtml(receipt, docSnapshot, kkm)
        val del = coreSettings.delivery ?: return emptyList()
        val results = mutableListOf<Pair<String, Boolean>>()

        val printSetting = del.print
        if (printSetting != null && printSetting.enabled) {
            val conn = printSetting.connection
            val host = conn?.host
            val port = conn?.port
            if (host != null && port != null) {
                val paperWidth = when (printSetting.paperWidthMm) {
                    48 -> 48
                    80 -> 80
                    else -> 58
                }
                val escPos = documentConvertPort.htmlToEscPos(html, paperWidth)
                val req = kz.mybrain.superkassa.core.domain.model.DeliveryRequest(
                    kkmId = kkmId,
                    documentId = documentId,
                    channel = "PRINT",
                    payloadType = "ESC_POS",
                    payloadBytes = escPos
                )
                val ok = delivery.deliver(req)
                results.add("PRINT" to ok)
            }
        }

        del.channels.filter { it.enabled }.forEach { ch ->
            val dest = ch.destination ?: run {
                results.add(ch.channel to false)
                return@forEach
            }
            if (ch.payloadType.uppercase() == "LINK") {
                results.add(ch.channel to false)
            } else {
                val bytes = convertDocument(html, ch.documentFormat)
                val req = kz.mybrain.superkassa.core.domain.model.DeliveryRequest(
                    kkmId = kkmId,
                    documentId = documentId,
                    channel = ch.channel,
                    destination = dest,
                    payloadType = ch.documentFormat.uppercase(),
                    payloadBytes = bytes
                )
                val ok = delivery.deliver(req)
                results.add(ch.channel to ok)
            }
        }
        return results
    }
}
