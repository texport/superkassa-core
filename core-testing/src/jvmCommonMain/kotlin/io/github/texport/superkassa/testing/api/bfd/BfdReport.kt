package io.github.texport.superkassa.testing.api.bfd

import kz.kazakhtelecom.proto.v203.MoneyPlacementEnum
import kz.kazakhtelecom.proto.v203.OperationTypeEnum
import kz.kazakhtelecom.proto.v203.PaymentTypeEnum

/**
 * Итоги смены X/Z-отчёта в сравнимом виде: всё, что отчёт считает, в тиынах
 * и штуках, без нулевых строк и без порядка строк.
 *
 * Нулевая строка у БФД и у кассы значит одно и то же — «такого не было»:
 * референс строк для небывших операций не заводит, касса пишет их нулями.
 * Время, номер смены и контрольная сумма — реквизиты документа, а не итоги,
 * и сюда не входят.
 *
 * @property sections итоги по отделам: код отдела — итоги операций.
 * @property operations число позиций и их сумма до скидок и наценок.
 * @property discounts число скидок (на чек и на позиции) и их сумма.
 * @property markups число наценок (на чек и на позиции) и их сумма.
 * @property totalResult число позиций и сумма со скидками и наценками.
 * @property tickets итоги по чекам вида операции.
 * @property placements итоги по внесениям и изъятиям.
 * @property taxes налоги: вид и ставка — итоги операций.
 * @property nonNullable необнуляемые суммы на конец отчёта.
 * @property startShiftNonNullable необнуляемые суммы на начало смены.
 * @property cashTiyn наличные в ящике.
 * @property revenueTiyn выручка со знаком.
 */
data class BfdReport(
    val sections: Map<String, Map<OperationTypeEnum, Tally>> = emptyMap(),
    val operations: Map<OperationTypeEnum, Tally> = emptyMap(),
    val discounts: Map<OperationTypeEnum, Tally> = emptyMap(),
    val markups: Map<OperationTypeEnum, Tally> = emptyMap(),
    val totalResult: Map<OperationTypeEnum, Tally> = emptyMap(),
    val tickets: Map<OperationTypeEnum, TicketTally> = emptyMap(),
    val placements: Map<MoneyPlacementEnum, PlacementTally> = emptyMap(),
    val taxes: Map<Pair<Int, Int>, Map<OperationTypeEnum, TaxTally>> = emptyMap(),
    val nonNullable: Map<OperationTypeEnum, Long> = emptyMap(),
    val startShiftNonNullable: Map<OperationTypeEnum, Long> = emptyMap(),
    val cashTiyn: Long = 0,
    val revenueTiyn: Long = 0
) {
    /** Число и сумма строки операции. */
    data class Tally(val count: Long, val sumTiyn: Long)

    /**
     * Строка чеков вида операции.
     *
     * @property totalCount чеков за всё время: переходит из смены в смену.
     * @property count чеков за смену.
     * @property payments оплаты по видам: число оплат и их сумма.
     */
    data class TicketTally(
        val totalCount: Long,
        val count: Long,
        val sumTiyn: Long,
        val payments: Map<PaymentTypeEnum, Tally>,
        val offlineCount: Long,
        val discountTiyn: Long,
        val markupTiyn: Long,
        val changeTiyn: Long
    )

    /**
     * Строка внесений или изъятий.
     *
     * @property totalCount операций за всё время: переходит из смены в смену.
     * @property count операций за смену.
     */
    data class PlacementTally(val totalCount: Long, val count: Long, val sumTiyn: Long, val offlineCount: Long)

    /** Оборот, налог и оборот без налога операции по ставке. */
    data class TaxTally(val turnoverTiyn: Long, val sumTiyn: Long, val turnoverWithoutTaxTiyn: Long)
}
