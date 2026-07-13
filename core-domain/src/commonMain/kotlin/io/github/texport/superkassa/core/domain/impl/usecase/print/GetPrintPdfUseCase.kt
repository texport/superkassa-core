package io.github.texport.superkassa.core.domain.impl.usecase.print

import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptLayoutType
import io.github.texport.superkassa.core.domain.api.model.report.PrintDocumentType
import io.github.texport.superkassa.core.domain.api.port.integration.DocumentConvertPort

/**
 * Сценарий (Use Case) получения печатной формы документа в формате PDF.
 *
 * Делегирует генерацию HTML-представления сценарию [GetPrintHtmlUseCase],
 * после чего конвертирует полученный HTML-код в бинарный поток PDF с помощью [DocumentConvertPort].
 *
 * @property getPrintHtmlUseCase Сценарий получения печатной формы документа в формате HTML.
 * @property documentConvertPort Порт конвертации документов (например, HTML в PDF).
 */
class GetPrintPdfUseCase(
    private val getPrintHtmlUseCase: GetPrintHtmlUseCase,
    private val documentConvertPort: DocumentConvertPort
) {
    /**
     * Выполняет сценарий генерации печатного PDF-документа.
     *
     * @param kkmId Идентификатор кассового аппарата (ККМ).
     * @param type Тип запрашиваемого печатного документа [PrintDocumentType].
     * @param documentId Идентификатор документа.
     * @param shiftId Идентификатор смены.
     * @param pin ПИН-код оператора для авторизации.
     * @param layout Шаблон/макет чека (опционально).
     * @return Массив байтов (ByteArray), представляющий сгенерированный PDF-файл.
     */
    fun execute(
        kkmId: String,
        type: PrintDocumentType,
        documentId: String?,
        shiftId: String?,
        pin: String,
        layout: ReceiptLayoutType? = null
    ): ByteArray {
        val html = getPrintHtmlUseCase.execute(kkmId, type, documentId, shiftId, pin, layout)
        return documentConvertPort.htmlToPdf(html)
    }
}
