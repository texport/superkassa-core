package kz.mybrain.superkassa.core.domain.model.zxreport

import kotlinx.serialization.Serializable

/**
 * Входные агрегированные данные для построения X/Z-отчетов (ZxReport).
 *
 * @property dateTimeMillis Время формирования отчета (в миллисекундах).
 * @property shiftNumber Номер кассовой смены.
 * @property openShiftTimeMillis Время открытия кассовой смены (в миллисекундах).
 * @property closeShiftTimeMillis Время закрытия кассовой смены (в миллисекундах, null для X-отчета).
 * @property cashSumBills Текущая сумма наличных денег в кассе (в целых тенге).
 * @property revenueBills Целая часть суммы выручки за смену (тенге).
 * @property revenueCoins Дробная часть суммы выручки за смену (тиыны).
 * @property nonNullableSums Необнуляемые суммы на момент формирования отчета (накапливаемый итог).
 * @property startShiftNonNullableSums Необнуляемые суммы на момент открытия смены.
 * @property sections Агрегированные продажи по секциям (отделам).
 * @property operations Общие агрегированные показатели по фискальным операциям.
 * @property discounts Агрегированные суммы предоставленных скидок.
 * @property markups Агрегированные суммы начислений/наценок.
 * @property totalResult Общий финансовый итог смены.
 * @property ticketOperations Детализированные показатели по чекам в разрезе типов фискальных операций.
 * @property moneyPlacements Агрегированные операции внесения и изъятия наличных.
 * @property taxes Агрегированные налоговые показатели в разрезе ставок НДС.
 */
@Serializable
data class ZxReportInput(
    val dateTimeMillis: Long,
    val shiftNumber: Int,
    val openShiftTimeMillis: Long,
    val closeShiftTimeMillis: Long?,
    val cashSumBills: Long,
    val revenueBills: Long,
    val revenueCoins: Int,
    val nonNullableSums: List<Pair<String, Long>>,
    val startShiftNonNullableSums: List<Pair<String, Long>>,
    val sections: List<SectionAggregate>,
    val operations: List<OperationAggregate>,
    val discounts: List<OperationAggregate>,
    val markups: List<OperationAggregate>,
    val totalResult: List<OperationAggregate>,
    val ticketOperations: List<TicketOperationAggregate>,
    val moneyPlacements: List<MoneyPlacementAggregate>,
    val taxes: List<TaxAggregate>
)
