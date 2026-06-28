package kz.mybrain.superkassa.core.domain.model.zxreport

import kotlinx.serialization.Serializable

/**
 * Агрегированная сумма и количество фискальных операций определенного типа за смену.
 *
 * @property operation Тип фискальной операции.
 * @property count Количество операций.
 * @property sumBills Общая сумма операций в целых единицах валюты (тенге).
 */
@Serializable
data class OperationAggregate(
    val operation: String,
    val count: Long,
    val sumBills: Long
)
