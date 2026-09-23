package io.github.texport.superkassa.embedded.impl.print

import io.github.texport.superkassa.core.domain.api.port.integration.DocumentConvertPort
import io.github.texport.superkassa.embedded.api.DocumentPrinter

/**
 * Принтер там, где печати ещё нет: отказ с причиной, а не тишина.
 *
 * @param platform название платформы для причины отказа.
 */
internal class UnsupportedPrinter(private val platform: String) : DocumentPrinter {
    override fun printerNames(): List<String> = emptyList()

    override fun printPdf(pdf: ByteArray, printerName: String?): Unit =
        throw UnsupportedOperationException("Printing to system printers is not implemented on $platform yet")
}

/**
 * Рисование документов там, где его ещё нет.
 *
 * Отказывает, а не отдаёт HTML под видом PDF: сохранённый «документ»,
 * который ничем не открывается, хуже честного отказа.
 */
internal class UnsupportedDocuments(private val platform: String) : DocumentConvertPort {
    override fun htmlToPdf(html: String): ByteArray = refuse()

    override fun htmlToImage(html: String): ByteArray = refuse()

    override fun htmlToEscPos(html: String, paperWidthMm: Int): ByteArray = refuse()

    private fun refuse(): Nothing =
        throw UnsupportedOperationException("Document rendering is not implemented on $platform yet")
}
