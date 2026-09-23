package io.github.texport.superkassa.testing.impl.kassa

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.presentation.api.model.kkm.DocumentDetailsResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.VatGroup
import io.github.texport.superkassa.core.presentation.api.model.receipt.ParentTicketRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptItemRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptItemView
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptPaymentRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptSellRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptSellReturnRequest
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Запросы чеков, какими их собирает экран кассира: позиции с настоящими
 * названиями (обобщённые «Товар» ядро не принимает), оплата наличными
 * ровно на сумму чека, свой ключ идемпотентности у каждого чека.
 */
internal object Receipts {
    private const val TIYN_SCALE = 2
    private const val QUANTITY_SCALE = 3
    private val goods = listOf("Хлеб бородинский", "Молоко 2,5%", "Сыр косичка", "Чай зелёный", "Яблоки апорт", "Кумыс")

    /** Одна позиция [price] × [quantity]. */
    fun sale(price: String, quantity: String): ReceiptSellRequest {
        val item = item(0, price, quantity)
        return ReceiptSellRequest(idempotencyKey = key("sale"), items = listOf(item), payments = cash(sum(item)))
    }

    /** Одна позиция на [price] со скидкой на чек [discount] в тенге. */
    fun discounted(price: String, discount: String): ReceiptSellRequest {
        val item = item(0, price, "1")
        val paid = sum(item) - Decimal.parse(discount).scaled(TIYN_SCALE)
        return ReceiptSellRequest(
            idempotencyKey = key("sale"),
            items = listOf(item),
            discountSum = Decimal.parse(discount),
            payments = cash(paid)
        )
    }

    /** Одна позиция на [price] с НДС на весь чек по ставке [vat]. */
    fun receiptVat(price: String, vat: VatGroup): ReceiptSellRequest =
        sale(price, "1").copy(idempotencyKey = key("sale"), vatGroup = vat.name)

    /** По позиции на [price] на каждую ставку [vats]. */
    fun itemVat(price: String, vats: List<VatGroup>): ReceiptSellRequest {
        val items = vats.mapIndexed { index, vat -> item(index, price, "1").copy(vatGroup = vat.name) }
        return ReceiptSellRequest(idempotencyKey = key("sale"), items = items, payments = cash(items.sumOf(::sum)))
    }

    /** Возврат всех позиций продажи [sale] по цене продажи, наличными. */
    fun refund(sale: DocumentDetailsResponse): ReceiptSellReturnRequest {
        val items = sale.items.map(::returned)
        return ReceiptSellReturnRequest(
            idempotencyKey = key("refund"),
            items = items,
            payments = cash(items.sumOf(::sum)),
            parentTicket = basis(sale.document)
        )
    }

    /** Ключ идемпотентности, не повторяющийся и после перезапуска кассы на том же каталоге. */
    fun key(kind: String): String = "$kind-${UUID.randomUUID()}"

    /** Позиция продажи, возвращаемая так, как была продана. */
    private fun returned(sold: ReceiptItemView) = ReceiptItemRequest(
        name = sold.name,
        nameKk = sold.nameKk,
        price = sold.price,
        quantity = Decimal.ofScaled(sold.quantityThousandths, QUANTITY_SCALE),
        vatGroup = sold.vatGroup
    )

    /** Основание возврата: номер, время, касса и сумма чека продажи. */
    private fun basis(sale: FiscalDocumentResponse) = ParentTicketRequest(
        parentTicketNumber = checkNotNull(sale.docNo) { "sale ${sale.id} has no BFD number" },
        parentTicketDateTime = utcSeconds(sale.createdAt),
        kgdKkmId = checkNotNull(sale.registrationNumber) { "sale ${sale.id} has no KGD number" },
        parentTicketTotal = Decimal.ofScaled(checkNotNull(sale.totalAmount), TIYN_SCALE),
        parentTicketIsOffline = sale.isAutonomous
    )

    private fun item(index: Int, price: String, quantity: String) =
        ReceiptItemRequest(
            name = goods[index % goods.size],
            price = Decimal.parse(price),
            quantity = Decimal.parse(quantity)
        )

    /** Сумма позиции в тиынах. */
    private fun sum(item: ReceiptItemRequest): Long =
        Decimal.ofScaled(
            item.price.scaled(TIYN_SCALE) * item.quantity.unscaled,
            TIYN_SCALE + item.quantity.scale
        ).scaled(TIYN_SCALE)

    private fun cash(tiyn: Long) = listOf(
        ReceiptPaymentRequest(type = "CASH", sum = Decimal.ofScaled(tiyn, TIYN_SCALE))
    )

    private fun utcSeconds(millis: Long): String {
        val utc = Instant.ofEpochMilli(millis).truncatedTo(ChronoUnit.SECONDS).atOffset(ZoneOffset.UTC)
        return utc.toLocalDateTime().toString()
    }
}
