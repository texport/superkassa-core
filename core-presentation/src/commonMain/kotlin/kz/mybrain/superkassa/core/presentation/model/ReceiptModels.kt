package kz.mybrain.superkassa.core.presentation.model

import kz.mybrain.superkassa.core.presentation.annotations.Schema
import kz.mybrain.superkassa.core.presentation.annotations.Valid
import kz.mybrain.superkassa.core.presentation.annotations.DecimalMax
import kz.mybrain.superkassa.core.presentation.annotations.DecimalMin
import kz.mybrain.superkassa.core.presentation.annotations.Max
import kz.mybrain.superkassa.core.presentation.annotations.NotBlank
import kz.mybrain.superkassa.core.presentation.annotations.NotEmpty
import kz.mybrain.superkassa.core.presentation.annotations.NotNull
import kz.mybrain.superkassa.core.presentation.annotations.Positive
import kz.mybrain.superkassa.core.presentation.annotations.Size
import kotlinx.serialization.Serializable
import kz.mybrain.superkassa.core.domain.annotations.ItemNameValid

/**
 * Запрос на создание чека продажи.
 *
 * @property idempotencyKey Ключ идемпотентности.
 * @property items Список позиций чека.
 * @property markupPercent Наценка в процентах на весь чек.
 * @property markupSum Наценка суммой на весь чек.
 * @property discountPercent Скидка в процентах на весь чек.
 * @property discountSum Скидка суммой на весь чек.
 * @property change Сдача.
 * @property defaultVatGroup Группа НДС по умолчанию.
 * @property payments Способы оплаты чека.
 * @property taken Получено от клиента.
 * @property customerBin БИН/ИИН покупателя.
 */
@Serializable
@Schema(description = "Запрос на создание чека продажи. Сумма чека и суммы позиций вычисляются на сервере.")
data class ReceiptSellRequest(
    @Schema(description = "Ключ идемпотентности для предотвращения дублирования операций", example = "unique-key-123")
    @field:NotBlank
    val idempotencyKey: String,
    @Schema(description = "Позиции чека")
    @field:NotEmpty(message = "Список позиций не может быть пустым")
    val items: List<@Valid ReceiptItemDto>,
    @Schema(description = "Наценка на весь чек: процент (0–100). Взаимоисключающе с markupSum.", example = "0")
    @field:DecimalMin("0")
    @field:DecimalMax("100")
    val markupPercent: Double? = null,
    @Schema(description = "Наценка на весь чек: сумма в тенге. Взаимоисключающе с markupPercent.", example = "0")
    @field:DecimalMin("0")
    val markupSum: Double? = null,
    @Schema(description = "Скидка на весь чек: процент (0–100). Взаимоисключающе с discountSum.", example = "5")
    @field:DecimalMin("0")
    @field:DecimalMax("100")
    val discountPercent: Double? = null,
    @Schema(description = "Скидка на весь чек: сумма в тенге. Взаимоисключающе с discountPercent.", example = "50.00")
    @field:DecimalMin("0")
    val discountSum: Double? = null,
    @Schema(description = "Сдача (в тенге, опционально)", example = "499.25")
    val change: Double? = null,
    @Schema(
        description = "Группа НДС на весь чек. Если не указана — используется настройка ККМ (defaultVatGroup). " +
            "Допустимые значения: NO_VAT, VAT_0, VAT_5, VAT_10, VAT_16. Отдельная позиция может переопределить через vatGroup.",
        example = "NO_VAT"
    )
    val defaultVatGroup: String? = null,
    @Schema(description = "Способы оплаты. Допустимые типы: CASH, CARD, ELECTRONIC.")
    @field:NotEmpty(message = "Укажите хотя бы один способ оплаты")
    val payments: List<@Valid ReceiptPaymentDto>,
    @Schema(description = "Получено от покупателя (в тенге, опционально)", example = "2000.00")
    val taken: Double? = null,
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

/**
 * Запрос на создание чека возврата продажи.
 *
 * @property idempotencyKey Ключ идемпотентности.
 * @property items Список позиций чека.
 * @property markupPercent Наценка в процентах на весь чек.
 * @property markupSum Наценка суммой на весь чек.
 * @property discountPercent Скидка в процентах на весь чек.
 * @property discountSum Скидка суммой на весь чек.
 * @property change Сдача.
 * @property defaultVatGroup Группа НДС по умолчанию.
 * @property payments Способы оплаты.
 * @property taken Получено от клиента.
 * @property parentTicket Сведения о чеке-основании возврата.
 * @property customerBin БИН/ИИН покупателя.
 */
@Serializable
@Schema(description = "Запрос на создание чека возврата продажи. Сумма чека и суммы позиций вычисляются на сервере.")
data class ReceiptSellReturnRequest(
    @Schema(description = "Ключ идемпотентности", example = "unique-key-123")
    @field:NotBlank
    val idempotencyKey: String,
    @Schema(description = "Позиции чека")
    @field:NotEmpty(message = "Список позиций не может быть пустым")
    val items: List<@Valid ReceiptItemDto>,
    @Schema(description = "Наценка на весь чек: процент (0–100). Взаимоисключающе с markupSum.", example = "0")
    @field:DecimalMin("0")
    @field:DecimalMax("100")
    val markupPercent: Double? = null,
    @Schema(description = "Наценка на весь чек: сумма в тенге. Взаимоисключающе с markupPercent.", example = "0")
    @field:DecimalMin("0")
    val markupSum: Double? = null,
    @Schema(description = "Скидка на весь чек: процент (0–100). Взаимоисключающе с discountSum.", example = "5")
    @field:DecimalMin("0")
    @field:DecimalMax("100")
    val discountPercent: Double? = null,
    @Schema(description = "Скидка на весь чек: сумма в тенге. Взаимоисключающе с discountPercent.", example = "50.00")
    @field:DecimalMin("0")
    val discountSum: Double? = null,
    @Schema(description = "Сдача (в тенге, опционально)", example = "499.25")
    val change: Double? = null,
    @Schema(
        description = "Группа НДС на весь чек. Если не указана — используется настройка ККМ. NO_VAT, VAT_0, VAT_5, VAT_10, VAT_16.",
        example = "NO_VAT"
    )
    val defaultVatGroup: String? = null,
    @Schema(description = "Способы оплаты. Допустимые типы: CASH, CARD, ELECTRONIC.")
    @field:NotEmpty(message = "Укажите хотя бы один способ оплаты")
    val payments: List<@Valid ReceiptPaymentDto>,
    @Schema(description = "Получено от покупателя (в тенге, опционально)", example = "2000.00")
    val taken: Double? = null,
    @Schema(description = "Информация об исходном чеке для возврата (parentTicket)", required = false)
    val parentTicket: ParentTicketDto? = null,
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

/**
 * Запрос на создание чека покупки.
 *
 * @property idempotencyKey Ключ идемпотентности.
 * @property items Список позиций.
 * @property markupPercent Наценка в процентах.
 * @property markupSum Наценка суммой.
 * @property discountPercent Скидка в процентах.
 * @property discountSum Скидка суммой.
 * @property change Сдача.
 * @property defaultVatGroup Группа НДС.
 * @property payments Способы оплаты.
 * @property taken Получено.
 * @property customerBin БИН/ИИН покупателя.
 */
@Serializable
@Schema(description = "Запрос на создание чека покупки. Сумма чека и суммы позиций вычисляются на сервере.")
data class ReceiptBuyRequest(
    @Schema(description = "Ключ идемпотентности", example = "unique-key-123")
    @field:NotBlank
    val idempotencyKey: String,
    @Schema(description = "Позиции чека")
    @field:NotEmpty(message = "Список позиций не может быть пустым")
    val items: List<@Valid ReceiptItemDto>,
    @Schema(description = "Наценка на весь чек: процент (0–100). Взаимоисключающе с markupSum.", example = "0")
    @field:DecimalMin("0")
    @field:DecimalMax("100")
    val markupPercent: Double? = null,
    @Schema(description = "Наценка на весь чек: сумма в тенге. Взаимоисключающе с markupPercent.", example = "0")
    @field:DecimalMin("0")
    val markupSum: Double? = null,
    @Schema(description = "Скидка на весь чек: процент (0–100). Взаимоисключающе с discountSum.", example = "5")
    @field:DecimalMin("0")
    @field:DecimalMax("100")
    val discountPercent: Double? = null,
    @Schema(description = "Скидка на весь чек: сумма в тенге. Взаимоисключающе с discountPercent.", example = "50.00")
    @field:DecimalMin("0")
    val discountSum: Double? = null,
    @Schema(description = "Сдача (в тенге, опционально)", example = "499.25")
    val change: Double? = null,
    @Schema(
        description = "Группа НДС на весь чек. Если не указана — используется настройка ККМ. NO_VAT, VAT_0, VAT_5, VAT_10, VAT_16.",
        example = "NO_VAT"
    )
    val defaultVatGroup: String? = null,
    @Schema(description = "Способы оплаты. Допустимые типы: CASH, CARD, ELECTRONIC.")
    @field:NotEmpty(message = "Укажите хотя бы один способ оплаты")
    val payments: List<@Valid ReceiptPaymentDto>,
    @Schema(description = "Получено от покупателя (в тенге, опционально)", example = "2000.00")
    val taken: Double? = null,
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

/**
 * Запрос на создание чека возврата покупки.
 *
 * @property idempotencyKey Ключ идемпотентности.
 * @property items Список позиций.
 * @property markupPercent Наценка в процентах.
 * @property markupSum Наценка суммой.
 * @property discountPercent Скидка в процентах.
 * @property discountSum Скидка суммой.
 * @property change Сдача.
 * @property defaultVatGroup Группа НДС.
 * @property payments Способы оплаты.
 * @property taken Получено.
 * @property parentTicket Сведения о чеке-основании возврата.
 * @property customerBin БИН/ИИН покупателя.
 */
@Serializable
@Schema(description = "Запрос на создание чека возврата покупки. Сумма чека и суммы позиций вычисляются на сервере.")
data class ReceiptBuyReturnRequest(
    @Schema(description = "Ключ идемпотентности", example = "unique-key-123")
    @field:NotBlank
    val idempotencyKey: String,
    @Schema(description = "Позиции чека")
    @field:NotEmpty(message = "Список позиций не может быть пустым")
    val items: List<@Valid ReceiptItemDto>,
    @Schema(description = "Наценка на весь чек: процент (0–100). Взаимоисключающе с markupSum.", example = "0")
    @field:DecimalMin("0")
    @field:DecimalMax("100")
    val markupPercent: Double? = null,
    @Schema(description = "Наценка на весь чек: сумма в тенге. Взаимоисключающе с markupPercent.", example = "0")
    @field:DecimalMin("0")
    val markupSum: Double? = null,
    @Schema(description = "Скидка на весь чек: процент (0–100). Взаимоисключающе с discountSum.", example = "5")
    @field:DecimalMin("0")
    @field:DecimalMax("100")
    val discountPercent: Double? = null,
    @Schema(description = "Скидка на весь чек: сумма в тенге. Взаимоисключающе с discountPercent.", example = "50.00")
    @field:DecimalMin("0")
    val discountSum: Double? = null,
    @Schema(description = "Сдача (в тенге, опционально)", example = "499.25")
    val change: Double? = null,
    @Schema(
        description = "Группа НДС на весь чек. Если не указана — используется настройка ККМ. NO_VAT, VAT_0, VAT_5, VAT_10, VAT_16.",
        example = "NO_VAT"
    )
    val defaultVatGroup: String? = null,
    @Schema(description = "Способы оплаты. Допустимые типы: CASH, CARD, ELECTRONIC.")
    @field:NotEmpty(message = "Укажите хотя бы один способ оплаты")
    val payments: List<@Valid ReceiptPaymentDto>,
    @Schema(description = "Получено от покупателя (в тенге, опционально)", example = "2000.00")
    val taken: Double? = null,
    @Schema(description = "Информация об исходном чеке для возврата (parentTicket)", required = false)
    val parentTicket: ParentTicketDto? = null,
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

/**
 * Товарная позиция чека.
 *
 * @property barcode Штрихкод товара.
 * @property listExciseStamp Акцизные марки.
 * @property markupPercent Наценка в процентах.
 * @property markupSum Наценка суммой.
 * @property discountPercent Скидка в процентах.
 * @property discountSum Скидка суммой.
 * @property measureUnitCode Код единицы измерения.
 * @property name Наименование товара/услуги.
 * @property ntin НТИН.
 * @property price Цена.
 * @property quantity Количество.
 * @property vatGroup Группа НДС.
 * @property isStorno Признак сторно.
 */
@Serializable
@Schema(description = "Позиция чека. Сумма позиции вычисляется на сервере по цене, количеству, скидке и наценке.")
data class ReceiptItemDto(
    @Schema(description = "Штрихкод товара (опционально)", example = "4607011417556")
    val barcode: String? = null,
    @Schema(description = "Список акцизных марок (протокол ОФД list_excise_stamp)", example = "[\"12345678901234\"]")
    val listExciseStamp: List<String>? = null,
    @Schema(description = "Наценка на позицию: процент (0–100). Взаимоисключающе с markupSum.", example = "0")
    @field:DecimalMin("0", message = "Процент наценки не может быть отрицательным")
    @field:DecimalMax("100", message = "Процент наценки не может быть больше 100")
    val markupPercent: Double? = null,
    @Schema(description = "Наценка на позицию: сумма в тенге. Взаимоисключающе с markupPercent.", example = "0")
    @field:DecimalMin("0", message = "Сумма наценки не может быть отрицательной")
    val markupSum: Double? = null,
    @Schema(description = "Скидка на позицию: процент (0–100). Взаимоисключающе с discountSum.", example = "10")
    @field:DecimalMin("0", message = "Процент скидки не может быть отрицательным")
    @field:DecimalMax("100", message = "Процент скидки не может быть больше 100")
    val discountPercent: Double? = null,
    @Schema(description = "Скидка на позицию: сумма в тенге. Взаимоисключающе с discountPercent.", example = "30.10")
    @field:DecimalMin("0", message = "Сумма скидки не может быть отрицательной")
    val discountSum: Double? = null,
    @Schema(
        description = "Код единицы измерения (ОКЕИ). Только код (796, 116...). По умолчанию — штука (796). См. GET /units-of-measurement.",
        example = "796"
    )
    val measureUnitCode: String? = null,
    @Schema(
        description = "Наименование товара/услуги. Не допускаются обобщённые названия: «Товар», «Продукты», «Товар один» и т.п.",
        example = "Хлеб белый нарезной",
        minLength = 3,
        maxLength = 128
    )
    @field:NotBlank(message = "Наименование обязательно")
    @field:Size(min = 3, max = 128)
    @ItemNameValid
    val name: String,
    @Schema(description = "НТИН (протокол ОФД ntin)", example = "123456789012")
    val ntin: String? = null,
    @Schema(description = "Цена за единицу (в тенге)", example = "150.50")
    @field:NotNull
    @field:DecimalMin("0.01", message = "Цена должна быть положительной")
    val price: Double,
    @Schema(description = "Количество", example = "2")
    @field:NotNull
    @field:Positive(message = "Количество должно быть больше 0")
    @field:Max(999_999_999)
    val quantity: Long,
    @Schema(
        description = "Группа НДС для позиции. Допустимые значения: NO_VAT, VAT_0, VAT_5, VAT_10, VAT_16. Если не указана — используется defaultVatGroup кассы.",
        allowableValues = ["NO_VAT", "VAT_0", "VAT_5", "VAT_10", "VAT_16"],
        example = "VAT_16"
    )
    val vatGroup: String? = null,
    @Schema(description = "Признак сторно (аннулирования) этой товарной позиции", example = "false")
    val isStorno: Boolean? = false
) {
    init {
        require(discountPercent == null || discountSum == null) {
            "Укажите скидку на позицию либо в процентах (discountPercent), либо суммой (discountSum), но не оба значения"
        }
        require(markupPercent == null || markupSum == null) {
            "Укажите наценку на позицию либо в процентах (markupPercent), либо суммой (markupSum), но не оба значения"
        }
    }
}

/**
 * Способ оплаты чека.
 *
 * @property type Тип оплаты (CASH, CARD, ELECTRONIC).
 * @property sum Сумма оплаты в тенге.
 */
@Serializable
@Schema(description = "Способ оплаты чека")
data class ReceiptPaymentDto(
    @Schema(
        description = "Тип оплаты. Допустимые значения: CASH (наличные), CARD (карта), ELECTRONIC (электронные средства).",
        allowableValues = ["CASH", "CARD", "ELECTRONIC"],
        example = "CASH"
    )
    val type: String,
    @Schema(description = "Сумма оплаты (в тенге)", example = "500.00")
    @field:NotNull
    @field:DecimalMin("0", message = "Сумма оплаты не может быть отрицательной")
    val sum: Double
)

/**
 * Данные исходного чека (для возвратов).
 *
 * @property parentTicketNumber Номер исходного чека.
 * @property parentTicketDateTime Дата и время исходного чека (ISO-8601).
 * @property kgdKkmId Регистрационный номер ККМ (КГД).
 * @property parentTicketTotal Сумма исходного чека.
 * @property parentTicketIsOffline Офлайн-признак исходного чека.
 */
@Serializable
@Schema(description = "Информация об исходном чеке для возврата")
data class ParentTicketDto(
    @Schema(description = "Номер исходного чека", example = "123")
    val parentTicketNumber: Long,
    @Schema(
        description = "Дата и время исходного чека в формате ISO-8601 (UTC)",
        example = "2025-02-20T10:15:30Z"
    )
    val parentTicketDateTime: String,
    @Schema(
        description = "Регистрационный номер ККМ (КГД), на которой был пробит исходный чек",
        example = "123456789012"
    )
    val kgdKkmId: String,
    @Schema(description = "Сумма исходного чека (в тенге)", example = "1500.75")
    val parentTicketTotal: Double,
    @Schema(
        description = "Был ли исходный чек пробит в офлайн-режиме",
        example = "false"
    )
    val parentTicketIsOffline: Boolean
)
