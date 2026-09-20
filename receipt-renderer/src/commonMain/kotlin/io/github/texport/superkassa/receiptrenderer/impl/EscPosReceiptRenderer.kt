package io.github.texport.superkassa.receiptrenderer.impl

import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest

/**
 * Рендерер чеков в бинарный поток команд ESC/POS для чековых термопринтеров.
 *
 * Формирует байтовый массив управляющих последовательностей ESC/POS (инициализация `ESC @`,
 * выравнивание `ESC a`, жирный шрифт `ESC E`, автоотрезка `GS V`) для прямой печати через USB/Bluetooth/Serial.
 */
internal class EscPosReceiptRenderer {

    /**
     * Формирует байтовый массив управляющих команд ESC/POS для печати фискального чека.
     *
     * @param receipt данные запроса чека [ReceiptRequest].
     * @param doc снимки фискального документа [FiscalDocumentSnapshot] (номер документа, ФПД).
     * @param kkm данные ККМ [KkmInfo] (регистрационный номер, ИНН).
     * @param paperWidthMm ширина чековой ленты в мм (58 мм или 80 мм).
     * @return байтовый массив (`ByteArray`) готов к отправке в порт термопринтера.
     */
    fun renderReceiptBytes(
        receipt: ReceiptRequest,
        doc: FiscalDocumentSnapshot,
        kkm: KkmInfo,
        paperWidthMm: Int = 80
    ): ByteArray {
        val bytes = mutableListOf<Byte>()

        // Initialize printer: ESC @
        bytes.addAll(listOf(0x1B.toByte(), 0x40.toByte()))

        // Center alignment: ESC a 1
        bytes.addAll(listOf(0x1B.toByte(), 0x61.toByte(), 0x01.toByte()))

        // Bold title
        bytes.addAll(listOf(0x1B.toByte(), 0x45.toByte(), 0x01.toByte()))
        bytes.addAll(encodeText("SUPERKASSA FISCAL TICKET\n"))
        bytes.addAll(listOf(0x1B.toByte(), 0x45.toByte(), 0x00.toByte()))

        bytes.addAll(encodeText("KKM: ${kkm.registrationNumber ?: "N/A"}\n"))
        bytes.addAll(encodeText("Doc #${doc.docNo ?: 0} | FPD: ${doc.fiscalSign ?: "N/A"}\n"))
        bytes.addAll(encodeText("--------------------------------\n"))

        // Left align
        bytes.addAll(listOf(0x1B.toByte(), 0x61.toByte(), 0x00.toByte()))

        receipt.items.forEach { item ->
            bytes.addAll(encodeText("${item.name}\n"))
            val qtyFormatted = item.quantity.toDouble() / 1000.0
            val qtyPrice = "  $qtyFormatted x ${item.price} = ${item.sum}\n"
            bytes.addAll(encodeText(qtyPrice))
        }

        bytes.addAll(encodeText("--------------------------------\n"))
        bytes.addAll(listOf(0x1B.toByte(), 0x45.toByte(), 0x01.toByte()))
        bytes.addAll(encodeText("TOTAL: ${receipt.total}\n"))
        bytes.addAll(listOf(0x1B.toByte(), 0x45.toByte(), 0x00.toByte()))
        bytes.addAll(encodeText("--------------------------------\n\n"))

        // Feed & Cut: GS V 66 0
        bytes.addAll(listOf(0x1D.toByte(), 0x56.toByte(), 0x42.toByte(), 0x00.toByte()))

        return bytes.toByteArray()
    }

    private fun encodeText(text: String): List<Byte> {
        return text.encodeToByteArray().toList()
    }
}
