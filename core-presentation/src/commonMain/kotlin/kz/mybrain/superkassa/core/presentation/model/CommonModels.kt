package kz.mybrain.superkassa.core.presentation.model

import kz.mybrain.superkassa.core.presentation.annotations.Schema
import kotlinx.serialization.Serializable
import kz.mybrain.superkassa.core.domain.model.common.UnitOfMeasurement
import kz.mybrain.superkassa.core.domain.model.common.VatGroup

/**
 * Пагинированный ответ.
 *
 * @param T Тип элементов списка.
 * @property items Список элементов текущей страницы.
 * @property total Общее количество элементов во всей выборке.
 * @property limit Лимит количества элементов на одной странице.
 * @property offset Смещение текущей страницы.
 * @property hasMore Признак наличия следующих страниц.
 */
@Serializable
data class PaginatedResponse<T>(
    val items: List<T>,
    val total: Int,
    val limit: Int,
    val offset: Int,
    val hasMore: Boolean
)

/**
 * Единый формат ответа с ошибкой от API.
 *
 * @property code Строковый код ошибки.
 * @property message Человекочитаемое сообщение об ошибке.
 * @property details Детали ошибки (например, стек-трейс или список невалидных полей).
 */
@Serializable
@Schema(description = "Ответ с ошибкой")
data class ApiErrorResponse(
    @Schema(description = "Код ошибки", example = "VALIDATION_ERROR") val code: String,
    @Schema(
        description = "Сообщение об ошибке",
        example = "Validation failed for argument [0]..."
    )
    val message: String,
    @Schema(
        description = "Детали ошибки (опционально)",
        example = "Field 'pin' must not be blank"
    )
    val details: String? = null
)

/**
 * Сгенерированный заводской номер ККМ.
 *
 * @property factoryNumber Заводской номер ККМ.
 * @property manufactureYear Год выпуска ККМ.
 */
@Serializable
@Schema(description = "Сгенерированный заводской номер и год выпуска ККМ")
data class FactoryNumberResponse(
    @Schema(description = "Заводской номер ККМ", example = "KZT2026000001")
    val factoryNumber: String,

    @Schema(description = "Год выпуска ККМ", example = "2026")
    val manufactureYear: Int
)

/**
 * Единица измерения (из справочника ОКЕИ).
 *
 * @property code Уникальный код единицы измерения.
 * @property nameShort Краткое наименование на русском языке.
 * @property nameFull Полное наименование на русском языке.
 */
@Serializable
data class UnitOfMeasurementResponse(
    val code: String,
    val nameShort: String,
    val nameFull: String
) {
    companion object {
        /**
         * Фабричный метод создания DTO из доменной модели.
         */
        fun from(uom: UnitOfMeasurement): UnitOfMeasurementResponse =
            UnitOfMeasurementResponse(
                code = uom.code,
                nameShort = uom.shortRus,
                nameFull = uom.nameRus
            )
    }
}

/**
 * Ставка НДС.
 *
 * @property code Код ставки НДС (например, VAT_16).
 * @property percent Процентная ставка.
 * @property description Описание ставки НДС.
 */
@Serializable
data class VatRateResponse(
    val code: String,
    val percent: Int,
    val description: String
) {
    companion object {
        /**
         * Фабричный метод создания DTO из доменной модели.
         */
        fun from(vatGroup: VatGroup): VatRateResponse =
            VatRateResponse(
                code = vatGroup.name,
                percent = vatGroup.percent,
                description = vatGroup.description
            )
    }
}
