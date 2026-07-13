package io.github.texport.superkassa.core.domain.api.model.receipt

/**
 * Типы принимаемых оплат (соответствует протоколу ОФД: PAYMENT_CASH, PAYMENT_CARD, PAYMENT_ELECTRONIC).
 */
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
