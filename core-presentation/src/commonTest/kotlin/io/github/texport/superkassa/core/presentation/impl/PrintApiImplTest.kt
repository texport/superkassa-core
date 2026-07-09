package io.github.texport.superkassa.core.presentation.impl

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.github.texport.superkassa.core.domain.model.receipt.ReceiptLayoutType
import io.github.texport.superkassa.core.domain.model.report.PrintDocumentType
import io.github.texport.superkassa.core.domain.usecase.print.GetPrintHtmlUseCase
import io.github.texport.superkassa.core.domain.usecase.print.GetPrintPdfUseCase
import io.github.texport.superkassa.core.domain.usecase.print.GetReceiptHtmlUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrintApiImplTest {

    private val getReceiptHtmlUseCase = mockk<GetReceiptHtmlUseCase>()
    private val getPrintHtmlUseCase = mockk<GetPrintHtmlUseCase>()
    private val getPrintPdfUseCase = mockk<GetPrintPdfUseCase>()

    private val printApi = PrintApiImpl(
        getReceiptHtmlUseCase = getReceiptHtmlUseCase,
        getPrintHtmlUseCase = getPrintHtmlUseCase,
        getPrintPdfUseCase = getPrintPdfUseCase
    )

    @Test
    fun testGetReceiptHtml() {
        every { getReceiptHtmlUseCase.execute("kkm-1", "doc-1", "1234", ReceiptLayoutType.TAPE_80MM) } returns "<html>Receipt</html>"

        val result = printApi.getReceiptHtml("kkm-1", "doc-1", "1234", ReceiptLayoutType.TAPE_80MM)
        assertEquals("<html>Receipt</html>", result)
        verify { getReceiptHtmlUseCase.execute("kkm-1", "doc-1", "1234", ReceiptLayoutType.TAPE_80MM) }
    }

    @Test
    fun testGetPrintHtml() {
        every { getPrintHtmlUseCase.execute("kkm-1", PrintDocumentType.DOCUMENT, "doc-1", "shift-1", "1234", ReceiptLayoutType.TAPE_80MM) } returns "<html>Print</html>"

        val result = printApi.getPrintHtml("kkm-1", PrintDocumentType.DOCUMENT, "doc-1", "shift-1", "1234", ReceiptLayoutType.TAPE_80MM)
        assertEquals("<html>Print</html>", result)
        verify { getPrintHtmlUseCase.execute("kkm-1", PrintDocumentType.DOCUMENT, "doc-1", "shift-1", "1234", ReceiptLayoutType.TAPE_80MM) }
    }

    @Test
    fun testGetPrintPdf() {
        val bytes = byteArrayOf(1, 2, 3)
        every { getPrintPdfUseCase.execute("kkm-1", PrintDocumentType.DOCUMENT, "doc-1", "shift-1", "1234", ReceiptLayoutType.TAPE_80MM) } returns bytes

        val result = printApi.getPrintPdf("kkm-1", PrintDocumentType.DOCUMENT, "doc-1", "shift-1", "1234", ReceiptLayoutType.TAPE_80MM)
        assertTrue(bytes.contentEquals(result))
        verify { getPrintPdfUseCase.execute("kkm-1", PrintDocumentType.DOCUMENT, "doc-1", "shift-1", "1234", ReceiptLayoutType.TAPE_80MM) }
    }
}
