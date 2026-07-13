package io.github.texport.superkassa.core.domain.api.model.zxreport

/**
 * Агрегированные показатели операций внесения и изъятия наличных за смену.
 *
 * @property operation Тип операции (внесение/изъятие).
 * @property operationsTotalCount Общее количество инициированных операций.
 * @property operationsCount Количество успешно завершенных операций.
 * @property operationsSumBills Сумма операций в целых единицах валюты (тенге).
 * @property offlineCount Количество операций, проведенных в автономном (офлайн) режиме.
 */
data class MoneyPlacementAggregate(
    val operation: String,
    val operationsTotalCount: Long,
    val operationsCount: Long,
    val operationsSumBills: Long,
    val offlineCount: Long
)
