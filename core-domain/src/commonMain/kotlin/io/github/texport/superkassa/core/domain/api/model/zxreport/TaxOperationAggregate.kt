package io.github.texport.superkassa.core.domain.api.model.zxreport

/**
 * Налоговые обороты по конкретной фискальной операции за смену.
 *
 * @property operation Тип фискальной операции (продажа, возврат и т.д.).
 * @property turnoverBills Общий оборот в целых единицах валюты (тенге).
 * @property turnoverWithoutTaxBills Налоговый оборот без учета налога (тенге).
 * @property taxSumBills Накопленная сумма налога за смену (тенге).
 */
data class TaxOperationAggregate(
    val operation: String,
    val turnoverBills: Long,
    val turnoverWithoutTaxBills: Long,
    val taxSumBills: Long
)
