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
 * Шрифт лежит в ресурсах модуля, а не берётся из системы: чек выглядит
 * одинаково на любой машине. Source Code Pro — под SIL OFL 1.1, лицензия
 * рядом с файлами; в нём есть все буквы казахского алфавита, знак тенге
 * и «№», и у него настоящее полужирное начертание для итогов. Все семейства,
 * названные в стилях формы, отображаются на него: движок PDF встраивает
 * только шрифты с контурами TrueType, а пропорционального шрифта с таким
 * покрытием и полужирным начертанием под свободной лицензией в сборке нет.
 */
internal object PdfPages {
    private const val FONTS = "/io/github/texport/superkassa/embedded/fonts/"
    private const val REGULAR = 400
    private const val BOLD = 700
    private val FAMILIES = listOf(
        "Inter", "Roboto", "DejaVu Sans", "sans-serif", "Arial",
        "DejaVu Sans Mono", "Courier New", "monospace"
    )

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
            FAMILIES.forEach { family ->
                font("SourceCodePro-Regular.ttf", family, REGULAR)
                font("SourceCodePro-Bold.ttf", family, BOLD)
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
