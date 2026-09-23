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

    /**
     * Печатная форма документа журнала по одному его идентификатору.
     *
     * Вид формы определяется по журналу: идентификатор смены или её документа
     * открытия и закрытия даёт форму смены, чек, внесение, изъятие и отчёт —
     * форму документа.
     *
     * @param kkmId ID ККМ.
     * @param documentId ID документа, смены или документа смены.
     * @param pin ПИН-код кассира или администратора.
     * @param layout ширина ленты; не задана — та, что настроена у кассы.
     * @return HTML строка.
     */
    fun getDocumentPrintHtml(kkmId: String, documentId: String, pin: String, layout: ReceiptLayoutType? = null): String

    /** Та же форма, что [getDocumentPrintHtml], в PDF. */
    fun getDocumentPrintPdf(kkmId: String, documentId: String, pin: String, layout: ReceiptLayoutType? = null): ByteArray

    /** Та же форма, что [getDocumentPrintHtml], в PNG. */
    fun getDocumentPrintPng(kkmId: String, documentId: String, pin: String, layout: ReceiptLayoutType? = null): ByteArray

    /**
     * Печатная форма документа по пакету протокола, например присланному кабинетом.
     *
     * Документ мог быть пробит на другой кассе: реквизиты (регистрационный номер КГД,
     * БИН, адрес) берутся из пакета, оформление — у кассы [kkmId].
     *
     * @param kkmId ID ККМ, которой рисуется документ: её оформление и её права.
     * @param pin ПИН-код кассира или администратора этой кассы.
     * @param packet тело пакета: JSON-объект с полями `request` и `response` по схеме CPCR.
     * @param layout ширина ленты; не задана — та, что настроена у кассы.
     * @return HTML строка.
     */
    fun getProtocolPrintHtml(kkmId: String, pin: String, packet: String, layout: ReceiptLayoutType? = null): String

    /** Та же форма, что [getProtocolPrintHtml], в PDF. */
    fun getProtocolPrintPdf(kkmId: String, pin: String, packet: String, layout: ReceiptLayoutType? = null): ByteArray

    /** Та же форма, что [getProtocolPrintHtml], в PNG. */
    fun getProtocolPrintPng(kkmId: String, pin: String, packet: String, layout: ReceiptLayoutType? = null): ByteArray
}
