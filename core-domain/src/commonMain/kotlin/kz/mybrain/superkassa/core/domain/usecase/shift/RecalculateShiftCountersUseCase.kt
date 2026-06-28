package kz.mybrain.superkassa.core.domain.usecase.shift

import kz.mybrain.superkassa.core.domain.model.common.CounterKeyFormats
import kz.mybrain.superkassa.core.domain.model.common.format
import kz.mybrain.superkassa.core.domain.model.common.CounterScopes
import kz.mybrain.superkassa.core.domain.model.common.VatGroup
import kz.mybrain.superkassa.core.domain.model.kkm.CashOperationType
import kz.mybrain.superkassa.core.domain.model.kkm.FiscalDocumentSnapshot
import kz.mybrain.superkassa.core.domain.model.receipt.PaymentType
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptOperationType
import kz.mybrain.superkassa.core.domain.model.shift.ShiftInfo
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.helper.tax.TaxCalculator

/**
 * Сценарий (Use Case) пересчета/пересобирания счетчиков смены на основе фактических фискальных документов.
 *
 * Используется для восстановления корректных значений счетчиков смены (например, при сбоях,
 * расхождениях или необходимости синхронизации) путем обхода всех фискальных документов
 * (чеков и операций внесения/изъятия наличных), зарегистрированных за данную смену.
 *
 * @property storage Порт для доступа к хранилищу данных ККМ, смен, документов и счетчиков.
 */
@Suppress("unused", "DuplicatedCode")
class RecalculateShiftCountersUseCase(
    private val storage: StoragePort
) {
    /**
     * Калькулятор для расчета налоговых показателей чеков.
     */
    private val taxService = TaxCalculator()

    /**
     * Запускает процедуру пересчета счетчиков смены и сохраняет их в хранилище.
     *
     * @param kkmId Уникальный идентификатор ККМ.
     * @param shift Информация о смене, для которой выполняется пересчет.
     * @return [Map] Карта пересчитанных счетчиков (ключ-значение).
     */
    fun execute(kkmId: String, shift: ShiftInfo): Map<String, Long> {
        val rebuilt = rebuildShiftCounters(kkmId, shift)
        rebuilt.forEach { (key, value) ->
            storage.upsertCounter(kkmId, CounterScopes.SHIFT, shift.id, key, value)
        }
        return rebuilt
    }

    /**
     * Восстанавливает значения счетчиков смены на основе документов из хранилища.
     *
     * @param kkmId Уникальный идентификатор ККМ.
     * @param shift Информация о смене.
     * @return [Map] Карта восстановленных счетчиков (ключ-значение).
     */
    fun rebuildShiftCounters(kkmId: String, shift: ShiftInfo): Map<String, Long> {
        val existing = storage.loadCounters(kkmId, CounterScopes.SHIFT, shift.id)
        val result = mutableMapOf<String, Long>()

        val operations = listOf(
            "OPERATION_SELL",
            "OPERATION_SELL_RETURN",
            "OPERATION_BUY",
            "OPERATION_BUY_RETURN"
        )
        // Инициализируем стартовые значения счетчиков смены на основе сохраненных данных на начало смены
        operations.forEach { op ->
            val startKey = CounterKeyFormats.START_SHIFT_NON_NULLABLE_SUM.format(op)
            val startValue = existing[startKey] ?: 0L
            result[startKey] = startValue

            val nonNullableKey = CounterKeyFormats.NON_NULLABLE_SUM.format(op)
            result[nonNullableKey] = startValue
        }

        var offset = 0
        val limit = 500
        // Постранично загружаем фискальные документы смены и применяем их к счетчикам
        while (true) {
            val docs = storage.listFiscalDocumentsByShift(kkmId, shift.id, limit = limit, offset = offset)
            if (docs.isEmpty()) break
            docs.forEach { doc ->
                when (doc.docType) {
                    "CHECK" -> applyReceiptDocument(doc, result)
                    "CASH_IN", "CASH_OUT" -> applyCashOperationDocument(doc, result)
                }
            }
            offset += limit
        }

        // Вычисляем суммарные не обнуляемые итоги на конец смены
        operations.forEach { op ->
            val opSum = result[CounterKeyFormats.OPERATION_SUM.format(op)] ?: 0L
            val startVal = result[CounterKeyFormats.START_SHIFT_NON_NULLABLE_SUM.format(op)] ?: 0L
            result[CounterKeyFormats.NON_NULLABLE_SUM.format(op)] = startVal + opSum
        }

        // Обновляем показатель выручки
        val revenue = result[CounterKeyFormats.REVENUE_SUM] ?: 0L
        if (revenue != 0L) {
            result[CounterKeyFormats.REVENUE_IS_NEGATIVE] = if (revenue < 0) 1L else 0L
        } else {
            result.remove(CounterKeyFormats.REVENUE_IS_NEGATIVE)
        }

        return result
    }

    /**
     * Применяет данные чека (продажа, возврат и т.д.) к сменным счетчикам.
     *
     * @param doc Снимок фискального документа.
     * @param counters Карта накапливаемых счетчиков.
     */
    private fun applyReceiptDocument(doc: FiscalDocumentSnapshot, counters: MutableMap<String, Long>) {
        val pair = storage.findFiscalDocumentWithReceiptPayload(doc.id) ?: return
        val request = pair.second

        val operationKey = when (request.operation) {
            ReceiptOperationType.SELL -> "OPERATION_SELL"
            ReceiptOperationType.SELL_RETURN -> "OPERATION_SELL_RETURN"
            ReceiptOperationType.BUY -> "OPERATION_BUY"
            ReceiptOperationType.BUY_RETURN -> "OPERATION_BUY_RETURN"
        }
        val sumValue = request.total.bills

        val totalItemDiscountBills = request.items.mapNotNull { it.discount?.bills }.sum()
        val totalItemMarkupBills = request.items.mapNotNull { it.markup?.bills }.sum()
        val discountBills = request.discount?.bills ?: totalItemDiscountBills
        val markupBills = request.markup?.bills ?: totalItemMarkupBills
        val changeBills = request.change?.bills ?: 0L

        // Увеличиваем счетчики количества и сумм операций
        increment(counters, CounterKeyFormats.OPERATION_COUNT.format(operationKey), 1L)
        increment(counters, CounterKeyFormats.OPERATION_SUM.format(operationKey), sumValue)
        increment(counters, CounterKeyFormats.DISCOUNT_SUM.format(operationKey), discountBills)
        increment(counters, CounterKeyFormats.MARKUP_SUM.format(operationKey), markupBills)

        // Обновляем счетчики по секциям/отделам
        request.items.forEach { item ->
            val sectionCode = item.sectionCode.ifBlank { "001" }
            increment(
                counters,
                CounterKeyFormats.SECTION_OPERATION_COUNT.format(sectionCode, operationKey),
                1L
            )
            increment(
                counters,
                CounterKeyFormats.SECTION_OPERATION_SUM.format(sectionCode, operationKey),
                item.sum.bills
            )
        }

        // Обновляем счетчики билетов/чеков
        increment(counters, CounterKeyFormats.TICKET_TOTAL_COUNT.format(operationKey), 1L)
        increment(counters, CounterKeyFormats.TICKET_COUNT.format(operationKey), 1L)
        increment(counters, CounterKeyFormats.TICKET_SUM.format(operationKey), sumValue)
        increment(counters, CounterKeyFormats.TICKET_DISCOUNT_SUM.format(operationKey), discountBills)
        increment(counters, CounterKeyFormats.TICKET_MARKUP_SUM.format(operationKey), markupBills)
        increment(counters, CounterKeyFormats.TICKET_CHANGE_SUM.format(operationKey), changeBills)
        if (doc.isAutonomous || doc.ofdStatus == "TIMEOUT") {
            increment(counters, CounterKeyFormats.TICKET_OFFLINE_COUNT.format(operationKey), 1L)
        }

        if (sumValue != 0L) {
            val nonNullableKey = CounterKeyFormats.NON_NULLABLE_SUM.format(operationKey)
            increment(counters, nonNullableKey, sumValue)
        }

        // Обновляем счетчики по типам оплат (наличные, карта, электронные и т.д.)
        request.payments.forEach { payment ->
            val payKey = when (payment.type) {
                PaymentType.CASH -> "PAYMENT_CASH"
                PaymentType.CARD -> "PAYMENT_CARD"
                PaymentType.ELECTRONIC -> "PAYMENT_ELECTRONIC"
                PaymentType.MOBILE -> "PAYMENT_MOBILE"
            }
            increment(
                counters,
                CounterKeyFormats.PAYMENT_SUM.format(operationKey, payKey),
                payment.sum.bills
            )
            increment(
                counters,
                CounterKeyFormats.PAYMENT_COUNT.format(operationKey, payKey),
                1L
            )
        }

        // Обновляем счетчик наличных в денежном ящике (только для наличных платежей)
        val cashBills = request.payments
            .filter { it.type == PaymentType.CASH }
            .sumOf { it.sum.bills }
        if (cashBills != 0L) {
            increment(counters, CounterKeyFormats.CASH_SUM, cashBills)
        }

        // Рассчитываем влияние на общую выручку ККМ
        val revenueDelta = when (request.operation) {
            ReceiptOperationType.SELL, ReceiptOperationType.BUY -> sumValue
            ReceiptOperationType.SELL_RETURN, ReceiptOperationType.BUY_RETURN -> -sumValue
        }
        if (revenueDelta != 0L) {
            val current = counters[CounterKeyFormats.REVENUE_SUM] ?: 0L
            counters[CounterKeyFormats.REVENUE_SUM] = current + revenueDelta
        }

        // Вычисляем и распределяем налоги чека по группам НДС
        val taxResult = taxService.calculateTicketTaxes(
            items = request.items,
            taxRegime = request.taxRegime,
            defaultVatGroup = request.defaultVatGroup ?: VatGroup.NO_VAT
        )
        taxResult.ticketTaxes.forEach { line ->
            val taxKey = line.vatGroup.name
            increment(
                counters,
                CounterKeyFormats.TAX_TURNOVER.format(taxKey, operationKey),
                line.taxBase.bills
            )
            increment(
                counters,
                CounterKeyFormats.TAX_SUM.format(taxKey, operationKey),
                line.taxSum.bills
            )
            val turnoverWithoutTax = line.taxBase.bills - line.taxSum.bills
            increment(
                counters,
                CounterKeyFormats.TAX_TURNOVER_NO_TAX.format(taxKey, operationKey),
                turnoverWithoutTax
            )
        }
    }

    /**
     * Применяет операцию внесения или изъятия наличных к счетчикам смены.
     *
     * @param doc Снимок фискального документа.
     * @param counters Карта накапливаемых счетчиков.
     */
    private fun applyCashOperationDocument(doc: FiscalDocumentSnapshot, counters: MutableMap<String, Long>) {
        val amount = doc.totalAmount ?: 0L
        if (amount == 0L) return

        val type = when (doc.docType) {
            CashOperationType.CASH_IN.name -> CashOperationType.CASH_IN
            CashOperationType.CASH_OUT.name -> CashOperationType.CASH_OUT
            else -> return
        }

        val opKey = when (type) {
            CashOperationType.CASH_IN -> "MONEY_PLACEMENT_DEPOSIT"
            CashOperationType.CASH_OUT -> "MONEY_PLACEMENT_WITHDRAWAL"
        }

        val delta = when (type) {
            CashOperationType.CASH_IN -> amount
            CashOperationType.CASH_OUT -> -amount
        }
        increment(counters, CounterKeyFormats.CASH_SUM, delta)

        // Обновляем счетчики операций внесения/изъятия
        increment(counters, CounterKeyFormats.MONEY_PLACEMENT_TOTAL_COUNT.format(opKey), 1L)
        increment(counters, CounterKeyFormats.MONEY_PLACEMENT_COUNT.format(opKey), 1L)
        increment(counters, CounterKeyFormats.MONEY_PLACEMENT_SUM.format(opKey), amount)
        if (doc.isAutonomous || doc.ofdStatus == "TIMEOUT") {
            increment(
                counters,
                CounterKeyFormats.MONEY_PLACEMENT_OFFLINE_COUNT.format(opKey),
                1L
            )
        }
    }

    /**
     * Безопасно увеличивает значение счетчика на заданную величину.
     *
     * @param counters Карта накапливаемых счетчиков.
     * @param key Уникальный ключ счетчика.
     * @param delta Величина, на которую увеличивается счетчик.
     */
    private fun increment(counters: MutableMap<String, Long>, key: String, delta: Long) {
        if (delta == 0L) return
        val current = counters[key] ?: 0L
        counters[key] = current + delta
    }
}
