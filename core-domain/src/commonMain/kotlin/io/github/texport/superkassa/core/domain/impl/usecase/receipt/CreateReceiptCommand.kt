package io.github.texport.superkassa.core.domain.impl.usecase.receipt

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
    val discountPercent: Double?,
    val discountSum: Double?,
    val markupPercent: Double?,
    val markupSum: Double?,
    val payments: List<PaymentInput>,
    val taken: Double?,
    val parentTicket: ParentTicket? = null,
    val defaultVatGroup: String? = null,
    val customerBin: String? = null
) {
    /**
     * Сырые входные данные товарной позиции чека.
     */
    data class ItemInput(
        val name: String,
        val price: Double,
        val quantity: Double,
        val barcode: String? = null,
        val vatGroup: String? = null,
        val discountPercent: Double? = null,
        val discountSum: Double? = null,
        val markupPercent: Double? = null,
        val markupSum: Double? = null,
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
        val sum: Double
    )
}
