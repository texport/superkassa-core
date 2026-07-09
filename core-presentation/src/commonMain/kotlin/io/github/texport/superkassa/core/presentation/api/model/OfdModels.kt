package io.github.texport.superkassa.core.presentation.api.model

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import io.github.texport.superkassa.core.presentation.api.annotations.NotBlank
import kotlinx.serialization.Serializable

/**
 * Запрос на обновление токена авторизации в ОФД.
 *
 * @property token Новый токен ОФД.
 */
@Serializable
@Schema(description = "Запрос на обновление токена ОФД")
data class OfdTokenUpdateRequest(
    @Schema(description = "Новый токен ОФД", example = "new-token-123")
    @field:NotBlank
    val token: String
)

/**
 * Запрос для получения параметров авторизации ОФД.
 *
 * @property kkmId Уникальный идентификатор ККМ.
 */
@Serializable
@Schema(description = "Запрос информации об авторизации в ОФД")
data class OfdAuthInfoRequest(
    @Schema(description = "Идентификатор ККМ", example = "kkm-uuid")
    @field:NotBlank
    val kkmId: String
)

/**
 * Информация об авторизации в ОФД, возвращаемая API.
 *
 * @property token Текущий активный токен ОФД.
 * @property nextReqNum Номер следующего запроса в ОФД.
 */
@Serializable
@Schema(description = "Информация об авторизации в ОФД")
data class OfdAuthInfoResponse(
    @Schema(description = "Текущий токен ОФД", example = "token-xyz") val token: String?,
    @Schema(description = "Следующий номер запроса (reqNum)", example = "105")
    val nextReqNum: Int
)

/**
 * Сведения о единичной номенклатурной позиции, полученной из ОФД.
 */
@Serializable
@Schema(description = "Сведения о номенклатурной позиции")
data class NomenclatureItemDto(
    @Schema(description = "Внутренний идентификатор позиции в каталоге НКТ", example = "639308")
    val id: Long,
    @Schema(description = "Штрихкод товара", example = "5449000176431")
    val barcode: String,
    @Schema(description = "Наименование товара на русском языке", example = "Напиток Piko Pulpy апельсин 0,5л")
    val name: String,
    @Schema(description = "Наименование товара на казахском языке", example = "Напиток Piko Pulpy апельсин 0,5л")
    val nameKk: String?,
    @Schema(description = "Глобальный номер товарной позиции NTIN", example = "0200091550792")
    val ntin: String?,
    @Schema(description = "Рекомендованная цена продажи (в тенге)", example = "0.0")
    val price: Double,
    @Schema(description = "Код единицы измерения (ОКЕИ)", example = "166")
    val measureUnitCode: String?,
    @Schema(description = "Группа НДС товара (если определена)", example = "NO_VAT")
    val vatGroup: String?
)

/**
 * Запрос для поиска номенклатурной позиции в каталоге ОФД.
 *
 * @property kkmId Уникальный идентификатор ККМ.
 * @property barcode Штрихкод искомого товара.
 */
@Serializable
@Schema(description = "Запрос для поиска номенклатурной позиции")
data class NomenclatureLookupRequest(
    @Schema(description = "Идентификатор ККМ", example = "kkm-uuid")
    @field:NotBlank
    val kkmId: String,
    @Schema(description = "Штрихкод товара", example = "5449000176431")
    @field:NotBlank
    val barcode: String
)

/**
 * Результат поиска номенклатурной позиции в каталоге ОФД.
 */
@Serializable
@Schema(description = "Результат поиска номенклатурной позиции")
data class NomenclatureLookupResponse(
    @Schema(description = "Флаг успешности поиска", example = "true")
    val found: Boolean,
    @Schema(description = "Детали найденной позиции (если найдена)")
    val item: NomenclatureItemDto?,
    @Schema(description = "Код результата обработки каталога ОФД", example = "0")
    val resultCode: Int,
    @Schema(description = "Текстовое описание результата", example = "OK")
    val resultText: String?
)
