package io.github.texport.superkassa.core.presentation.api.model.receipt

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import io.github.texport.superkassa.core.presentation.api.annotations.Min
import io.github.texport.superkassa.core.presentation.api.annotations.NotBlank
import io.github.texport.superkassa.core.presentation.api.annotations.DecimalMin
import kotlinx.serialization.Serializable

/**
 * Данные исходного чека (для возвратов).
 */
@Serializable
@Schema(description = "Информация об исходном чеке для возврата")
data class ParentTicketRequest(
    @Schema(description = "Номер исходного чека", example = "123")
    @field:Min(1)
    val parentTicketNumber: Long,
    @Schema(
        description = "Дата и время исходного чека в формате ISO-8601 (UTC)",
        example = "2025-02-20T10:15:30Z"
    )
    @field:NotBlank(message = "Дата исходного чека обязательна")
    val parentTicketDateTime: String,
    @Schema(
        description = "Регистрационный номер ККМ (КГД), на которой был пробит исходный чек",
        example = "123456789012"
    )
    @field:NotBlank(message = "Идентификатор ККМ КГД обязателен")
    val kgdKkmId: String,
    @Schema(description = "Сумма исходного чека (в тенге)", example = "1500.75")
    @field:DecimalMin("0.01", message = "Сумма чека должна быть больше нуля")
    val parentTicketTotal: Double,
    @Schema(
        description = "Был ли исходный чек пробит в офлайн-режиме",
        example = "false"
    )
    val parentTicketIsOffline: Boolean
)
