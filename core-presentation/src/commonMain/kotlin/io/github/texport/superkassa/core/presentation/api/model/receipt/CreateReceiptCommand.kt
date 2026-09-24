package io.github.texport.superkassa.core.presentation.api.model.receipt

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
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
    val discountPercent: Decimal?,
    @Schema(description = "Сумма скидки на чек", example = "150.0")
    @field:DecimalMin("0.0")
    val discountSum: Decimal?,
    @Schema(description = "Процент наценки на чек (0-100)", example = "0.0")
    @field:DecimalMin("0.0")
    @field:DecimalMax("100.0")
    val markupPercent: Decimal?,
    @Schema(description = "Сумма наценки на чек", example = "0.0")
    @field:DecimalMin("0.0")
    val markupSum: Decimal?,
    @Schema(description = "Способы оплаты чека")
    @field:NotEmpty(message = "Укажите хотя бы один способ оплаты")
    val payments: List<@Valid ReceiptPaymentRequest>,
    @Schema(description = "Сумма полученных средств от покупателя", example = "5000.0")
    @field:DecimalMin("0.0")
    val taken: Decimal?,
    @Schema(description = "Информация об исходном чеке (для чеков возврата)")
    val parentTicket: ParentTicketRequest? = null,
    /** Отраслевые реквизиты чека. */
    val domain: ReceiptDomainRequest? = null,
    @Schema(
        description = "НДС на весь чек: одна ставка на весь итог чека. Допустимые значения: NO_VAT, VAT_0, VAT_5, " +
            "VAT_10, VAT_12, VAT_16. Взаимоисключающе со ставками позиций (vatGroup позиции), как скидка на чек " +
            "и на позиции: заданы оба — отказ RECEIPT_VAT_SCOPES_CONFLICT. Не указана — НДС по позициям, " +
            "позиция без ставки облагается ставкой кассы по умолчанию.",
        example = "VAT_16"
    )
    val vatGroup: String? = null,
    @Schema(description = "БИН/ИИН покупателя (для юридических лиц)", example = "123456789012")
    val customerBin: String? = null,
    @Schema(description = "Контакт покупателя: по нему ему уходит чек в подходящий включённый канал доставки")
    val customerContact: CustomerContactRequest? = null
)
