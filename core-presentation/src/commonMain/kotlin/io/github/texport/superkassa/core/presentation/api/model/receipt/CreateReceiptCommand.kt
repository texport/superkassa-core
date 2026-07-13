package io.github.texport.superkassa.core.presentation.api.model.receipt

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import io.github.texport.superkassa.core.presentation.api.annotations.NotBlank
import io.github.texport.superkassa.core.presentation.api.annotations.NotEmpty
import io.github.texport.superkassa.core.presentation.api.annotations.Valid
import io.github.texport.superkassa.core.presentation.api.annotations.DecimalMin
import io.github.texport.superkassa.core.presentation.api.annotations.DecimalMax
import kotlinx.serialization.Serializable

/**
 * Команда для создания фискального чека.
 */
@Serializable
@Schema(description = "Команда создания и фискализации чека")
data class CreateReceiptCommand(
    @Schema(description = "Уникальный идентификатор ККМ", example = "kkm-uuid-12345")
    @field:NotBlank(message = "kkmId обязателен")
    val kkmId: String,
    @Schema(description = "ПИН-код кассира/администратора", example = "1234")
    @field:NotBlank(message = "pin обязателен")
    val pin: String,
    @Schema(description = "Тип операции чека (SELL, SELL_RETURN, BUY, BUY_RETURN)", example = "SELL")
    @field:NotBlank(message = "operation обязательна")
    val operation: String,
    @Schema(description = "Ключ идемпотентности для исключения дублирования чеков", example = "idemp-key-abc-123")
    @field:NotBlank(message = "idempotencyKey обязателен")
    val idempotencyKey: String,
    @Schema(description = "Список позиций чека")
    @field:NotEmpty(message = "Список позиций не должен быть пустым")
    val items: List<@Valid ReceiptItemRequest>,
    @Schema(description = "Процент скидки на чек (0-100)", example = "5.0")
    @field:DecimalMin("0.0")
    @field:DecimalMax("100.0")
    val discountPercent: Double?,
    @Schema(description = "Сумма скидки на чек", example = "150.0")
    @field:DecimalMin("0.0")
    val discountSum: Double?,
    @Schema(description = "Процент наценки на чек (0-100)", example = "0.0")
    @field:DecimalMin("0.0")
    @field:DecimalMax("100.0")
    val markupPercent: Double?,
    @Schema(description = "Сумма наценки на чек", example = "0.0")
    @field:DecimalMin("0.0")
    val markupSum: Double?,
    @Schema(description = "Способы оплаты чека")
    @field:NotEmpty(message = "Укажите хотя бы один способ оплаты")
    val payments: List<@Valid ReceiptPaymentRequest>,
    @Schema(description = "Сумма полученных средств от покупателя", example = "5000.0")
    @field:DecimalMin("0.0")
    val taken: Double?,
    @Schema(description = "Информация об исходном чеке (для чеков возврата)")
    val parentTicket: ParentTicketRequest? = null,
    @Schema(description = "Группа НДС по умолчанию для чека (NO_VAT, VAT_0, VAT_5, VAT_10, VAT_16)", example = "NO_VAT")
    val defaultVatGroup: String? = null,
    @Schema(description = "БИН/ИИН покупателя (для юридических лиц)", example = "123456789012")
    val customerBin: String? = null
)
