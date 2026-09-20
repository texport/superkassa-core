package io.github.texport.superkassa.receiptrenderer.impl

import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest

/**
 * Векторный SVG рендерер чеков для веб-интерфейсов, мобильных экранов и печати высокого качества.
 *
 * Генерирует чистое масштабируемое векторное изображение в формате XML SVG без использования Canvas
 * или визуального браузерного движка HTML.
 */
internal class SvgReceiptRenderer {

    /**
     * Формирует векторный документ SVG для визуального отображения чека.
     *
     * @param receipt данные запроса чека [ReceiptRequest].
     * @param doc снимок фискального документа [FiscalDocumentSnapshot].
     * @param kkm данные ККМ [KkmInfo].
     * @param widthPx ширина векторного документа SVG в пикселях.
     * @return валидная XML-строка `<svg...></svg>`.
     */
    fun renderReceiptSvg(
        receipt: ReceiptRequest,
        doc: FiscalDocumentSnapshot,
        kkm: KkmInfo,
        widthPx: Int = 384
    ): String {
        val sb = StringBuilder()
        val lineHeight = 20
        var currentY = 30

        sb.append(
            """<svg xmlns="http://www.w3.org/2000/svg" width="$widthPx" height="600" viewBox="0 0 $widthPx 600">"""
        )
        sb.append("""<rect width="100%" height="100%" fill="#FAFAFA"/>""")
        sb.append("""<style>text { font-family: monospace; font-size: 14px; fill: #111; }</style>""")

        fun addLine(text: String, bold: Boolean = false) {
            val weight = if (bold) "font-weight=\"bold\"" else ""
            sb.append("""<text x="20" y="$currentY" $weight>$text</text>""")
            currentY += lineHeight
        }

        addLine("SUPERKASSA FISCAL TICKET", bold = true)
        addLine("KKM: ${kkm.registrationNumber ?: "N/A"}")
        addLine("Doc #${doc.docNo ?: 0} | FPD: ${doc.fiscalSign ?: "N/A"}")
        addLine("--------------------------------")

        receipt.items.forEach { item ->
            addLine(item.name)
            val qtyFormatted = item.quantity.toDouble() / 1000.0
            addLine("  $qtyFormatted x ${item.price} = ${item.sum}")
        }

        addLine("--------------------------------")
        addLine("TOTAL: ${receipt.total}", bold = true)
        addLine("--------------------------------")

        sb.append("</svg>")
        return sb.toString()
    }
}
