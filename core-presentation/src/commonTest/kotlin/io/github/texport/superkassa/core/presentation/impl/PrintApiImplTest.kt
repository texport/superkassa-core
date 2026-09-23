package io.github.texport.superkassa.core.presentation.impl

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptLayoutType
import io.github.texport.superkassa.core.domain.api.model.report.PrintDocumentType
import io.github.texport.superkassa.core.domain.api.port.integration.DocumentConvertPort
import io.github.texport.superkassa.core.domain.impl.usecase.print.GetDocumentPrintHtmlUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.print.GetPrintHtmlUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.print.GetPrintPdfUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.print.GetReceiptHtmlUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.print.protocol.GetProtocolPrintHtmlUseCase
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
    private val getDocumentPrintHtmlUseCase = mockk<GetDocumentPrintHtmlUseCase>()
    private val getProtocolPrintHtmlUseCase = mockk<GetProtocolPrintHtmlUseCase>()

    private val printApi = PrintApiImpl(
        getReceiptHtmlUseCase = getReceiptHtmlUseCase,
        getPrintHtmlUseCase = getPrintHtmlUseCase,
        getPrintPdfUseCase = getPrintPdfUseCase,
        documentConvertPort = documentConvertPort,
        getDocumentPrintHtmlUseCase = getDocumentPrintHtmlUseCase,
        getProtocolPrintHtmlUseCase = getProtocolPrintHtmlUseCase
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

    @Test
    fun `печать по идентификатору документа отдаёт одну разметку в HTML, PDF и PNG`() {
        every { getDocumentPrintHtmlUseCase.execute("kkm-1", "doc-1", "1234", ReceiptLayoutType.TAPE_58MM) } returns "<html>Doc</html>"
        every { documentConvertPort.htmlToPdf("<html>Doc</html>") } returns byteArrayOf(1)
        every { documentConvertPort.htmlToImage("<html>Doc</html>") } returns byteArrayOf(2)
        val layout = PresentationReceiptLayoutType.TAPE_58MM

        assertEquals("<html>Doc</html>", printApi.getDocumentPrintHtml("kkm-1", "doc-1", "1234", layout))
        assertTrue(byteArrayOf(1).contentEquals(printApi.getDocumentPrintPdf("kkm-1", "doc-1", "1234", layout)))
        assertTrue(byteArrayOf(2).contentEquals(printApi.getDocumentPrintPng("kkm-1", "doc-1", "1234", layout)))
    }

    @Test
    fun `печать по пакету протокола отдаёт одну разметку в HTML, PDF и PNG`() {
        every { getProtocolPrintHtmlUseCase.execute("kkm-1", "1234", "{}", null) } returns "<html>Packet</html>"
        every { documentConvertPort.htmlToPdf("<html>Packet</html>") } returns byteArrayOf(3)
        every { documentConvertPort.htmlToImage("<html>Packet</html>") } returns byteArrayOf(4)

        assertEquals("<html>Packet</html>", printApi.getProtocolPrintHtml("kkm-1", "1234", "{}"))
        assertTrue(byteArrayOf(3).contentEquals(printApi.getProtocolPrintPdf("kkm-1", "1234", "{}")))
        assertTrue(byteArrayOf(4).contentEquals(printApi.getProtocolPrintPng("kkm-1", "1234", "{}")))
    }
}
