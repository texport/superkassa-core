package io.github.texport.superkassa.embedded.impl.print

import io.github.texport.superkassa.embedded.api.DocumentPrinter
import org.apache.pdfbox.Loader
import org.apache.pdfbox.printing.PDFPageable
import java.awt.print.PrinterJob
import javax.print.PrintServiceLookup

/**
 * Печать через Java Print Service: принтеры, установленные в системе.
 *
 * PDF печатается страницами PDFBox, поэтому драйвер принтера получает
 * картинку страницы и печатает её так же, как любой другой документ.
 */
internal class SystemPrinter : DocumentPrinter {
    override fun printerNames(): List<String> = PrintServiceLookup.lookupPrintServices(null, null).map { it.name }

    override fun printPdf(pdf: ByteArray, printerName: String?) {
        val service = if (printerName == null) {
            PrintServiceLookup.lookupDefaultPrintService() ?: error("No default printer in the system")
        } else {
            PrintServiceLookup.lookupPrintServices(null, null).firstOrNull { it.name == printerName }
                ?: error("Printer '$printerName' is not installed")
        }
        Loader.loadPDF(pdf).use { document ->
            val job = PrinterJob.getPrinterJob()
            job.printService = service
            job.setPageable(PDFPageable(document))
            job.print()
        }
    }
}
