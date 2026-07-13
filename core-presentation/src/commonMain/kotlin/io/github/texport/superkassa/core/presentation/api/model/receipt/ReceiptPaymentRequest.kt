package io.github.texport.superkassa.core.presentation.api.model.receipt

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import io.github.texport.superkassa.core.presentation.api.annotations.DecimalMin
import io.github.texport.superkassa.core.presentation.api.annotations.NotNull
import io.github.texport.superkassa.core.presentation.api.annotations.NotBlank
import kotlinx.serialization.Serializable

/**
 * Способ оплаты чека.
 */
@Serializable
@Schema(description = "Способ оплаты чека")
data class ReceiptPaymentRequest(
    @Schema(
        description = "Тип оплаты. Допустимые значения: CASH (наличные), CARD (карта), ELECTRONIC (электронные средства).",
        allowableValues = ["CASH", "CARD", "ELECTRONIC"],
        example = "CASH"
    )
    @field:NotBlank(message = "Тип оплаты обязателен")
    val type: String,
    @Schema(description = "Сумма оплаты (в тенге)", example = "500.00")
    @field:NotNull
    @field:DecimalMin("0", message = "Сумма оплаты не может быть отрицательной")
    val sum: Double
)
