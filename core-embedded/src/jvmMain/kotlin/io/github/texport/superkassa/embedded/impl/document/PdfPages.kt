package io.github.texport.superkassa.embedded.impl.document

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.openhtmltopdf.util.XRLog
import org.jsoup.Jsoup
import org.jsoup.helper.W3CDom
import java.io.ByteArrayOutputStream

/**
 * PDF из печатной формы движком openhtmltopdf, без браузера.
 *
 * Движок PDF встраивает только шрифты из сборки, поэтому каждое семейство
 * из стилей формы отображается на шрифты [PrintFonts].
 */
internal object PdfPages {
    init {
        XRLog.setLoggingEnabled(false)
    }

    /** Рисует страницу заданного размера; форма длиннее листа переходит на следующие листы. */
    fun render(html: String, paperWidthMm: Double, pageHeightMm: Double): ByteArray {
        val page = PrintForm.printable(html, paperWidthMm, pageHeightMm)
        val document = W3CDom().fromJsoup(Jsoup.parse(page))
        val out = ByteArrayOutputStream()
        PdfRendererBuilder().apply {
            withW3cDocument(document, null)
            PrintFonts.FAMILIES.forEach { family -> PrintFonts.FACES.forEach { face -> font(face, family) } }
            toStream(out)
        }.run()
        return out.toByteArray()
    }

    private fun PdfRendererBuilder.font(face: PrintFonts.Face, family: String) {
        useFont(
            {
                checkNotNull(PdfPages::class.java.getResourceAsStream(PrintFonts.DIR + face.file)) {
                    "Font ${face.file} is missing"
                }
            },
            family,
            face.weight,
            FontStyle.NORMAL,
            true
        )
    }
}
