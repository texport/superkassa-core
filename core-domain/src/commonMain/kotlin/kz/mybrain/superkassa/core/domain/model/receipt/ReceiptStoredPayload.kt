package kz.mybrain.superkassa.core.domain.model.receipt

import kotlinx.serialization.Serializable
import kz.mybrain.superkassa.core.domain.model.common.Money
import kz.mybrain.superkassa.core.domain.model.common.TaxRegime
import kz.mybrain.superkassa.core.domain.model.common.VatGroup

/**
 * Сохраняемые полезные данные чека для последующей переотрисовки (без конфиденциального PIN и ключа идемпотентности).
 * Используется для хранения в базе данных в бинарном/сериализованном формате.
 *
 * @property kkmId Идентификатор кассы.
 * @property operation Тип фискальной операции.
 * @property items Список позиций чека.
 * @property payments Список платежей.
 * @property total Итоговая сумма чека.
 * @property taken Сумма принятых средств.
 * @property change Сумма сдачи.
 * @property parentTicket Ссылка на исходный чек (при возврате).
 * @property taxRegime Применяемый налоговый режим.
 * @property defaultVatGroup Ставка НДС по умолчанию.
 * @property discount Сумма общей скидки на чек.
 * @property markup Сумма общей наценки на чек.
 * @property customerBin БИН/ИИН покупателя.
 * @property ticketTaxes Строки распределения налогов по чеку.
 */
@Suppress("unused")
@Serializable
data class ReceiptStoredPayload(
    val kkmId: String,
    val operation: ReceiptOperationType,
    val items: List<ReceiptItem>,
    val payments: List<ReceiptPayment>,
    val total: Money,
    val taken: Money? = null,
    val change: Money? = null,
    val parentTicket: ParentTicket? = null,
    val taxRegime: TaxRegime = TaxRegime.NO_VAT,
    val defaultVatGroup: VatGroup = VatGroup.NO_VAT,
    val discount: Money? = null,
    val markup: Money? = null,
    val customerBin: String? = null,
    val ticketTaxes: List<TaxLine>? = null
) {
    /**
     * Преобразует сохраненные данные обратно в структуру запроса чека.
     */
    fun toReceiptRequest(): ReceiptRequest = ReceiptRequest(
        kkmId = kkmId,
        pin = "",
        operation = operation,
        items = items,
        payments = payments,
        total = total,
        taken = taken,
        change = change,
        idempotencyKey = "",
        parentTicket = parentTicket,
        taxRegime = taxRegime,
        defaultVatGroup = defaultVatGroup,
        discount = discount,
        markup = markup,
        customerBin = customerBin,
        ticketTaxes = ticketTaxes
    )

    companion object {
        /**
         * Создает объект [ReceiptStoredPayload] из структуры запроса чека.
         */
        fun fromReceiptRequest(request: ReceiptRequest): ReceiptStoredPayload = ReceiptStoredPayload(
            kkmId = request.kkmId,
            operation = request.operation,
            items = request.items,
            payments = request.payments,
            total = request.total,
            taken = request.taken,
            change = request.change,
            parentTicket = request.parentTicket,
            taxRegime = request.taxRegime,
            defaultVatGroup = request.defaultVatGroup ?: VatGroup.NO_VAT,
            discount = request.discount,
            markup = request.markup,
            customerBin = request.customerBin,
            ticketTaxes = request.ticketTaxes
        )
    }
}
