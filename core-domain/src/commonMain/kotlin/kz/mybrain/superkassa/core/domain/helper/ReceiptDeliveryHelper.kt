package kz.mybrain.superkassa.core.domain.helper

import kz.mybrain.superkassa.core.domain.model.delivery.DeliveryRequest
import kz.mybrain.superkassa.core.domain.model.kkm.FiscalDocumentSnapshot
import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptRequest
import kz.mybrain.superkassa.core.domain.model.settings.CoreSettings
import kz.mybrain.superkassa.core.domain.model.settings.DeliveryChannelSettings
import kz.mybrain.superkassa.core.domain.port.DeliveryPort
import kz.mybrain.superkassa.core.domain.port.DocumentConvertPort
import kz.mybrain.superkassa.core.domain.port.ReceiptRenderPort
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kotlinx.datetime.Clock

/**
 * Вспомогательный класс для доставки и повторной отправки фискальных чеков
 * по различным каналам (печать на принтере чеков, электронная почта, мессенджеры и др.).
 *
 * @property storage Порт для доступа к хранилищу данных ККМ.
 * @property delivery Порт для физической отправки документов получателю.
 * @property coreSettings Объект настроек системы (включая параметры каналов доставки).
 * @property documentConvertPort Порт для конвертации документов в различные форматы (PDF, ESC/POS, изображения).
 * @property receiptRenderPort Порт визуализации чека (генерация HTML-представления чека).
 */
class ReceiptDeliveryHelper(
    private val storage: StoragePort,
    private val delivery: DeliveryPort,
    private val coreSettings: CoreSettings,
    private val documentConvertPort: DocumentConvertPort,
    private val receiptRenderPort: ReceiptRenderPort
) {
    /**
     * Выполняет первичную доставку чека по всем активным каналам связи.
     *
     * 1. Рендерит HTML-представление чека на основе запроса и фискального снимка.
     * 2. При отсутствии настроек доставки выполняет резервную печать (при наличии [responseBin]).
     * 3. Если настроена печать, конвертирует HTML в ESC/POS под нужную ширину бумаги (48, 80 или 58 мм)
     *    и отправляет на принтер.
     * 4. Отправляет чек во все включенные цифровые каналы (E-mail, мессенджеры) в виде ссылки, документа или обоих вариантов.
     *
     * @param kkmId Уникальный идентификатор ККМ.
     * @param documentId Уникальный идентификатор фискального документа.
     * @param receipt Запрос на создание чека с исходными данными.
     * @param docSnapshot Фискальный снимок документа (данные от ОФД).
     * @param receiptUrl Ссылка на электронную версию чека на портале ОФД.
     * @param responseBin Сырой бинарный ответ от ОФД (используется для резервной печати).
     */
    fun deliverReceipt(
        kkmId: String,
        documentId: String,
        receipt: ReceiptRequest,
        docSnapshot: FiscalDocumentSnapshot,
        receiptUrl: String?,
        responseBin: ByteArray?
    ) {
        val kkm = storage.findKkm(kkmId) ?: KkmInfo(
            id = kkmId,
            createdAt = Clock.System.now().toEpochMilliseconds(),
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            mode = "ACTIVE",
            state = "ACTIVE"
        )
        val html = receiptRenderPort.renderHtml(receipt, docSnapshot, kkm)
        val del = coreSettings.delivery

        if (del == null) {
            if (responseBin != null) {
                delivery.deliver(
                    DeliveryRequest(
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
                    DeliveryRequest(
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

    /**
     * Отправляет чек в конкретный цифровой канал доставки.
     */
    private fun deliverToChannel(
        kkmId: String,
        documentId: String,
        html: String,
        receiptUrl: String?,
        ch: DeliveryChannelSettings,
        dest: String
    ) {
        when (ch.payloadType.uppercase()) {
            "LINK" -> {
                if (receiptUrl != null) {
                    delivery.deliver(
                        DeliveryRequest(
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
                    DeliveryRequest(
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
                        DeliveryRequest(
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
                    DeliveryRequest(
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

    /**
     * Преобразует HTML-документ чека в бинарный массив заданного формата (PDF, IMAGE или UTF-8).
     */
    private fun convertDocument(html: String, format: String): ByteArray {
        return when (format.uppercase()) {
            "PDF" -> documentConvertPort.htmlToPdf(html)
            "IMAGE" -> documentConvertPort.htmlToImage(html)
            else -> html.encodeToByteArray()
        }
    }

    /**
     * Выполняет повторную отправку фискального чека (при сбоях доставки или по запросу клиента).
     *
     * @param kkmId Уникальный идентификатор ККМ.
     * @param documentId Уникальный идентификатор фискального документа.
     * @param receipt Запрос на создание чека.
     * @param docSnapshot Снимок фискального документа.
     * @return Список пар (канал доставки -> признак успешности отправки).
     */
    fun retryDelivery(
        kkmId: String,
        documentId: String,
        receipt: ReceiptRequest,
        docSnapshot: FiscalDocumentSnapshot
    ): List<Pair<String, Boolean>> {
        val kkm = storage.findKkm(kkmId) ?: KkmInfo(
            id = kkmId,
            createdAt = Clock.System.now().toEpochMilliseconds(),
            updatedAt = Clock.System.now().toEpochMilliseconds(),
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
                val req = DeliveryRequest(
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
                val req = DeliveryRequest(
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
