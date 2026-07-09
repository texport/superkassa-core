package io.github.texport.superkassa.core.presentation.impl

import io.github.texport.superkassa.core.domain.model.receipt.ReceiptLayoutType
import io.github.texport.superkassa.core.domain.model.report.PrintDocumentType
import io.github.texport.superkassa.core.domain.usecase.print.GetPrintHtmlUseCase
import io.github.texport.superkassa.core.domain.usecase.print.GetPrintPdfUseCase
import io.github.texport.superkassa.core.domain.usecase.print.GetReceiptHtmlUseCase
import io.github.texport.superkassa.core.presentation.api.PrintApi

/**
 * Реализация API операций печати, делегирующая вызовы юзкейсам доменного слоя.
 */
class PrintApiImpl(
    private val getReceiptHtmlUseCase: GetReceiptHtmlUseCase,
    private val getPrintHtmlUseCase: GetPrintHtmlUseCase,
    private val getPrintPdfUseCase: GetPrintPdfUseCase
) : PrintApi {

    override fun getReceiptHtml(
        kkmId: String,
        documentId: String,
        pin: String,
        layout: ReceiptLayoutType?
    ): String = getReceiptHtmlUseCase.execute(kkmId, documentId, pin, layout)

    override fun getPrintHtml(
        kkmId: String,
        type: PrintDocumentType,
        documentId: String?,
        shiftId: String?,
        pin: String,
        layout: ReceiptLayoutType?
    ): String = getPrintHtmlUseCase.execute(kkmId, type, documentId, shiftId, pin, layout)

    override fun getPrintPdf(
        kkmId: String,
        type: PrintDocumentType,
        documentId: String?,
        shiftId: String?,
        pin: String,
        layout: ReceiptLayoutType?
    ): ByteArray = getPrintPdfUseCase.execute(kkmId, type, documentId, shiftId, pin, layout)
}
