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
    MOBILE
}
