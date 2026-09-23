package io.github.texport.superkassa.core.domain.impl.usecase.receipt

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType
import io.github.texport.superkassa.core.domain.api.model.receipt.ParentTicket

/**
 * Команда для создания фискального чека, передаваемая в сценарий [ProcessReceiptUseCase].
 *
 * Содержит сырые входные данные, полученные от клиента, для последующей валидации и расчета в доменном слое.
 */
data class CreateReceiptCommand(
    val kkmId: String,
    val pin: String,
    val operation: ReceiptOperationType,
    val idempotencyKey: String,
    val items: List<ItemInput>,
    val discountPercent: Decimal?,
    val discountSum: Decimal?,
    val markupPercent: Decimal?,
    val markupSum: Decimal?,
    val payments: List<PaymentInput>,
    val taken: Decimal?,
    val parentTicket: ParentTicket? = null,
    val domain: io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptDomain? = null,
    /** НДС на весь чек; `null` — НДС по позициям. Взаимоисключающе со ставками позиций. */
    val vatGroup: String? = null,
    val customerBin: String? = null
) {
    /**
     * Сырые входные данные товарной позиции чека.
     */
    data class ItemInput(
        val name: String,
        /** Наименование на казахском: печатается на чеке, в ОФД не уходит. */
        val nameKk: String? = null,
        val price: Decimal,
        val quantity: Decimal,
        val barcode: String? = null,
        val vatGroup: String? = null,
        val discountPercent: Decimal? = null,
        val discountSum: Decimal? = null,
        val markupPercent: Decimal? = null,
        val markupSum: Decimal? = null,
        val measureUnitCode: String? = null,
        val listExciseStamp: List<String>? = null,
        val ntin: String? = null,
        val isStorno: Boolean = false
    )

    /**
     * Сырые входные данные способа оплаты.
     */
    data class PaymentInput(
        val type: String,
        val sum: Decimal
    )
}
