package io.github.texport.superkassa.core.presentation.api

import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptLayoutType
import io.github.texport.superkassa.core.presentation.api.model.receipt.PrintDocumentType

/**
 * Презентационное API для операций печати и генерации печатных форм документов/отчетов.
 */
interface PrintApi {

    /**
     * Сгенерировать HTML-представление конкретного чека по ID документа.
     *
     * @param kkmId ID ККМ.
     * @param documentId ID фискального документа.
     * @param pin ПИН-код кассира/администратора.
     * @param layout Шаблон визуализации чека.
     * @return HTML строка с контентом чека.
     */
    fun getReceiptHtml(
        kkmId: String,
        documentId: String,
        pin: String,
        layout: ReceiptLayoutType? = null
    ): String

    /**
     * Сгенерировать HTML-представление печатного документа (чека, отчета смены и др.).
     *
     * @param kkmId ID ККМ.
     * @param type Тип печатного документа.
     * @param documentId ID документа (для чеков).
     * @param shiftId ID смены (для отчетов).
     * @param pin ПИН-код пользователя.
     * @param layout Шаблон визуализации.
     * @return HTML строка.
     */
    fun getPrintHtml(
        kkmId: String,
        type: PrintDocumentType,
        documentId: String?,
        shiftId: String?,
        pin: String,
        layout: ReceiptLayoutType? = null
    ): String

    /**
     * Сгенерировать PDF-документ (в виде байтового массива) для печати/скачивания.
     *
     * @param kkmId ID ККМ.
     * @param type Тип печатного документа.
     * @param documentId ID документа.
     * @param shiftId ID смены.
     * @param pin ПИН-код пользователя.
     * @param layout Шаблон визуализации.
     * @return PDF файл в виде ByteArray.
     */
    fun getPrintPdf(
        kkmId: String,
        type: PrintDocumentType,
        documentId: String?,
        shiftId: String?,
        pin: String,
        layout: ReceiptLayoutType? = null
    ): ByteArray

    /**
     * Сгенерировать PNG-изображение (в виде байтового массива) для печати/отображения.
     *
     * @param kkmId ID ККМ.
     * @param type Тип печатного документа.
     * @param documentId ID документа.
     * @param shiftId ID смены.
     * @param pin ПИН-код пользователя.
     * @param layout Шаблон визуализации.
     * @return PNG файл в виде ByteArray.
     */
    fun getPrintPng(
        kkmId: String,
        type: PrintDocumentType,
        documentId: String?,
        shiftId: String?,
        pin: String,
        layout: ReceiptLayoutType? = null
    ): ByteArray
}
