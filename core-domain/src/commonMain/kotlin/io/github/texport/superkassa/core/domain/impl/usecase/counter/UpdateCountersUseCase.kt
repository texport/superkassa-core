package io.github.texport.superkassa.core.domain.impl.usecase.counter

import io.github.texport.superkassa.core.domain.api.model.common.CounterKeyFormats
import io.github.texport.superkassa.core.domain.api.model.common.format
import io.github.texport.superkassa.core.domain.api.model.common.CounterScopes
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup
import io.github.texport.superkassa.core.domain.api.model.receipt.PaymentType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.helper.tax.TaxCalculator

/**
 * Сценарий (Use Case) обновления счетчиков ККМ после проведения фискальных чеков.
 *
 * Отвечает за расчет и инкрементацию различных видов финансовых счетчиков:
 * - Операционных счетчиков смены (количественные и суммовые показатели продаж, покупок, возвратов).
 * - Секционных счетчиков (по кодам товарных секций).
 * - Счетчиков чеков и типов оплат (наличные, безналичные, мобильные и т.д.).
 * - Необнуляемых сумм (глобальные накопительные счетчики ККМ).
 * - Налоговых счетчиков по группам НДС (оборот, сумма налога, оборот без налога).
 * - Сменных и глобальных показателей выручки (включая учет знака выручки).
 *
 * @property storage Порт для доступа и обновления данных счетчиков в хранилище.
 */
class UpdateCountersUseCase(
    private val storage: StoragePort
) {
    /**
     * Калькулятор налогов для вычисления налоговых групп и сумм по позициям чека.
     */
    private val taxCalculator = TaxCalculator()

    /**
     * Выполняет обновление всех финансовых счетчиков на основе данных зарегистрированного чека.
     *
     * @param kkmId Идентификатор кассового аппарата (ККМ).
     * @param shiftId Идентификатор текущей открытой смены.
     * @param request Запрос на регистрацию чека, содержащий позиции, суммы и типы оплат.
     * @param isOffline Признак того, был ли чек зарегистрирован в автономном (офлайн) режиме.
     */
    fun execute(kkmId: String, shiftId: String, request: ReceiptRequest, isOffline: Boolean) {
        val operationKey = operationKey(request.operation)
        val sumValue = request.total.tiyn()

        // Суммы скидок/наценок/сдачи в тенге (только bills для счетчиков).
        val totalItemDiscountTiyn = request.items.mapNotNull { it.discount?.tiyn() }.sum()
        val totalItemMarkupTiyn = request.items.mapNotNull { it.markup?.tiyn() }.sum()
        val discountTiyn = request.discount?.tiyn() ?: totalItemDiscountTiyn
        val markupTiyn = request.markup?.tiyn() ?: totalItemMarkupTiyn
        val changeTiyn = request.change?.tiyn() ?: 0L

        // Обновление операционных счетчиков.
        increment(kkmId, CounterScopes.SHIFT, shiftId, CounterKeyFormats.OPERATION_COUNT.format(operationKey), 1)
        increment(kkmId, CounterScopes.SHIFT, shiftId, CounterKeyFormats.OPERATION_SUM.format(operationKey), sumValue)
        increment(
            kkmId,
            CounterScopes.SHIFT,
            shiftId,
            CounterKeyFormats.DISCOUNT_SUM.format(operationKey),
            discountTiyn
        )
        increment(kkmId, CounterScopes.SHIFT, shiftId, CounterKeyFormats.MARKUP_SUM.format(operationKey), markupTiyn)

        // Секционные счётчики по позициям чека.
        request.items.forEach { item ->
            val sectionCode = item.sectionCode.ifBlank { "001" }
            val countDelta = if (item.isStorno) -1L else 1L
            val sumDelta = if (item.isStorno) -item.sum.tiyn() else item.sum.tiyn()
            increment(
                kkmId,
                CounterScopes.SHIFT,
                shiftId,
                CounterKeyFormats.SECTION_OPERATION_COUNT.format(sectionCode, operationKey),
                countDelta
            )
            increment(
                kkmId,
                CounterScopes.SHIFT,
                shiftId,
                CounterKeyFormats.SECTION_OPERATION_SUM.format(sectionCode, operationKey),
                sumDelta
            )
        }

        increment(kkmId, CounterScopes.SHIFT, shiftId, CounterKeyFormats.TICKET_TOTAL_COUNT.format(operationKey), 1)
        increment(kkmId, CounterScopes.SHIFT, shiftId, CounterKeyFormats.TICKET_COUNT.format(operationKey), 1)
        increment(kkmId, CounterScopes.SHIFT, shiftId, CounterKeyFormats.TICKET_SUM.format(operationKey), sumValue)
        increment(
            kkmId,
            CounterScopes.SHIFT,
            shiftId,
            CounterKeyFormats.TICKET_DISCOUNT_SUM.format(operationKey),
            discountTiyn
        )
        increment(
            kkmId,
            CounterScopes.SHIFT,
            shiftId,
            CounterKeyFormats.TICKET_MARKUP_SUM.format(operationKey),
            markupTiyn
        )
        increment(
            kkmId,
            CounterScopes.SHIFT,
            shiftId,
            CounterKeyFormats.TICKET_CHANGE_SUM.format(operationKey),
            changeTiyn
        )

        if (isOffline) {
            increment(
                kkmId,
                CounterScopes.SHIFT,
                shiftId,
                CounterKeyFormats.TICKET_OFFLINE_COUNT.format(operationKey),
                1
            )
        }

        // Необнуляемые суммы: глобальные и по смене.
        if (sumValue != 0L) {
            val nonNullableKey = CounterKeyFormats.NON_NULLABLE_SUM.format(operationKey)
            increment(kkmId, CounterScopes.SHIFT, shiftId, nonNullableKey, sumValue)
            increment(kkmId, CounterScopes.GLOBAL, null, nonNullableKey, sumValue)
        }

        // Платежи по типам.
        request.payments.forEach { payment ->
            val payKey = paymentKey(payment.type)
            increment(
                kkmId,
                CounterScopes.SHIFT,
                shiftId,
                CounterKeyFormats.PAYMENT_SUM.format(operationKey, payKey),
                payment.sum.tiyn()
            )
            increment(
                kkmId,
                CounterScopes.SHIFT,
                shiftId,
                CounterKeyFormats.PAYMENT_COUNT.format(operationKey, payKey),
                1
            )
        }

        // Кассовая сумма (наличные) и выручка по смене.
        // Знак операции для денежного ящика: продажа и возврат покупки кладут
        // наличные в кассу, возврат продажи и покупка — выдают их из кассы.
        // Совпадает с эталоном OperationCalculator.addTicket.
        val cashDirection = when (request.operation) {
            ReceiptOperationType.SELL, ReceiptOperationType.BUY_RETURN -> 1L
            ReceiptOperationType.SELL_RETURN, ReceiptOperationType.BUY -> -1L
        }
        // В тиынах: целые тенге теряли дробную часть каждого чека.
        val cashTiyn = cashDirection *
            request.payments.filter { it.type == PaymentType.CASH }.sumOf { it.sum.tiyn() }
        if (cashTiyn != 0L) {
            increment(kkmId, CounterScopes.SHIFT, shiftId, CounterKeyFormats.CASH_SUM, cashTiyn)
        }
        // Знак выручки у покупки тот же, что у денежного ящика: покупка
        // выдаёт деньги из кассы и выручку уменьшает, возврат покупки —
        // возвращает их и увеличивает. Прежде знак у этой пары был
        // обратный, и смена, за которую касса отдала денег, показывала
        // выручку со знаком плюс. Совпадает с эталоном
        // OperationCalculator.addTicket.
        val revenueDelta = when (request.operation) {
            ReceiptOperationType.SELL, ReceiptOperationType.BUY_RETURN -> sumValue
            ReceiptOperationType.SELL_RETURN, ReceiptOperationType.BUY -> -sumValue
        }
        if (revenueDelta != 0L) {
            val currentRevenue = storage.loadCounters(kkmId, CounterScopes.SHIFT, shiftId)[CounterKeyFormats.REVENUE_SUM] ?: 0L
            val newRevenue = currentRevenue + revenueDelta
            storage.upsertCounter(kkmId, CounterScopes.SHIFT, shiftId, CounterKeyFormats.REVENUE_SUM, newRevenue)
            storage.upsertCounter(
                kkmId,
                CounterScopes.SHIFT,
                shiftId,
                CounterKeyFormats.REVENUE_IS_NEGATIVE,
                if (newRevenue < 0) 1 else 0
            )
        }

        // Налоговые счетчики.
        val taxResult = taxCalculator.calculateTicketTaxes(
            items = request.items,
            taxRegime = request.taxRegime,
            defaultVatGroup = request.defaultVatGroup ?: VatGroup.NO_VAT
        )
        taxResult.ticketTaxes.forEach { line ->
            val taxKey = line.vatGroup.name
            increment(
                kkmId,
                CounterScopes.SHIFT,
                shiftId,
                CounterKeyFormats.TAX_TURNOVER.format(taxKey, operationKey),
                line.taxBase.tiyn()
            )
            increment(
                kkmId,
                CounterScopes.SHIFT,
                shiftId,
                CounterKeyFormats.TAX_SUM.format(taxKey, operationKey),
                line.taxSum.tiyn()
            )
            val turnoverWithoutTax = line.taxBase.tiyn()
            increment(
                kkmId,
                CounterScopes.SHIFT,
                shiftId,
                CounterKeyFormats.TAX_TURNOVER_NO_TAX.format(taxKey, operationKey),
                turnoverWithoutTax
            )
        }

        // Глобальные счетчики.
        increment(kkmId, CounterScopes.GLOBAL, null, CounterKeyFormats.OPERATION_COUNT.format(operationKey), 1)
        increment(kkmId, CounterScopes.GLOBAL, null, CounterKeyFormats.OPERATION_SUM.format(operationKey), sumValue)
        increment(kkmId, CounterScopes.GLOBAL, null, CounterKeyFormats.DISCOUNT_SUM.format(operationKey), discountTiyn)
        increment(kkmId, CounterScopes.GLOBAL, null, CounterKeyFormats.MARKUP_SUM.format(operationKey), markupTiyn)

        // Глобальные секционные счётчики по позициям чека.
        request.items.forEach { item ->
            val sectionCode = item.sectionCode.ifBlank { "001" }
            val countDelta = if (item.isStorno) -1L else 1L
            val sumDelta = if (item.isStorno) -item.sum.tiyn() else item.sum.tiyn()
            increment(
                kkmId,
                CounterScopes.GLOBAL,
                null,
                CounterKeyFormats.SECTION_OPERATION_COUNT.format(sectionCode, operationKey),
                countDelta
            )
            increment(
                kkmId,
                CounterScopes.GLOBAL,
                null,
                CounterKeyFormats.SECTION_OPERATION_SUM.format(sectionCode, operationKey),
                sumDelta
            )
        }

        increment(kkmId, CounterScopes.GLOBAL, null, CounterKeyFormats.TICKET_TOTAL_COUNT.format(operationKey), 1)
        increment(kkmId, CounterScopes.GLOBAL, null, CounterKeyFormats.TICKET_COUNT.format(operationKey), 1)
        increment(kkmId, CounterScopes.GLOBAL, null, CounterKeyFormats.TICKET_SUM.format(operationKey), sumValue)
        increment(
            kkmId,
            CounterScopes.GLOBAL,
            null,
            CounterKeyFormats.TICKET_DISCOUNT_SUM.format(operationKey),
            discountTiyn
        )
        increment(
            kkmId,
            CounterScopes.GLOBAL,
            null,
            CounterKeyFormats.TICKET_MARKUP_SUM.format(operationKey),
            markupTiyn
        )
        increment(
            kkmId,
            CounterScopes.GLOBAL,
            null,
            CounterKeyFormats.TICKET_CHANGE_SUM.format(operationKey),
            changeTiyn
        )
        if (isOffline) {
            increment(kkmId, CounterScopes.GLOBAL, null, CounterKeyFormats.TICKET_OFFLINE_COUNT.format(operationKey), 1)
        }

        request.payments.forEach { payment ->
            val payKey = paymentKey(payment.type)
            increment(
                kkmId,
                CounterScopes.GLOBAL,
                null,
                CounterKeyFormats.PAYMENT_SUM.format(operationKey, payKey),
                payment.sum.tiyn()
            )
            increment(
                kkmId,
                CounterScopes.GLOBAL,
                null,
                CounterKeyFormats.PAYMENT_COUNT.format(operationKey, payKey),
                1
            )
        }

        if (cashTiyn != 0L) {
            increment(kkmId, CounterScopes.GLOBAL, null, CounterKeyFormats.CASH_SUM, cashTiyn)
        }
        if (revenueDelta != 0L) {
            val currentRevenue = storage.loadCounters(kkmId, CounterScopes.GLOBAL, null)[CounterKeyFormats.REVENUE_SUM] ?: 0L
            val newRevenue = currentRevenue + revenueDelta
            storage.upsertCounter(kkmId, CounterScopes.GLOBAL, null, CounterKeyFormats.REVENUE_SUM, newRevenue)
            storage.upsertCounter(
                kkmId,
                CounterScopes.GLOBAL,
                null,
                CounterKeyFormats.REVENUE_IS_NEGATIVE,
                if (newRevenue < 0) 1 else 0
            )
        }
    }

    /**
     * Вспомогательный метод для увеличения значения счетчика в хранилище на заданную величину.
     *
     * @param kkmId Идентификатор кассы.
     * @param scope Область видимости счетчика (сменная/глобальная).
     * @param shiftId Идентификатор смены (передается только для сменных счетчиков).
     * @param key Строковый ключ счетчика.
     * @param delta Величина, на которую необходимо увеличить текущее значение счетчика.
     */
    private fun increment(kkmId: String, scope: String, shiftId: String?, key: String, delta: Long) {
        val current = storage.loadCounters(kkmId, scope, shiftId)[key] ?: 0L
        storage.upsertCounter(kkmId, scope, shiftId, key, current + delta)
    }

    /**
     * Возвращает строковый идентификатор типа фискальной операции для формирования ключа счетчика.
     */
    private fun operationKey(operation: ReceiptOperationType): String {
        return when (operation) {
            ReceiptOperationType.SELL -> "OPERATION_SELL"
            ReceiptOperationType.SELL_RETURN -> "OPERATION_SELL_RETURN"
            ReceiptOperationType.BUY -> "OPERATION_BUY"
            ReceiptOperationType.BUY_RETURN -> "OPERATION_BUY_RETURN"
        }
    }

    /**
     * Возвращает строковый идентификатор типа платежа для формирования ключа счетчика.
     */
    private fun paymentKey(payment: PaymentType): String {
        return when (payment) {
            PaymentType.CASH -> "PAYMENT_CASH"
            PaymentType.CARD -> "PAYMENT_CARD"
            PaymentType.ELECTRONIC -> "PAYMENT_ELECTRONIC"
            PaymentType.MOBILE -> "PAYMENT_MOBILE"
            PaymentType.CREDIT -> "PAYMENT_CREDIT"
            PaymentType.TARE -> "PAYMENT_TARE"
        }
    }
}
