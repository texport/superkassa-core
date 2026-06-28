package kz.mybrain.superkassa.core.domain.usecase.counter

import kz.mybrain.superkassa.core.domain.model.common.CounterKeyFormats
import kz.mybrain.superkassa.core.domain.model.common.format
import kz.mybrain.superkassa.core.domain.model.common.CounterScopes
import kz.mybrain.superkassa.core.domain.model.common.VatGroup
import kz.mybrain.superkassa.core.domain.model.receipt.PaymentType
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptOperationType
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptRequest
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.helper.tax.TaxCalculator

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
@Suppress("unused", "DuplicatedCode")
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
        val sumValue = request.total.bills

        // Суммы скидок/наценок/сдачи в тенге (только bills для счетчиков).
        val totalItemDiscountBills = request.items.mapNotNull { it.discount?.bills }.sum()
        val totalItemMarkupBills = request.items.mapNotNull { it.markup?.bills }.sum()
        val discountBills = request.discount?.bills ?: totalItemDiscountBills
        val markupBills = request.markup?.bills ?: totalItemMarkupBills
        val changeBills = request.change?.bills ?: 0L

        // Обновление операционных счетчиков.
        increment(kkmId, CounterScopes.SHIFT, shiftId, CounterKeyFormats.OPERATION_COUNT.format(operationKey), 1)
        increment(kkmId, CounterScopes.SHIFT, shiftId, CounterKeyFormats.OPERATION_SUM.format(operationKey), sumValue)
        increment(
            kkmId,
            CounterScopes.SHIFT,
            shiftId,
            CounterKeyFormats.DISCOUNT_SUM.format(operationKey),
            discountBills
        )
        increment(kkmId, CounterScopes.SHIFT, shiftId, CounterKeyFormats.MARKUP_SUM.format(operationKey), markupBills)

        // Секционные счётчики по позициям чека.
        request.items.forEach { item ->
            val sectionCode = item.sectionCode.ifBlank { "001" }
            increment(
                kkmId,
                CounterScopes.SHIFT,
                shiftId,
                CounterKeyFormats.SECTION_OPERATION_COUNT.format(sectionCode, operationKey),
                1
            )
            increment(
                kkmId,
                CounterScopes.SHIFT,
                shiftId,
                CounterKeyFormats.SECTION_OPERATION_SUM.format(sectionCode, operationKey),
                item.sum.bills
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
            discountBills
        )
        increment(
            kkmId,
            CounterScopes.SHIFT,
            shiftId,
            CounterKeyFormats.TICKET_MARKUP_SUM.format(operationKey),
            markupBills
        )
        increment(
            kkmId,
            CounterScopes.SHIFT,
            shiftId,
            CounterKeyFormats.TICKET_CHANGE_SUM.format(operationKey),
            changeBills
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
                payment.sum.bills
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
        val cashBills = request.payments.filter { it.type == PaymentType.CASH }.sumOf { it.sum.bills }
        if (cashBills != 0L) {
            increment(kkmId, CounterScopes.SHIFT, shiftId, CounterKeyFormats.CASH_SUM, cashBills)
        }
        val revenueDelta = when (request.operation) {
            ReceiptOperationType.SELL, ReceiptOperationType.BUY -> sumValue
            ReceiptOperationType.SELL_RETURN, ReceiptOperationType.BUY_RETURN -> -sumValue
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
                line.taxBase.bills
            )
            increment(
                kkmId,
                CounterScopes.SHIFT,
                shiftId,
                CounterKeyFormats.TAX_SUM.format(taxKey, operationKey),
                line.taxSum.bills
            )
            val turnoverWithoutTax = line.taxBase.bills - line.taxSum.bills
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
        increment(kkmId, CounterScopes.GLOBAL, null, CounterKeyFormats.DISCOUNT_SUM.format(operationKey), discountBills)
        increment(kkmId, CounterScopes.GLOBAL, null, CounterKeyFormats.MARKUP_SUM.format(operationKey), markupBills)

        // Глобальные секционные счётчики по позициям чека.
        request.items.forEach { item ->
            val sectionCode = item.sectionCode.ifBlank { "001" }
            increment(
                kkmId,
                CounterScopes.GLOBAL,
                null,
                CounterKeyFormats.SECTION_OPERATION_COUNT.format(sectionCode, operationKey),
                1
            )
            increment(
                kkmId,
                CounterScopes.GLOBAL,
                null,
                CounterKeyFormats.SECTION_OPERATION_SUM.format(sectionCode, operationKey),
                item.sum.bills
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
            discountBills
        )
        increment(
            kkmId,
            CounterScopes.GLOBAL,
            null,
            CounterKeyFormats.TICKET_MARKUP_SUM.format(operationKey),
            markupBills
        )
        increment(
            kkmId,
            CounterScopes.GLOBAL,
            null,
            CounterKeyFormats.TICKET_CHANGE_SUM.format(operationKey),
            changeBills
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
                payment.sum.bills
            )
            increment(
                kkmId,
                CounterScopes.GLOBAL,
                null,
                CounterKeyFormats.PAYMENT_COUNT.format(operationKey, payKey),
                1
            )
        }

        if (cashBills != 0L) {
            increment(kkmId, CounterScopes.GLOBAL, null, CounterKeyFormats.CASH_SUM, cashBills)
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
        }
    }
}
