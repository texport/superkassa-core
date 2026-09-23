package io.github.texport.superkassa.core.domain.api.model.receipt

import kotlinx.serialization.Serializable

import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.common.TaxRegime
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup

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
 * @property defaultVatGroup Ставка кассы для позиций без своей ставки.
 * @property vatGroup НДС на весь чек; `null` — НДС по позициям (и у всех чеков, записанных раньше).
 * @property discount Сумма общей скидки на чек.
 * @property markup Сумма общей наценки на чек.
 * @property customerBin БИН/ИИН покупателя.
 * @property ticketTaxes Строки распределения налогов по чеку.
 */
@Serializable
data class ReceiptStoredPayload(
    val kkmId: String,
    val operation: ReceiptOperationType,
    val items: List<ReceiptItem>,
    val payments: List<ReceiptPayment>,
    val total: Money,
    val taken: Money? = null,
    val change: Money? = null,
    val idempotencyKey: String = "",
    val parentTicket: ParentTicket? = null,
    /** Отраслевые реквизиты чека, если отрасль объявлена. */
    val domain: ReceiptDomain? = null,
    val taxRegime: TaxRegime = TaxRegime.NO_VAT,
    val defaultVatGroup: VatGroup = VatGroup.NO_VAT,
    val vatGroup: VatGroup? = null,
    val discount: Money? = null,
    val markup: Money? = null,
    val customerBin: String? = null,
    val ticketTaxes: List<TaxLine>? = null,
    /** Кто оформил чек: имя, а не пин — пин на диск не пишется. */
    val operatorName: String? = null
) {
    /**
     * Преобразует сохраненные данные обратно в структуру запроса чека.
     */
    fun toReceiptRequest(): ReceiptRequest = ReceiptRequest(
        kkmId = kkmId,
        pin = "",
        operatorName = operatorName,
        operation = operation,
        items = items,
        payments = payments,
        total = total,
        taken = taken,
        change = change,
        idempotencyKey = "",
        parentTicket = parentTicket,
        domain = domain,
        taxRegime = taxRegime,
        defaultVatGroup = defaultVatGroup,
        vatGroup = vatGroup,
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
            operatorName = request.operatorName,
            items = request.items,
            payments = request.payments,
            total = request.total,
            taken = request.taken,
            change = request.change,
            idempotencyKey = request.idempotencyKey,
            parentTicket = request.parentTicket,
            domain = request.domain,
            taxRegime = request.taxRegime,
            defaultVatGroup = request.defaultVatGroup ?: VatGroup.NO_VAT,
            vatGroup = request.vatGroup,
            discount = request.discount,
            markup = request.markup,
            customerBin = request.customerBin,
            ticketTaxes = request.ticketTaxes
        )
    }
}
