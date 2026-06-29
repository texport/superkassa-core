package kz.mybrain.superkassa.core.domain.model.kkm

/**
 * Тип операции с наличными деньгами.
 */
enum class CashOperationType {
    /** Внесение денег в кассу. */
    CASH_IN,

    /** Изъятие денег из кассы. */
    CASH_OUT
}
