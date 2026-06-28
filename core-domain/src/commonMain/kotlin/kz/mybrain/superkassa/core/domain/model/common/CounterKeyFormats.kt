package kz.mybrain.superkassa.core.domain.model.common

/**
 * Константы форматов ключей счетчиков ККМ.
 * Используются для хранения и обновления агрегированных показателей продаж, оплат, налогов и т.д.
 */
object CounterKeyFormats {
    /** Количество операций определенного типа. */
    const val OPERATION_COUNT = "operation.%s.count"
    /** Сумма операций определенного типа. */
    const val OPERATION_SUM = "operation.%s.sum"
    /** Сумма скидок для операций определенного типа. */
    const val DISCOUNT_SUM = "operation.%s.discount_sum"
    /** Сумма наценок для операций определенного типа. */
    const val MARKUP_SUM = "operation.%s.markup_sum"

    /** Общее количество сформированных чеков определенного типа. */
    const val TICKET_TOTAL_COUNT = "ticket.%s.total_count"
    /** Количество успешно проведенных чеков определенного типа. */
    const val TICKET_COUNT = "ticket.%s.count"
    /** Сумма проведенных чеков определенного типа. */
    const val TICKET_SUM = "ticket.%s.sum"
    /** Количество чеков определенного типа, отправленных в офлайн-режиме. */
    const val TICKET_OFFLINE_COUNT = "ticket.%s.offline_count"
    /** Сумма скидок по чекам определенного типа. */
    const val TICKET_DISCOUNT_SUM = "ticket.%s.discount_sum"
    /** Сумма наценок по чекам определенного типа. */
    const val TICKET_MARKUP_SUM = "ticket.%s.markup_sum"
    /** Сумма выданной сдачи по чекам определенного типа. */
    const val TICKET_CHANGE_SUM = "ticket.%s.change_sum"

    /** Сумма оплат определенным типом платежа для чеков определенного типа. */
    const val PAYMENT_SUM = "ticket.%s.payment.%s.sum"
    /** Количество платежей определенным типом оплат для чеков определенного типа. */
    const val PAYMENT_COUNT = "ticket.%s.payment.%s.count"

    /** Количество операций определенного типа в разрезе товарной секции. */
    const val SECTION_OPERATION_COUNT = "section.%s.operation.%s.count"
    /** Сумма операций определенного типа в разрезе товарной секции. */
    const val SECTION_OPERATION_SUM = "section.%s.operation.%s.sum"

    /** Количество операций внесения/изъятия денег. */
    const val MONEY_PLACEMENT_COUNT = "money_placement.%s.count"
    /** Общее количество операций внесения/изъятия денег. */
    const val MONEY_PLACEMENT_TOTAL_COUNT = "money_placement.%s.total_count"
    /** Сумма внесенных/изъятых денег. */
    const val MONEY_PLACEMENT_SUM = "money_placement.%s.sum"
    /** Количество операций внесения/изъятия денег, проведенных офлайн. */
    const val MONEY_PLACEMENT_OFFLINE_COUNT = "money_placement.%s.offline_count"

    /** Налоговый оборот для определенного типа операции и группы НДС. */
    const val TAX_TURNOVER = "tax.%s.%s.turnover"
    /** Сумма налога для определенного типа операции и группы НДС. */
    const val TAX_SUM = "tax.%s.%s.sum"
    /** Налоговый оборот без учета налога для определенного типа операции и группы НДС. */
    const val TAX_TURNOVER_NO_TAX = "tax.%s.%s.turnover_without_tax"

    /** Необнуляемая сумма (накапливаемый итог) по типу операции. */
    const val NON_NULLABLE_SUM = "non_nullable.%s.sum"
    /** Стартовая необнуляемая сумма на начало смены по типу операции. */
    const val START_SHIFT_NON_NULLABLE_SUM = "start_shift_non_nullable.%s.sum"

    /** Текущая сумма наличных денег в кассе. */
    const val CASH_SUM = "cash.sum"
    /** Выручка за смену. */
    const val REVENUE_SUM = "revenue.sum"
    /** Флаг, указывающий на отрицательную выручку за смену. */
    const val REVENUE_IS_NEGATIVE = "revenue.is_negative"
}

/**
 * Кроссплатформенный хелпер для форматирования строк вида %s в KMP.
 */
fun String.format(vararg args: Any?): String {
    var result = this
    val regex = Regex("%[sdf]")
    for (arg in args) {
        result = result.replaceFirst(regex, arg?.toString() ?: "null")
    }
    return result
}

