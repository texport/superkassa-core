package kz.mybrain.superkassa.core.domain.model.receipt

import kotlinx.serialization.Serializable

/**
 * Типы фискальных операций чека (продажа, возврат, покупка, возврат покупки).
 */
@Serializable
enum class ReceiptOperationType {
    /** Продажа товара/услуги клиенту. */
    SELL,
    /** Возврат товара/услуги от клиента. */
    SELL_RETURN,
    /** Покупка (прием товара/услуги у клиента). */
    BUY,
    /** Возврат совершенной покупки клиенту. */
    BUY_RETURN
}
