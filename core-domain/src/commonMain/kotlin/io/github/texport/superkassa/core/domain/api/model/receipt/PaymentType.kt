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
    MOBILE,

    /** Устарел с версии 203, но принимается кассой 2.0.2 и 2.0.3. */
    CREDIT,

    /** Оплата тарой. Устарел с версии 203, схема 2.0.4 его не содержит. */
    TARE;

    companion object {
        /**
         * Виды оплаты, которые поддерживает версия протокола.
         *
         * Оплата в кредит и тарой объявлены устаревшими и в схеме 2.0.4
         * отсутствуют. Принять такой чек значило бы оформить документ,
         * который невозможно доставить ни сейчас, ни повтором.
         */
        fun supportedBy(protocolVersion: String): Set<PaymentType> =
            if (protocolVersion.startsWith("204")) {
                entries.toSet() - setOf(CREDIT, TARE)
            } else {
                entries.toSet()
            }
    }
}
