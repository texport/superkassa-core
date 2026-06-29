package kz.mybrain.superkassa.core.domain.helper.zxreport

import kz.mybrain.superkassa.core.domain.model.zxreport.ZxReportInput

/**
 * Оркестратор построения данных Zx-отчетов (Z-отчет или X-отчет) на основе счетчиков смены.
 *
 * Собирает и агрегирует различные логические блоки отчета:
 * - Сумму наличных;
 * - Сумму выручки;
 * - Накопительные итоги на начало и конец смены;
 * - Статистику по секциям и типам операций;
 * - Скидки, наценки и итоговые результаты;
 * - Операции с чеками и движение денег (внесения/изъятия);
 * - Налоги.
 */
object ZxReportBuilder {

    /**
     * Строит агрегированные значения для Zx-отчета на основе карты счетчиков смены.
     *
     * @param counters Карта ключ/значение счетчиков для конкретной смены (scope = SHIFT).
     * @param dateTimeMillis Время формирования отчета в миллисекундах.
     * @param shiftNumber Номер текущей смены.
     * @param openShiftTimeMillis Время открытия смены в миллисекундах.
     * @param closeShiftTimeMillis Время закрытия смены в миллисекундах (null для X-отчета, непустое для Z-отчета).
     * @return Объект [ZxReportInput], содержащий всю агрегированную информацию для генерации отчета.
     */
    fun build(
        counters: Map<String, Long>,
        dateTimeMillis: Long,
        shiftNumber: Int,
        openShiftTimeMillis: Long,
        closeShiftTimeMillis: Long?
    ): ZxReportInput {
        val cashSumBills = ZxCashBlockBuilder.resolveCashSum(counters)
        val (revenueBills, revenueCoins) = ZxOperationsBlockBuilder.resolveRevenue(counters)
        val nonNullable = ZxCashBlockBuilder.resolveNonNullableSums(counters)
        val startShiftNonNullable = ZxCashBlockBuilder.resolveStartShiftNonNullableSums(counters)

        val operations = ZxOperationsBlockBuilder.resolveOperations(counters)
        val sections = ZxOperationsBlockBuilder.resolveSections(counters, operations)
        val discounts = ZxOperationsBlockBuilder.resolveDiscounts(counters)
        val markups = ZxOperationsBlockBuilder.resolveMarkups(counters)
        val totalResult = ZxOperationsBlockBuilder.resolveTotalResult(counters)

        val ticketOperations = ZxTicketOperationsBlockBuilder.resolveTicketOperations(counters)
        val moneyPlacements = ZxMoneyPlacementsBlockBuilder.build(counters)
        val taxes = ZxTaxBlockBuilder.resolveTaxes(counters)

        return ZxReportInput(
            dateTimeMillis = dateTimeMillis,
            shiftNumber = shiftNumber,
            openShiftTimeMillis = openShiftTimeMillis,
            closeShiftTimeMillis = closeShiftTimeMillis,
            cashSumBills = cashSumBills,
            revenueBills = revenueBills,
            revenueCoins = revenueCoins,
            nonNullableSums = nonNullable,
            startShiftNonNullableSums = startShiftNonNullable,
            sections = sections,
            operations = operations,
            discounts = discounts,
            markups = markups,
            totalResult = totalResult,
            ticketOperations = ticketOperations,
            moneyPlacements = moneyPlacements,
            taxes = taxes
        )
    }
}
