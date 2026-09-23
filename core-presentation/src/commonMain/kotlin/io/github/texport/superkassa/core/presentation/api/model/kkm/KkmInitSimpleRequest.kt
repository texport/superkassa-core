package io.github.texport.superkassa.core.presentation.api.model.kkm

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import io.github.texport.superkassa.core.presentation.api.annotations.NotBlank
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * Упрощенный запрос на инициализацию ККМ без черновика.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Schema(description = "Упрощенный запрос на инициализацию ККМ (данные получаются из ОФД)")
data class KkmInitSimpleRequest(
    @Schema(description = "ID провайдера ОФД", example = "kazakhtelecom")
    @field:NotBlank(message = "BFD provider ID is required")
    val ofdId: String,

    @Schema(description = "Среда ОФД (test/prod)", example = "test")
    @field:NotBlank(message = "BFD environment is required")
    val ofdEnvironment: String,

    @Schema(description = "Системный ID ККМ в ОФД", example = "200367")
    @field:NotBlank(message = "BFD System ID is required")
    val ofdSystemId: String,

    @Schema(description = "Токен доступа ОФД", example = "32876190")
    @field:NotBlank(message = "BFD Token is required")
    val ofdToken: String,

    @Schema(
        description = "Базовая группа НДС по умолчанию: NO_VAT, VAT_0, VAT_5, VAT_10, VAT_16. " +
            "Если не указана — считается NO_VAT (касса не плательщик НДС).",
        example = "NO_VAT"
    )
    val defaultVatGroup: VatGroup = VatGroup.NO_VAT,
    @Schema(description = "Ручной ввод ОКЭД при отсутствии данных от ОФД. Прежнее имя поля okved принимается.", example = "47111")
    @JsonNames("okved")
    val oked: String? = null,
    @Schema(
        description = "Пин администратора новой кассы. Без него касса заводится со стандартным " +
            "пином, а войти с ним узел не даёт — открыть такую кассу будет нельзя.",
        example = "4821"
    )
    val adminPin: String? = null
)
