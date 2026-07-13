package io.github.texport.superkassa.core.presentation.impl

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptLayoutType
import io.github.texport.superkassa.core.domain.api.model.report.PrintDocumentType
import io.github.texport.superkassa.core.domain.api.port.integration.DocumentConvertPort
import io.github.texport.superkassa.core.domain.impl.usecase.print.GetPrintHtmlUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.print.GetPrintPdfUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.print.GetReceiptHtmlUseCase
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptLayoutType as PresentationReceiptLayoutType
import io.github.texport.superkassa.core.presentation.api.model.receipt.PrintDocumentType as PresentationPrintDocumentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PrintApiImplTest {

    private val getReceiptHtmlUseCase = mockk<GetReceiptHtmlUseCase>()
    private val getPrintHtmlUseCase = mockk<GetPrintHtmlUseCase>()
    private val getPrintPdfUseCase = mockk<GetPrintPdfUseCase>()
    private val documentConvertPort = mockk<DocumentConvertPort>()

    private val printApi = PrintApiImpl(
        getReceiptHtmlUseCase = getReceiptHtmlUseCase,
        getPrintHtmlUseCase = getPrintHtmlUseCase,
        getPrintPdfUseCase = getPrintPdfUseCase,
        documentConvertPort = documentConvertPort
    )

    @Test
    fun testGetReceiptHtml() {
        every { getReceiptHtmlUseCase.execute("kkm-1", "doc-1", "1234", ReceiptLayoutType.TAPE_80MM) } returns "<html>Receipt</html>"

        val result = printApi.getReceiptHtml("kkm-1", "doc-1", "1234", PresentationReceiptLayoutType.TAPE_80MM)
        assertEquals("<html>Receipt</html>", result)
        verify { getReceiptHtmlUseCase.execute("kkm-1", "doc-1", "1234", ReceiptLayoutType.TAPE_80MM) }
    }

    @Test
    fun testGetPrintHtml() {
        every { getPrintHtmlUseCase.execute("kkm-1", PrintDocumentType.DOCUMENT, "doc-1", "shift-1", "1234", ReceiptLayoutType.TAPE_80MM) } returns "<html>Print</html>"

        val result = printApi.getPrintHtml("kkm-1", PresentationPrintDocumentType.DOCUMENT, "doc-1", "shift-1", "1234", PresentationReceiptLayoutType.TAPE_80MM)
        assertEquals("<html>Print</html>", result)
        verify { getPrintHtmlUseCase.execute("kkm-1", PrintDocumentType.DOCUMENT, "doc-1", "shift-1", "1234", ReceiptLayoutType.TAPE_80MM) }
    }

    @Test
    fun testGetPrintPdf() {
        val bytes = byteArrayOf(1, 2, 3)
        every { getPrintPdfUseCase.execute("kkm-1", PrintDocumentType.DOCUMENT, "doc-1", "shift-1", "1234", ReceiptLayoutType.TAPE_80MM) } returns bytes

        val result = printApi.getPrintPdf("kkm-1", PresentationPrintDocumentType.DOCUMENT, "doc-1", "shift-1", "1234", PresentationReceiptLayoutType.TAPE_80MM)
        assertTrue(bytes.contentEquals(result))
        verify { getPrintPdfUseCase.execute("kkm-1", PrintDocumentType.DOCUMENT, "doc-1", "shift-1", "1234", ReceiptLayoutType.TAPE_80MM) }
    }

    @Test
    fun testGetPrintPng() {
        val bytes = byteArrayOf(4, 5, 6)
        every { getPrintHtmlUseCase.execute("kkm-1", PrintDocumentType.DOCUMENT, "doc-1", "shift-1", "1234", ReceiptLayoutType.TAPE_80MM) } returns "<html>Print</html>"
        every { documentConvertPort.htmlToImage("<html>Print</html>") } returns bytes

        val result = printApi.getPrintPng("kkm-1", PresentationPrintDocumentType.DOCUMENT, "doc-1", "shift-1", "1234", PresentationReceiptLayoutType.TAPE_80MM)
        assertTrue(bytes.contentEquals(result))
        verify { documentConvertPort.htmlToImage("<html>Print</html>") }
    }

    @Test
    fun testDefaultArguments() {
        every { getReceiptHtmlUseCase.execute("kkm-1", "doc-1", "1234", null) } returns "<html>Receipt</html>"
        val result = printApi.getReceiptHtml("kkm-1", "doc-1", "1234")
        assertEquals("<html>Receipt</html>", result)

        every { getPrintHtmlUseCase.execute("kkm-1", PrintDocumentType.DOCUMENT, null, null, "1234", null) } returns "<html>Print</html>"
        val printHtml = printApi.getPrintHtml("kkm-1", PresentationPrintDocumentType.DOCUMENT, null, null, "1234")
        assertEquals("<html>Print</html>", printHtml)

        val bytes = byteArrayOf(1, 2, 3)
        every { getPrintPdfUseCase.execute("kkm-1", PrintDocumentType.DOCUMENT, null, null, "1234", null) } returns bytes
        val printPdf = printApi.getPrintPdf("kkm-1", PresentationPrintDocumentType.DOCUMENT, null, null, "1234")
        assertTrue(bytes.contentEquals(printPdf))

        val pngBytes = byteArrayOf(4, 5, 6)
        every { getPrintHtmlUseCase.execute("kkm-1", PrintDocumentType.DOCUMENT, null, null, "1234", null) } returns "<html>Print</html>"
        every { documentConvertPort.htmlToImage("<html>Print</html>") } returns pngBytes
        val printPng = printApi.getPrintPng("kkm-1", PresentationPrintDocumentType.DOCUMENT, null, null, "1234")
        assertTrue(pngBytes.contentEquals(printPng))
    }
}
