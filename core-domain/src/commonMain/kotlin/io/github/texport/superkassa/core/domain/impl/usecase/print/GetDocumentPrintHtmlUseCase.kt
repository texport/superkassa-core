package io.github.texport.superkassa.core.domain.impl.usecase.print

import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptLayoutType
import io.github.texport.superkassa.core.domain.api.model.report.PrintDocumentType
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort

/**
 * Печатная форма документа по одному его идентификатору.
 *
 * Приложение знает о документе только идентификатор из журнала: чек,
 * внесение, изъятие, отчёт или документ смены. Какую форму рисовать,
 * решается здесь, а не у вызывающего: идентификатор смены или её
 * документа открытия и закрытия рисуется формой смены, всё прочее —
 * формой документа.
 *
 * Права проверяет сама отрисовка [GetPrintHtmlUseCase]: выбор формы
 * ничего о кассе не раскрывает, а неверный пин отказывает там же,
 * где и для любой другой формы.
 *
 * @property storage журнал смен кассы.
 * @property getPrintHtml отрисовка формы выбранного вида.
 */
class GetDocumentPrintHtmlUseCase(
    private val storage: StoragePort,
    private val getPrintHtml: GetPrintHtmlUseCase
) {

    /**
     * Рисует документ по идентификатору.
     *
     * @param kkmId касса документа.
     * @param documentId идентификатор документа, смены или документа смены.
     * @param pin пин кассира или администратора.
     * @param layout ширина ленты; не задана — та, что настроена у кассы.
     * @return HTML печатной формы.
     */
    fun execute(kkmId: String, documentId: String, pin: String, layout: ReceiptLayoutType?): String {
        val shift = storage.listShifts(kkmId, RECENT_SHIFTS).firstOrNull {
            it.id == documentId || it.openDocumentId == documentId || it.closeDocumentId == documentId
        } ?: return getPrintHtml.execute(kkmId, PrintDocumentType.DOCUMENT, documentId, null, pin, layout)
        val closesShift = shift.closeDocumentId == documentId || shift.id == documentId
        val type = if (closesShift) PrintDocumentType.CLOSE_SHIFT else PrintDocumentType.OPEN_SHIFT
        return getPrintHtml.execute(kkmId, type, null, shift.id, pin, layout)
    }

    private companion object {
        /**
         * Сколько последних смен просматривается в поисках документа смены.
         *
         * Документ смены постарше этого окна рисуется формой документа:
         * у записей открытия и закрытия в журнале есть свой вид, и отрисовка
         * документа выбирает по нему ту же форму смены.
         */
        const val RECENT_SHIFTS = 100
    }
}
