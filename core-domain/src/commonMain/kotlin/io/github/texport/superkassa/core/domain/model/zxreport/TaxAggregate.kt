package io.github.texport.superkassa.core.domain.model.zxreport

/**
 * Агрегированные налоговые показатели смены по конкретной ставке/группе НДС.
 *
 * @property taxType Внутренний идентификатор типа налога.
 * @property taxTypeCode Код типа налога для протокола ОФД.
 * @property percent Процентная ставка налога.
 * @property operations Список агрегированных налоговых показателей по типам фискальных операций.
 */
data class TaxAggregate(
    val taxType: Int,
    val taxTypeCode: String,
    val percent: Int,
    val operations: List<TaxOperationAggregate>
)
