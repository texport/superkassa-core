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
 * Шрифты — DejaVu из самой сборки, а не из системы: в них есть все буквы
 * казахского алфавита и знак тенге, и чек выглядит одинаково на любой
 * машине. Семейства, названные в стилях формы, отображаются на них же.
 */
internal object PdfPages {
    private const val FONTS = "/net/sf/jasperreports/fonts/dejavu/"
    private const val BOLD = 700
    private const val REGULAR = 400
    private val SANS_FAMILIES = listOf("Inter", "Roboto", "DejaVu Sans", "sans-serif", "Arial")
    private val MONO_FAMILIES = listOf("DejaVu Sans Mono", "Courier New", "monospace")

    init {
        XRLog.setLoggingEnabled(false)
    }

    /** Рисует страницу заданного размера; форма длиннее листа переходит на следующие листы. */
    fun render(html: String, paperWidthMm: Double, pageHeightMm: Double): ByteArray {
        val page = PrintForm.printable(html, paperWidthMm, pageHeightMm)
        val document = W3CDom().fromJsoup(Jsoup.parse(page))
        val out = ByteArrayOutputStream()
        PdfRendererBuilder().apply {
            useFastMode()
            withW3cDocument(document, null)
            SANS_FAMILIES.forEach { family ->
                font("DejaVuSans.ttf", family, REGULAR)
                font("DejaVuSans-Bold.ttf", family, BOLD)
            }
            MONO_FAMILIES.forEach { family ->
                font("DejaVuSansMono.ttf", family, REGULAR)
                font("DejaVuSansMono-Bold.ttf", family, BOLD)
            }
            toStream(out)
        }.run()
        return out.toByteArray()
    }

    private fun PdfRendererBuilder.font(file: String, family: String, weight: Int) {
        useFont(
            { checkNotNull(PdfPages::class.java.getResourceAsStream(FONTS + file)) { "Font $file is missing" } },
            family,
            weight,
            FontStyle.NORMAL,
            true
        )
    }
}
