package io.github.texport.superkassa.embedded

import io.github.texport.superkassa.core.presentation.api.model.receipt.PrintDocumentType
import io.github.texport.superkassa.embedded.TestCashRegister.ADMIN_PIN
import io.github.texport.superkassa.embedded.TestCashRegister.CASHIER_PIN
import io.github.texport.superkassa.embedded.TestCashRegister.KKM_ID
import org.apache.pdfbox.Loader
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Печать по одному идентификатору из журнала: приложение не выбирает вид
 * формы, а получает ту же форму, что и при явном выборе вида.
 */
class DocumentPrintByIdTest {
    private val dir: File = createTempDirectory("kassa-print-id-").toFile()

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    @Test
    fun `чек, открытие и закрытие смены рисуются по одному идентификатору`() {
        TestCashRegister.open(dir).use { kassa ->
            TestCashRegister.registerKkm(kassa)
            val opened = kassa.api.openShift(KKM_ID, ADMIN_PIN)
            val receipt = kassa.api.createSellReceipt(KKM_ID, CASHIER_PIN, TestCashRegister.sale("sale-1"))
            kassa.api.closeShift(KKM_ID, ADMIN_PIN)
            val shift = kassa.api.listShifts(KKM_ID, 1, 0, ADMIN_PIN).single()
            val print = kassa.print

            assertEquals(
                print.getPrintHtml(KKM_ID, PrintDocumentType.DOCUMENT, receipt.documentId, null, CASHIER_PIN),
                print.getDocumentPrintHtml(KKM_ID, receipt.documentId, CASHIER_PIN)
            )
            assertEquals(
                print.getPrintHtml(KKM_ID, PrintDocumentType.OPEN_SHIFT, null, opened.id, CASHIER_PIN),
                print.getDocumentPrintHtml(KKM_ID, requireNotNull(shift.openDocumentId), CASHIER_PIN)
            )
            val zReport = print.getPrintHtml(KKM_ID, PrintDocumentType.CLOSE_SHIFT, null, shift.id, CASHIER_PIN)
            assertEquals(zReport, print.getDocumentPrintHtml(KKM_ID, requireNotNull(shift.closeDocumentId), CASHIER_PIN))
            assertEquals(zReport, print.getDocumentPrintHtml(KKM_ID, shift.id, CASHIER_PIN))
        }
    }

    @Test
    fun `PDF по идентификатору — одна страница ленты`() {
        TestCashRegister.open(dir).use { kassa ->
            TestCashRegister.registerKkm(kassa)
            kassa.api.openShift(KKM_ID, ADMIN_PIN)
            val receipt = kassa.api.createSellReceipt(KKM_ID, CASHIER_PIN, TestCashRegister.sale("sale-1"))

            val pdf = kassa.print.getDocumentPrintPdf(KKM_ID, receipt.documentId, CASHIER_PIN)

            Loader.loadPDF(pdf).use { assertEquals(1, it.numberOfPages) }
        }
    }
}
