package io.github.texport.superkassa.embedded.impl.document

import io.github.texport.superkassa.core.domain.api.port.integration.DocumentConvertPort
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/**
 * PDF, PNG и ESC/POS из печатной формы на JVM без браузера.
 *
 * Лист PDF — сама лента: форма сначала рисуется на заведомо длинном листе,
 * по нему измеряется, где кончается, и рисуется второй раз на лист ровно
 * такой высоты. PNG и ESC/POS — растр этого же PDF, поэтому все три вида
 * одного документа совпадают.
 */
internal class HtmlDocuments : DocumentConvertPort {

    override fun htmlToPdf(html: String): ByteArray {
        val paper = PrintForm.paperWidthMm(html)
        val probe = PdfPages.render(html, paper, PROBE_HEIGHT_MM)
        return PdfPages.render(html, paper, contentHeightMm(probe) + BOTTOM_MARGIN_MM)
    }

    override fun htmlToImage(html: String): ByteArray = Loader.loadPDF(htmlToPdf(html)).use { document ->
        png(PDFRenderer(document).renderImageWithDPI(0, SCREEN_DPI, ImageType.RGB))
    }

    override fun htmlToEscPos(html: String, paperWidthMm: Int): ByteArray =
        Loader.loadPDF(htmlToPdf(html)).use { document ->
            val dots = EscPosRaster.printableDots(paperWidthMm)
            val dpi = dots * POINTS_PER_INCH / document.getPage(0).mediaBox.width
            val image = PDFRenderer(document).renderImageWithDPI(0, dpi, ImageType.GRAY)
            EscPosRaster.encode(minOf(dots, image.width), image.height) { x, y -> image.luminance(x, y) < INK }
        }

    /** Где кончается форма на пробном листе, в миллиметрах от верха первого листа. */
    private fun contentHeightMm(probe: ByteArray): Double = Loader.loadPDF(probe).use { document ->
        val pages = document.numberOfPages
        val lastRow = lastInkedRow(document, pages - 1)
        val pageHeightPt = document.getPage(0).mediaBox.height
        val heightPt = (pages - 1) * pageHeightPt + lastRow * POINTS_PER_INCH / MEASURE_DPI
        heightPt * MM_PER_INCH / POINTS_PER_INCH
    }

    private fun lastInkedRow(document: PDDocument, pageIndex: Int): Int {
        val image = PDFRenderer(document).renderImageWithDPI(pageIndex, MEASURE_DPI, ImageType.GRAY)
        for (y in image.height - 1 downTo 0) {
            if ((0 until image.width).any { x -> image.luminance(x, y) < PAPER }) return y + 1
        }
        return 0
    }

    private fun BufferedImage.luminance(x: Int, y: Int): Int = getRGB(x, y) and BYTE

    private fun png(image: BufferedImage): ByteArray = ByteArrayOutputStream().use { out ->
        check(ImageIO.write(image, "PNG", out)) { "No PNG writer available" }
        out.toByteArray()
    }

    private companion object {
        /** Пробный лист: длиннее любого Z-отчёта; длиннее — форма продолжится на следующем. */
        const val PROBE_HEIGHT_MM = 2000.0
        const val BOTTOM_MARGIN_MM = 2.0
        const val MEASURE_DPI = 48f
        const val SCREEN_DPI = 216f
        const val POINTS_PER_INCH = 72f
        const val MM_PER_INCH = 25.4
        const val BYTE = 0xFF
        const val PAPER = 250
        const val INK = 128
    }
}
