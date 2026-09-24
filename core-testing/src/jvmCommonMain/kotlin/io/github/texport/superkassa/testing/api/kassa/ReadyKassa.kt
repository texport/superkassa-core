package io.github.texport.superkassa.testing.api.kassa

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.presentation.api.SuperkassaApi
import io.github.texport.superkassa.core.presentation.api.model.delivery.ReceiptDeliveryResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.CashOperationRequest
import io.github.texport.superkassa.core.presentation.api.model.kkm.CashOperationResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.VatGroup
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptResponse
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptSellRequest
import io.github.texport.superkassa.core.presentation.api.model.shift.ReportResponse
import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftResponse
import io.github.texport.superkassa.testing.api.bfd.FakeBfd
import io.github.texport.superkassa.testing.api.clock.MovableClock
import io.github.texport.superkassa.testing.impl.kassa.Receipts
import kotlin.time.Duration.Companion.seconds

/**
 * Зарегистрированная касса на стенде и сценарные заготовки для неё.
 *
 * Каждая заготовка — вызов фасада под пином кассира или администратора,
 * тот же, что делает экран кассы; в базу напрямую ничего не пишется.
 * Сбои связи заказываются у [bfd] перед заготовкой.
 *
 * @property kkmId идентификатор кассы в ядре.
 * @property systemId номер кассы в БФД.
 * @property adminPin пин администратора.
 * @property cashierPin пин кассира.
 */
class ReadyKassa internal constructor(
    private val bench: TestBench,
    val kkmId: String,
    val systemId: Long,
    val adminPin: String,
    val cashierPin: String
) {
    /** Фасад кассы. */
    val api: SuperkassaApi get() = bench.api

    /** БФД стенда. */
    val bfd: FakeBfd get() = bench.bfd

    /** Часы кассы. */
    val clock: MovableClock get() = bench.clock

    /** Касса, как её видит фасад сейчас. */
    fun info(): KkmResponse = api.getKkm(kkmId)

    /** Открывает смену под администратором. */
    fun openShift(): ShiftResponse = api.openShift(kkmId, adminPin)

    /** Закрывает смену Z-отчётом под администратором. */
    fun closeShift(): ReportResponse = api.closeShift(kkmId, adminPin)

    /** Снимает X-отчёт под кассиром. */
    fun xReport(): ReportResponse = api.createReport(kkmId, cashierPin)

    /** Чек продажи [request] под кассиром: повтор того же запроса проверяет идемпотентность. */
    fun sell(request: ReceiptSellRequest): ReceiptResponse = api.createSellReceipt(kkmId, cashierPin, request)

    /** Продажа одной позиции [price] × [quantity], наличными без сдачи. */
    fun sell(price: String = "500.00", quantity: String = "3"): ReceiptResponse = sell(Receipts.sale(price, quantity))

    /** Продажа позиции на [price] со скидкой на чек [discount] в тенге. */
    fun sellWithDiscount(price: String = "1000.00", discount: String = "100.00"): ReceiptResponse =
        sell(Receipts.discounted(price, discount))

    /** Продажа с НДС на весь чек по ставке [vat]. */
    fun sellWithReceiptVat(vat: VatGroup, price: String = "1120.00"): ReceiptResponse =
        sell(Receipts.receiptVat(price, vat))

    /** Продажа с НДС по позициям: по позиции на [price] на каждую ставку [vats]. */
    fun sellWithItemVat(vararg vats: VatGroup, price: String = "1120.00"): ReceiptResponse =
        sell(Receipts.itemVat(price, vats.toList()))

    /** Возврат всех позиций чека продажи [sale] по цене продажи, наличными. */
    fun refund(sale: ReceiptResponse): ReceiptResponse {
        val details = api.getDocumentDetails(kkmId, sale.documentId, adminPin)
        return api.createSellReturnReceipt(kkmId, cashierPin, Receipts.refund(details))
    }

    /** Внесение наличных [amount] в тенге. */
    fun cashIn(amount: String = "5000.00"): CashOperationResponse =
        api.cashIn(kkmId, cashierPin, CashOperationRequest(Decimal.parse(amount), Receipts.key("cash-in")))

    /** Изъятие наличных [amount] в тенге. */
    fun cashOut(amount: String = "1000.00"): CashOperationResponse =
        api.cashOut(kkmId, cashierPin, CashOperationRequest(Decimal.parse(amount), Receipts.key("cash-out")))

    /** Продажа, до БФД не дошедшая: документ оформлен автономно и ждёт в очереди досылки. */
    fun offlineSale(price: String = "700.00"): ReceiptResponse {
        bfd.unreachableOnce()
        return sell(price, "1")
    }

    /** Продажа, которой БФД отказал кодом [code]: документ отклонён и виден кассиру с кодом. */
    fun rejectedSale(code: Int = INCORRECT_REQUEST_DATA, price: String = "900.00"): ReceiptResponse {
        bfd.refuseNext(code)
        return sell(price, "1")
    }

    /**
     * Связь с БФД восстановлена, пауза восстановления прошла, и очередь
     * досылается одним заходом, как это сделал бы фон, — под замком
     * писателя кассы, тем же путём, что у досылки самой кассы.
     *
     * @return сколько документов обработано заходом.
     */
    fun resendQueue(): Int {
        bfd.connect()
        clock.move(RECONNECT_PAUSE)
        return bench.superkassa.sendQueueNow()
    }

    /**
     * Доставляет чеки покупателям одним заходом, как это сделал бы фон:
     * всё, чей срок наступил по часам кассы.
     *
     * @return сколько задач доставки отправлено, с успехом или отказом.
     */
    fun deliverReceipts(): Int = bench.superkassa.sendDeliveriesNow()

    /** Доставка чека [receipt] по каналам, как её видит журнал кассира. */
    fun deliveries(receipt: ReceiptResponse): List<ReceiptDeliveryResponse> =
        bench.superkassa.delivery.receiptDeliveries(kkmId, receipt.documentId, cashierPin)

    /** Повтор доставки чека [receipt] кассиром из журнала. */
    fun resendReceipt(receipt: ReceiptResponse): List<ReceiptDeliveryResponse> =
        bench.superkassa.delivery.resendReceipt(kkmId, receipt.documentId, cashierPin)

    private companion object {
        /** RESULT_TYPE_INCORRECT_REQUEST_DATA: БФД не принял данные документа. */
        const val INCORRECT_REQUEST_DATA = 13
    }
}

/** Больше паузы восстановления связи: не менее 60 с по протоколу. */
private val RECONNECT_PAUSE = 61.seconds
