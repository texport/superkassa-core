package io.github.texport.superkassa.core.domain.api.model.zxreport

/**
 * Агрегированная сумма и количество фискальных операций определенного типа за смену.
 *
 * @property operation Тип фискальной операции.
 * @property count Количество операций.
 * @property sumTiyn Общая сумма операций в целых единицах валюты (тенге).
 */
data class OperationAggregate(
    val operation: String,
    val count: Long,
    val sumTiyn: Long
)
