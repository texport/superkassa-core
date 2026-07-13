package io.github.texport.superkassa.core.presentation.api.model.kkm

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import io.github.texport.superkassa.core.presentation.api.annotations.DecimalMin
import io.github.texport.superkassa.core.presentation.api.annotations.NotBlank
import kotlinx.serialization.Serializable

/**
 * Запрос на проведение операции с наличными (внесение/изъятие).
 */
@Serializable
@Schema(description = "Запрос на операцию с наличными")
data class CashOperationRequest(
    @Schema(description = "Сумма операции в тенге", example = "5000.00")
    @field:DecimalMin("0.01", message = "Сумма операции должна быть положительной")
    val amount: Double,
    @Schema(description = "Ключ идемпотентности", example = "unique-id-123")
    @field:NotBlank(message = "Ключ идемпотентности обязателен")
    val idempotencyKey: String
)
