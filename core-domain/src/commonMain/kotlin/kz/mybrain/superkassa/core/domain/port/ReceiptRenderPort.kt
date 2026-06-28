package kz.mybrain.superkassa.core.domain.port

import kz.mybrain.superkassa.core.domain.model.kkm.FiscalDocumentSnapshot
import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptBranding
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptLayoutType
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptRequest
import kz.mybrain.superkassa.core.domain.model.shift.ShiftInfo

/**
 * Порт рендеринга чеков и отчётов ККМ в формат HTML.
 * Сгенерированная HTML-версия используется для отображения, печати, генерации PDF/изображений и доставки клиентам.
 */
@Suppress("unused")
interface ReceiptRenderPort {

    /**
     * Рендерит торговый чек (продажа, покупка, возврат) в HTML-представление.
     *
     * @param receipt данные чека (позиции, виды оплаты, налоги и итоги).
     * @param doc фискальный документ (номер документа, фискальный признак, дата).
     * @param kkm информация о ККМ (регистрационный номер, реквизиты налогоплательщика).
     * @param layoutType тип разметки и оформления шаблона чека.
     * @return HTML-строка с визуализацией чека.
     */
    fun renderHtml(
        receipt: ReceiptRequest,
        doc: FiscalDocumentSnapshot,
        kkm: KkmInfo,
        layoutType: ReceiptLayoutType? = null
    ): String

    /**
     * Рендерит сменный X-отчёт (сменный отчёт без гашения) в HTML.
     *
     * @param shift текущая смена ККМ.
     * @param counters фискальные счётчики за смену (суммы продаж, возвратов, типы оплат).
     * @param kkm информация о ККМ.
     * @param ofdStatus статус отправки данных в ОФД.
     * @param layoutType тип разметки шаблона.
     * @return HTML-строка с визуализацией X-отчёта.
     */
    fun renderXReportHtml(
        shift: ShiftInfo,
        counters: Map<String, Long>,
        kkm: KkmInfo,
        ofdStatus: String?,
        layoutType: ReceiptLayoutType? = null
    ): String

    /**
     * Рендерит отчёт об открытии смены в HTML.
     *
     * @param shift открываемая смена ККМ.
     * @param kkm информация о ККМ.
     * @param ofdStatus статус отправки данных в ОФД.
     * @param docNo фискальный номер документа открытия смены.
     * @param layoutType тип разметки шаблона.
     * @return HTML-строка с визуализацией документа открытия смены.
     */
    fun renderOpenShiftHtml(
        shift: ShiftInfo,
        kkm: KkmInfo,
        ofdStatus: String?,
        docNo: String? = null,
        layoutType: ReceiptLayoutType? = null
    ): String

    /**
     * Рендерит Z-отчёт (отчёт о закрытии смены с гашением) в HTML.
     *
     * @param shift закрываемая смена ККМ.
     * @param counters итоговые счётчики за смену на момент её закрытия.
     * @param kkm информация о ККМ.
     * @param ofdStatus статус отправки данных в ОФД.
     * @param docNo фискальный номер документа закрытия смены.
     * @param layoutType тип разметки шаблона.
     * @return HTML-строка с визуализацией Z-отчёта.
     */
    fun renderCloseShiftHtml(
        shift: ShiftInfo,
        counters: Map<String, Long>,
        kkm: KkmInfo,
        ofdStatus: String?,
        docNo: String? = null,
        layoutType: ReceiptLayoutType? = null
    ): String

    /**
     * Рендерит нефискальный документ операции с наличными (внесение или изъятие средств) в HTML.
     *
     * @param doc фискальный документ, представляющий кассовую операцию.
     * @param kkm информация о ККМ.
     * @param layoutType тип разметки шаблона.
     * @return HTML-строка с визуализацией кассового ордера.
     */
    fun renderCashOperationHtml(
        doc: FiscalDocumentSnapshot,
        kkm: KkmInfo,
        layoutType: ReceiptLayoutType? = null
    ): String

    /**
     * Рендерит демонстрационный шаблон предпросмотра чека с настройками брендирования.
     *
     * @param branding параметры брендирования чека (логотип, заголовки, футер).
     * @param layoutType тип разметки шаблона.
     * @return HTML-строка демонстрационного чека.
     */
    fun renderPreviewHtml(
        branding: ReceiptBranding,
        layoutType: ReceiptLayoutType? = null
    ): String
}
