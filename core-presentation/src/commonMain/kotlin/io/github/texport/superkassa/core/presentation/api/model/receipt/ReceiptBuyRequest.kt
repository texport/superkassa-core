package io.github.texport.superkassa.core.presentation.api.model.receipt

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import io.github.texport.superkassa.core.presentation.api.annotations.Valid
import io.github.texport.superkassa.core.presentation.api.annotations.DecimalMax
import io.github.texport.superkassa.core.presentation.api.annotations.DecimalMin
import io.github.texport.superkassa.core.presentation.api.annotations.NotBlank
import io.github.texport.superkassa.core.presentation.api.annotations.NotEmpty
import kotlinx.serialization.Serializable

/**
 * Запрос на создание чека покупки.
 */
@Serializable
@Schema(description = "Запрос на создание чека покупки. Сумма чека и суммы позиций вычисляются на сервере.")
data class ReceiptBuyRequest(
    @Schema(description = "Ключ идемпотентности", example = "unique-key-123")
    @field:NotBlank
    val idempotencyKey: String,
    @Schema(description = "Позиции чека")
    @field:NotEmpty(message = "Список позиций не может быть пустым")
    val items: List<@Valid ReceiptItemRequest>,
    @Schema(description = "Наценка на весь чек: процент (0–100). Взаимоисключающе с markupSum.", example = "0")
    @field:DecimalMin("0")
    @field:DecimalMax("100")
    val markupPercent: Decimal? = null,
    @Schema(description = "Наценка на весь чек: сумма в тенге. Взаимоисключающе с markupPercent.", example = "0")
    @field:DecimalMin("0")
    val markupSum: Decimal? = null,
    @Schema(description = "Скидка на весь чек: процент (0–100). Взаимоисключающе с discountSum.", example = "5")
    @field:DecimalMin("0")
    @field:DecimalMax("100")
    val discountPercent: Decimal? = null,
    @Schema(description = "Скидка на весь чек: сумма в тенге. Взаимоисключающе с discountPercent.", example = "50.00")
    @field:DecimalMin("0")
    val discountSum: Decimal? = null,
    @Schema(description = "Сдача (в тенге, опционально)", example = "499.25")
    val change: Decimal? = null,
    @Schema(
        description = "НДС на весь чек: одна ставка на весь итог чека. Допустимые значения: NO_VAT, VAT_0, VAT_5, " +
            "VAT_10, VAT_12, VAT_16. Взаимоисключающе со ставками позиций (vatGroup позиции), как скидка на чек " +
            "и на позиции: заданы оба — отказ RECEIPT_VAT_SCOPES_CONFLICT. Не указана — НДС по позициям, " +
            "позиция без ставки облагается ставкой кассы по умолчанию.",
        example = "VAT_16"
    )
    val vatGroup: String? = null,
    @Schema(description = "Способы оплаты. Допустимые типы: CASH, CARD, ELECTRONIC.")
    @field:NotEmpty(message = "Укажите хотя бы один способ оплаты")
    val payments: List<@Valid ReceiptPaymentRequest>,
    /** Отраслевые реквизиты чека. */
    val domain: ReceiptDomainRequest? = null,
    @Schema(description = "Получено от покупателя (в тенге, опционально)", example = "2000.00")
    val taken: Decimal? = null,
    @Schema(description = "БИН/ИИН покупателя (по требованию)", example = "123456789012")
    val customerBin: String? = null
) {
    init {
        require(discountPercent == null || discountSum == null) {
            "Укажите скидку на чек либо в процентах (discountPercent), либо суммой (discountSum), но не оба значения"
        }
        require(markupPercent == null || markupSum == null) {
            "Укажите наценку на чек либо в процентах (markupPercent), либо суммой (markupSum), но не оба значения"
        }
    }
}
