package io.github.texport.superkassa.core.domain.impl.usecase.shift

import io.github.texport.superkassa.core.domain.api.model.common.CounterKeyFormats
import io.github.texport.superkassa.core.domain.api.model.common.format
import io.github.texport.superkassa.core.domain.api.model.common.CounterScopes
import io.github.texport.superkassa.core.domain.api.model.kkm.CashOperationType
import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.kkm.becameFiscal
import io.github.texport.superkassa.core.domain.api.model.receipt.PaymentType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptDocumentTypes
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.helper.tax.taxCounterDeltas

/**
 * Сценарий (Use Case) пересчета/пересобирания счетчиков смены на основе фактических фискальных документов.
 *
 * Используется для восстановления корректных значений счетчиков смены (например, при сбоях,
 * расхождениях или необходимости синхронизации) путем обхода всех фискальных документов
 * (чеков и операций внесения/изъятия наличных), зарегистрированных за данную смену.
 *
 * @property storage Порт для доступа к хранилищу данных ККМ, смен, документов и счетчиков.
 */
class RecalculateShiftCountersUseCase(
    private val storage: StoragePort
) {
    /**
     * Запускает процедуру пересчета счетчиков смены и сохраняет их в хранилище.
     *
     * @param kkmId Уникальный идентификатор ККМ.
     * @param shift Информация о смене, для которой выполняется пересчет.
     * @return [Map] Карта пересчитанных счетчиков (ключ-значение).
     */
    fun execute(kkmId: String, shift: ShiftInfo): Map<String, Long> {
        val cashBefore = storage.loadCounters(kkmId, CounterScopes.SHIFT, shift.id)[CounterKeyFormats.CASH_SUM]
        val rebuilt = rebuildShiftCounters(kkmId, shift)
        rebuilt.forEach { (key, value) ->
            storage.upsertCounter(kkmId, CounterScopes.SHIFT, shift.id, key, value)
        }
        reconcileGlobalCash(kkmId, cashBefore, rebuilt[CounterKeyFormats.CASH_SUM] ?: 0L)
        return rebuilt
    }

    /**
     * Переносит поправку денежного ящика из смены в глобальный счётчик.
     *
     * Наличные в ящике живут в двух областях сразу: сменной и глобальной.
     * Смена открывается значением глобального счётчика и дальше идёт с ним
     * в ногу — каждый чек и каждое внесение меняют обе. Пересчёт же
     * переписывал только сменную, и после него кассир видел в ящике одно,
     * а в журнале другое.
     *
     * Правится ровно поправка: глобальный счётчик сдвигается на ту же
     * величину, на которую пересчёт изменил сменный. Так остаток ящика,
     * накопленный прошлыми сменами, остаётся нетронутым.
     */
    private fun reconcileGlobalCash(kkmId: String, cashBefore: Long?, cashAfter: Long) {
        // Счётчика ещё не было: пересчёт ничего не исправлял, а завёл смену.
        if (cashBefore == null) return
        val correction = cashAfter - cashBefore
        if (correction == 0L) return
        val global = storage.loadCounters(kkmId, CounterScopes.GLOBAL, null)[CounterKeyFormats.CASH_SUM] ?: 0L
        storage.upsertCounter(kkmId, CounterScopes.GLOBAL, null, CounterKeyFormats.CASH_SUM, global + correction)
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

        // Сохраняем начальные значения смены из existing (стартовые суммы ОФД)
        existing.filterKeys { it.startsWith("start_shift_") }.forEach { (k, v) ->
            result[k] = v
        }

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

        val startCash = existing["start_shift_cash.sum"] ?: 0L
        result["start_shift_cash.sum"] = startCash
        result[CounterKeyFormats.CASH_SUM] = startCash

        var offset = 0
        val limit = 500
        // Постранично загружаем фискальные документы смены и применяем их к счетчикам
        while (true) {
            val docs = storage.listFiscalDocumentsByShift(kkmId, shift.id, limit = limit, offset = offset)
            if (docs.isEmpty()) break
            docs.filter { it.becameFiscal() }.forEach { doc ->
                when {
                    doc.docType in ReceiptDocumentTypes.ALL -> applyReceiptDocument(doc, result)
                    doc.docType == CashOperationType.CASH_IN.name ||
                        doc.docType == CashOperationType.CASH_OUT.name ->
                        applyCashOperationDocument(doc, result)
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
        val sumValue = request.total.tiyn()

        val totalItemDiscountTiyn = request.items.mapNotNull { it.discount?.tiyn() }.sum()
        val totalItemMarkupTiyn = request.items.mapNotNull { it.markup?.tiyn() }.sum()
        val discountTiyn = request.discount?.tiyn() ?: totalItemDiscountTiyn
        val markupTiyn = request.markup?.tiyn() ?: totalItemMarkupTiyn
        val changeTiyn = request.change?.tiyn() ?: 0L

        // Увеличиваем счетчики количества и сумм операций
        increment(counters, CounterKeyFormats.OPERATION_COUNT.format(operationKey), 1L)
        increment(counters, CounterKeyFormats.OPERATION_SUM.format(operationKey), sumValue)
        increment(counters, CounterKeyFormats.DISCOUNT_SUM.format(operationKey), discountTiyn)
        increment(counters, CounterKeyFormats.MARKUP_SUM.format(operationKey), markupTiyn)

        // Обновляем счетчики по секциям/отделам
        request.items.forEach { item ->
            val sectionCode = item.sectionCode.ifBlank { "001" }
            // Сторно вычитает из отдела сумму, но не число проданного:
            // так считает референс (`OperationCalculator.extractSections`
            // ставит сторно-позиции `count = 0`). Прежде сторно уменьшало
            // счётчик на единицу, и чек, где одну позицию сняли, а другую
            // продали, уходил в отчёт по отделу нулём — отдел показывал
            // шесть чеков там, где смена знала семь.
            val countDelta = if (item.isStorno) 0L else 1L
            val sumDelta = if (item.isStorno) -item.sum.tiyn() else item.sum.tiyn()
            increment(
                counters,
                CounterKeyFormats.SECTION_OPERATION_COUNT.format(sectionCode, operationKey),
                countDelta
            )
            increment(
                counters,
                CounterKeyFormats.SECTION_OPERATION_SUM.format(sectionCode, operationKey),
                sumDelta
            )
        }

        // Обновляем счетчики билетов/чеков
        increment(counters, CounterKeyFormats.TICKET_TOTAL_COUNT.format(operationKey), 1L)
        increment(counters, CounterKeyFormats.TICKET_COUNT.format(operationKey), 1L)
        increment(counters, CounterKeyFormats.TICKET_SUM.format(operationKey), sumValue)
        increment(counters, CounterKeyFormats.TICKET_DISCOUNT_SUM.format(operationKey), discountTiyn)
        increment(counters, CounterKeyFormats.TICKET_MARKUP_SUM.format(operationKey), markupTiyn)
        increment(counters, CounterKeyFormats.TICKET_CHANGE_SUM.format(operationKey), changeTiyn)
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
                PaymentType.CREDIT -> "PAYMENT_CREDIT"
                PaymentType.TARE -> "PAYMENT_TARE"
            }
            increment(
                counters,
                CounterKeyFormats.PAYMENT_SUM.format(operationKey, payKey),
                payment.sum.tiyn()
            )
            increment(
                counters,
                CounterKeyFormats.PAYMENT_COUNT.format(operationKey, payKey),
                1L
            )
        }

        // Обновляем счетчик наличных в денежном ящике (только для наличных платежей)
        // Знак операции для денежного ящика: продажа и возврат покупки кладут
        // наличные в кассу, возврат продажи и покупка — выдают их из кассы.
        // Совпадает с эталоном OperationCalculator.addTicket.
        val cashDirection = when (request.operation) {
            ReceiptOperationType.SELL, ReceiptOperationType.BUY_RETURN -> 1L
            ReceiptOperationType.SELL_RETURN, ReceiptOperationType.BUY -> -1L
        }
        // Наличные копятся в тиынах: суммирование одних целых тенге теряло
        // до тиына с каждого чека, и остаток ящика расходился с настоящим.
        val cashTiyn = cashDirection * request.payments
            .filter { it.type == PaymentType.CASH }
            .sumOf { it.sum.tiyn() }
        if (cashTiyn != 0L) {
            increment(counters, CounterKeyFormats.CASH_SUM, cashTiyn)
        }

        // Рассчитываем влияние на общую выручку ККМ. Знак у покупки тот же,
        // что у денежного ящика: покупка выдаёт деньги и выручку уменьшает,
        // возврат покупки возвращает их и увеличивает — как в эталоне
        // OperationCalculator.addTicket.
        val revenueDelta = when (request.operation) {
            ReceiptOperationType.SELL, ReceiptOperationType.BUY_RETURN -> sumValue
            ReceiptOperationType.SELL_RETURN, ReceiptOperationType.BUY -> -sumValue
        }
        if (revenueDelta != 0L) {
            val current = counters[CounterKeyFormats.REVENUE_SUM] ?: 0L
            counters[CounterKeyFormats.REVENUE_SUM] = current + revenueDelta
        }

        // Налоги чека по ставкам — тем же расчётом, что и при самом чеке
        taxCounterDeltas(request, operationKey).forEach { (key, delta) -> increment(counters, key, delta) }
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
