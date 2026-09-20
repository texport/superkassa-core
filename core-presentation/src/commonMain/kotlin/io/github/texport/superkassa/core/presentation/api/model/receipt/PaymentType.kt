package io.github.texport.superkassa.core.presentation.api.model.receipt

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Типы оплат фискальных чеков.
 */
@Serializable
@Schema(description = "Тип оплаты")
enum class PaymentType {
    /** Наличные средства */
    @Schema(description = "Наличные")
    CASH,

    /** Банковская платежная карта */
    @Schema(description = "Карта")
    CARD,

    /** Электронные деньги */
    @Schema(description = "Электронные")
    ELECTRONIC,

    /** Мобильный QR-перевод */
    @Schema(description = "Мобильный перевод")
    MOBILE,

    /** Устарел с версии 203, но принимается кассой 2.0.2 и 2.0.3. */
    CREDIT,

    /** Оплата тарой. Устарел с версии 203, схема 2.0.4 его не содержит. */
    TARE
}
