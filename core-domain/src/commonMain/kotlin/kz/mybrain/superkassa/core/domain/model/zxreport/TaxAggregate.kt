package kz.mybrain.superkassa.core.domain.model.zxreport

import kotlinx.serialization.Serializable

/**
 * Агрегированные налоговые показатели смены по конкретной ставке/группе НДС.
 *
 * @property taxType Внутренний идентификатор типа налога.
 * @property taxTypeCode Код типа налога для протокола ОФД.
 * @property percent Процентная ставка налога.
 * @property operations Список агрегированных налоговых показателей по типам фискальных операций.
 */
@Serializable
data class TaxAggregate(
    val taxType: Int,
    val taxTypeCode: String,
    val percent: Int,
    val operations: List<TaxOperationAggregate>
)
