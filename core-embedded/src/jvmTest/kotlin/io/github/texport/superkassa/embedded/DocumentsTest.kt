package io.github.texport.superkassa.embedded

import io.github.texport.superkassa.core.presentation.api.model.receipt.PrintDocumentType
import io.github.texport.superkassa.embedded.TestCashRegister.ADMIN_PIN
import io.github.texport.superkassa.embedded.TestCashRegister.CASHIER_PIN
import io.github.texport.superkassa.embedded.TestCashRegister.KKM_ID
import io.github.texport.superkassa.embedded.impl.document.HtmlDocuments
import org.apache.pdfbox.Loader
import java.io.File
import javax.imageio.ImageIO
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Документы кассы без браузера: PDF — одна страница по длине ленты,
 * PNG читается, ESC/POS начинается сбросом принтера и несёт растр.
 *
 * Образы складываются в `build/documents` — их можно открыть и посмотреть глазами.
 */
class DocumentsTest {
    private val dir: File = createTempDirectory("kassa-docs-").toFile()
    private val out = File("build/documents").apply { mkdirs() }

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    @Test
    fun `чек и X-отчёт рисуются в PDF, PNG и ESC-POS`() {
        TestCashRegister.open(dir).use { kassa ->
            TestCashRegister.registerKkm(kassa)
            kassa.api.openShift(KKM_ID, ADMIN_PIN)
            val receipt = kassa.api.createSellReceipt(KKM_ID, CASHIER_PIN, TestCashRegister.sale("sale-1"))
            val html = kassa.print.getPrintHtml(KKM_ID, PrintDocumentType.DOCUMENT, receipt.documentId, null, CASHIER_PIN)

            val pdf = kassa.print.getPrintPdf(KKM_ID, PrintDocumentType.DOCUMENT, receipt.documentId, null, CASHIER_PIN)
            val png = kassa.print.getPrintPng(KKM_ID, PrintDocumentType.DOCUMENT, receipt.documentId, null, CASHIER_PIN)
            val escPos = HtmlDocuments().htmlToEscPos(html, 80)
            File(out, "receipt.pdf").writeBytes(pdf)
            File(out, "receipt.png").writeBytes(png)

            Loader.loadPDF(pdf).use { document ->
                assertEquals(1, document.numberOfPages)
                val page = document.getPage(0).mediaBox
                assertTrue(page.height > page.width, "the page is the tape: ${page.width} x ${page.height}")
            }
            assertTrue(ImageIO.read(png.inputStream()).height > 0)
            assertContentEquals(byteArrayOf(0x1B, '@'.code.toByte()), escPos.copyOfRange(0, 2))
            assertContentEquals(byteArrayOf(0x1D, 'v'.code.toByte(), '0'.code.toByte()), escPos.copyOfRange(2, 5))
        }
    }

    @Test
    fun `X-отчёт рисуется в PNG`() {
        TestCashRegister.open(dir).use { kassa ->
            TestCashRegister.registerKkm(kassa)
            val shift = kassa.api.openShift(KKM_ID, ADMIN_PIN)
            kassa.api.createSellReceipt(KKM_ID, CASHIER_PIN, TestCashRegister.sale("sale-1"))

            val png = kassa.print.getPrintPng(KKM_ID, PrintDocumentType.X_REPORT, null, shift.id, CASHIER_PIN)
            File(out, "x-report.png").writeBytes(png)

            assertTrue(ImageIO.read(png.inputStream()).height > 0)
        }
    }
}
