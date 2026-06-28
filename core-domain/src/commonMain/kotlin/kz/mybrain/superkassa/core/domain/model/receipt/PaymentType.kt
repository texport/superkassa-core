package kz.mybrain.superkassa.core.domain.model.receipt

import kotlinx.serialization.Serializable

/**
 * Типы принимаемых оплат (соответствует протоколу ОФД: PAYMENT_CASH, PAYMENT_CARD, PAYMENT_ELECTRONIC).
 */
@Serializable
enum class PaymentType {
    /** Наличные средства. */
    CASH,
    /** Платежная карта. */
    CARD,
    /** Электронные деньги. */
    ELECTRONIC,
    /** Мобильный платеж (например, по QR-коду). */
    MOBILE
}
