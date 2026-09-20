package io.github.texport.superkassa.core.domain.api.model.zxreport

/**
 * Налоговые обороты по конкретной фискальной операции за смену.
 *
 * @property operation Тип фискальной операции (продажа, возврат и т.д.).
 * @property turnoverTiyn Общий оборот в целых единицах валюты (тенге).
 * @property turnoverWithoutTaxTiyn Налоговый оборот без учета налога (тенге).
 * @property taxSumTiyn Накопленная сумма налога за смену (тенге).
 */
data class TaxOperationAggregate(
    val operation: String,
    val turnoverTiyn: Long,
    val turnoverWithoutTaxTiyn: Long,
    val taxSumTiyn: Long
)
