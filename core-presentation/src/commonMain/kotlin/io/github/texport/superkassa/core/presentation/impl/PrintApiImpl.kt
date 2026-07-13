package io.github.texport.superkassa.core.presentation.impl

import io.github.texport.superkassa.core.domain.api.port.integration.DocumentConvertPort
import io.github.texport.superkassa.core.domain.impl.usecase.print.GetPrintHtmlUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.print.GetPrintPdfUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.print.GetReceiptHtmlUseCase
import io.github.texport.superkassa.core.presentation.api.PrintApi
import io.github.texport.superkassa.core.presentation.api.model.receipt.PrintDocumentType
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptLayoutType
import io.github.texport.superkassa.core.presentation.impl.mapper.ReceiptMapper

/**
 * Реализация API операций печати, делегирующая вызовы юзкейсам доменного слоя.
 */
class PrintApiImpl(
    private val getReceiptHtmlUseCase: GetReceiptHtmlUseCase,
    private val getPrintHtmlUseCase: GetPrintHtmlUseCase,
    private val getPrintPdfUseCase: GetPrintPdfUseCase,
    private val documentConvertPort: DocumentConvertPort
) : PrintApi {

    override fun getReceiptHtml(
        kkmId: String,
        documentId: String,
        pin: String,
        layout: ReceiptLayoutType?
    ): String = getReceiptHtmlUseCase.execute(kkmId, documentId, pin, layout?.let { ReceiptMapper.toDomain(it) })

    override fun getPrintHtml(
        kkmId: String,
        type: PrintDocumentType,
        documentId: String?,
        shiftId: String?,
        pin: String,
        layout: ReceiptLayoutType?
    ): String = getPrintHtmlUseCase.execute(kkmId, ReceiptMapper.toDomain(type), documentId, shiftId, pin, layout?.let { ReceiptMapper.toDomain(it) })

    override fun getPrintPdf(
        kkmId: String,
        type: PrintDocumentType,
        documentId: String?,
        shiftId: String?,
        pin: String,
        layout: ReceiptLayoutType?
    ): ByteArray = getPrintPdfUseCase.execute(kkmId, ReceiptMapper.toDomain(type), documentId, shiftId, pin, layout?.let { ReceiptMapper.toDomain(it) })

    override fun getPrintPng(
        kkmId: String,
        type: PrintDocumentType,
        documentId: String?,
        shiftId: String?,
        pin: String,
        layout: ReceiptLayoutType?
    ): ByteArray {
        val html = getPrintHtml(kkmId, type, documentId, shiftId, pin, layout)
        return documentConvertPort.htmlToImage(html)
    }
}
