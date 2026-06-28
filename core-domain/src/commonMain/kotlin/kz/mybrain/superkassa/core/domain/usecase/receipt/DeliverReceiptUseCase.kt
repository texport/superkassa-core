package kz.mybrain.superkassa.core.domain.usecase.receipt

import kz.mybrain.superkassa.core.domain.model.kkm.FiscalDocumentSnapshot
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptRequest
import kz.mybrain.superkassa.core.domain.helper.ReceiptDeliveryHelper

/**
 * Сценарий доставки чека покупателю.
 *
 * Предоставляет логику для отправки фискального документа (чека) покупателю через различные каналы:
 * печать на принтере, отправка по электронной почте, SMS или через мессенджеры.
 *
 * @property helper Вспомогательный компонент для непосредственной отправки и форматирования чека.
 */
class DeliverReceiptUseCase(
    private val helper: ReceiptDeliveryHelper
) {
    /**
     * Выполняет отправку фискального чека покупателю.
     *
     * @param kkmId Идентификатор кассового аппарата (ККМ).
     * @param documentId Уникальный идентификатор фискального документа.
     * @param receipt Исходный запрос чека, содержащий настройки доставки и контакты покупателя.
     * @param docSnapshot Снимок состояния фискального документа из базы данных.
     * @param receiptUrl Ссылка на электронный чек на сервере ОФД (при наличии).
     * @param responseBin Бинарные данные ответа от ОФД/принтера.
     */
    fun execute(
        kkmId: String,
        documentId: String,
        receipt: ReceiptRequest,
        docSnapshot: FiscalDocumentSnapshot,
        receiptUrl: String?,
        responseBin: ByteArray?
    ) {
        // Перенаправление вызова во вспомогательный сервис для физической/электронной отправки
        helper.deliverReceipt(kkmId, documentId, receipt, docSnapshot, receiptUrl, responseBin)
    }
}
