package io.github.texport.superkassa.core.presentation.api.model.kkm

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import io.github.texport.superkassa.core.presentation.api.annotations.NotBlank
import io.github.texport.superkassa.core.presentation.api.annotations.Min
import io.github.texport.superkassa.core.presentation.api.annotations.Max
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * Запрос на прямую инициализацию ККМ.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@Schema(description = "Запрос на прямую инициализацию ККМ")
data class KkmInitDirectRequest(
    @Schema(description = "ID провайдера ОФД", example = "kazakhtelecom")
    @field:NotBlank(message = "BFD provider ID is required")
    val ofdId: String,
    @Schema(description = "Среда ОФД (test/prod)", example = "test")
    @field:NotBlank(message = "BFD environment is required")
    val ofdEnvironment: String,
    @Schema(description = "Системный ID ККМ в ОФД", example = "system-id-12345")
    @field:NotBlank(message = "BFD System ID is required")
    val ofdSystemId: String,
    @Schema(description = "Токен доступа ОФД", example = "token-abc-123")
    @field:NotBlank(message = "BFD Token is required")
    val ofdToken: String,
    @Schema(description = "Регистрационный номер ККМ (КГД)", example = "123456789012")
    @field:NotBlank(message = "Registration number is required")
    val kkmKgdId: String,
    @Schema(description = "Заводской номер ККМ", example = "SWK-0001")
    @field:NotBlank(message = "Factory number is required")
    val factoryNumber: String,
    @Schema(description = "Год выпуска", example = "2024")
    @field:Min(2000)
    @field:Max(2100)
    val manufactureYear: Int,
    @Schema(description = "Сервисная информация ОФД") val serviceInfo: OfdServiceInfoResponse? = null,
    @Schema(description = "Ручной ввод ОКЭД при отсутствии данных от ОФД. Прежнее имя поля okved принимается.", example = "47111")
    @JsonNames("okved")
    val oked: String? = null,
    @Schema(
        description = "Пин администратора новой кассы, от 4 до 10 символов. Пина по умолчанию нет: " +
            "без этого поля касса не заводится (KKM_ADMIN_PIN_REQUIRED).",
        example = "7391",
        required = true,
        minLength = 4,
        maxLength = 10
    )
    val adminPin: String? = null,
    @Schema(
        description = "Не используется",
        example = "deprecated"
    )
    val _unused: String? = null
)
