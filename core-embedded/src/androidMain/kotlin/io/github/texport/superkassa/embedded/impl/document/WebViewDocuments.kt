package io.github.texport.superkassa.embedded.impl.document

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import io.github.texport.superkassa.core.domain.api.port.integration.DocumentConvertPort
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

/**
 * PDF, PNG и ESC/POS на Android через системный WebView.
 *
 * Форма рисуется в растр нужной ширины, и уже растр кладётся на лист PDF,
 * сжимается в PNG или кодируется для термопринтера. Вызывать не из главного
 * потока: WebView работает в нём, и вызов из него же ждал бы сам себя.
 */
internal class WebViewDocuments(private val context: Context) : DocumentConvertPort {

    override fun htmlToPdf(html: String): ByteArray {
        val paper = PrintForm.paperWidthMm(html)
        val bitmap = snapshot(html, paper, (paper * PrintForm.PX_PER_MM * PDF_SCALE).roundToInt())
        val pageWidth = (paper * POINTS_PER_MM).roundToInt()
        val pageHeight = (bitmap.height.toDouble() * pageWidth / bitmap.width).roundToInt()
        val document = PdfDocument()
        try {
            val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
            page.canvas.drawBitmap(bitmap, null, Rect(0, 0, pageWidth, pageHeight), null)
            document.finishPage(page)
            return ByteArrayOutputStream().use { out ->
                document.writeTo(out)
                out.toByteArray()
            }
        } finally {
            document.close()
            bitmap.recycle()
        }
    }

    override fun htmlToImage(html: String): ByteArray {
        val paper = PrintForm.paperWidthMm(html)
        val bitmap = snapshot(html, paper, (paper * PrintForm.PX_PER_MM * SCREEN_SCALE).roundToInt())
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }

    override fun htmlToEscPos(html: String, paperWidthMm: Int): ByteArray {
        val dots = EscPosRaster.printableDots(paperWidthMm)
        val bitmap = snapshot(html, PrintForm.paperWidthMm(html), dots)
        return try {
            EscPosRaster.encode(bitmap.width, bitmap.height) { x, y -> luminance(bitmap.getPixel(x, y)) < INK }
        } finally {
            bitmap.recycle()
        }
    }

    private fun snapshot(html: String, paperWidthMm: Double, widthPx: Int): Bitmap =
        WebViewSnapshot.take(context, PrintForm.printable(html, paperWidthMm, null), paperWidthMm, widthPx)

    private fun luminance(color: Int): Int =
        (Color.red(color) * RED + Color.green(color) * GREEN + Color.blue(color) * BLUE).roundToInt()

    private companion object {
        const val PDF_SCALE = 3.0
        const val SCREEN_SCALE = 3.0
        const val POINTS_PER_MM = 72 / 25.4
        const val PNG_QUALITY = 100
        const val INK = 128
        const val RED = 0.299
        const val GREEN = 0.587
        const val BLUE = 0.114
    }
}
