package io.github.texport.superkassa.core.presentation.api.model

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable
import io.github.texport.superkassa.core.domain.model.kkm.KkmInfo
import io.github.texport.superkassa.core.presentation.api.annotations.NotBlank
import io.github.texport.superkassa.core.presentation.api.annotations.Min
import io.github.texport.superkassa.core.presentation.api.annotations.Max

@Serializable
enum class TaxRegimeDto {
    NO_VAT,
    VAT_PAYER,
    MIXED
}

@Serializable
enum class VatGroupDto {
    NO_VAT,
    VAT_0,
    VAT_5,
    VAT_10,
    VAT_16
}

@Serializable
enum class ReceiptLanguageDto {
    RU,
    KK,
    MIXED
}

@Serializable
data class OfdServiceInfoDto(
    val orgTitle: String,
    val orgAddress: String,
    val orgAddressKz: String,
    val orgInn: String,
    val orgOkved: String,
    val geoLatitude: Int,
    val geoLongitude: Int,
    val geoSource: String
)

@Serializable
data class ReceiptBrandingDto(
    val language: ReceiptLanguageDto = ReceiptLanguageDto.MIXED,
    val headerLogoUrl: String? = null,
    val paperWidthMm: Int = 80,
    val themeColor: String = "indigo",
    val beforeHeaderMsg: String? = null,
    val headerMsg: String? = null,
    val afterHeaderMsg: String? = null,
    val beforeItemsMsg: String? = null,
    val afterItemsMsg: String? = null,
    val beforeTotalsMsg: String? = null,
    val afterTotalsMsg: String? = null,
    val beforeQrMsg: String? = null,
    val footerMsg: String? = null,
    val useForceDarkTheme: Boolean = false,
    val customBackgroundColorHex: String? = null,
    val customCardTopBorderColorHex: String? = null,
    val ofdTicketAds: List<String> = emptyList(),
    val printOfdTicketAds: Boolean = true
)

// Mapping helpers
fun io.github.texport.superkassa.core.domain.model.ofd.OfdServiceInfo.toDto(): OfdServiceInfoDto = OfdServiceInfoDto(
    orgTitle = orgTitle,
    orgAddress = orgAddress,
    orgAddressKz = orgAddressKz,
    orgInn = orgInn,
    orgOkved = orgOkved,
    geoLatitude = geoLatitude,
    geoLongitude = geoLongitude,
    geoSource = geoSource
)

fun OfdServiceInfoDto.toDomain(): io.github.texport.superkassa.core.domain.model.ofd.OfdServiceInfo = io.github.texport.superkassa.core.domain.model.ofd.OfdServiceInfo(
    orgTitle = orgTitle,
    orgAddress = orgAddress,
    orgAddressKz = orgAddressKz,
    orgInn = orgInn,
    orgOkved = orgOkved,
    geoLatitude = geoLatitude,
    geoLongitude = geoLongitude,
    geoSource = geoSource
)

fun io.github.texport.superkassa.core.domain.model.receipt.ReceiptBranding.toDto(): ReceiptBrandingDto = ReceiptBrandingDto(
    language = ReceiptLanguageDto.valueOf(language.name),
    headerLogoUrl = headerLogoUrl,
    paperWidthMm = paperWidthMm,
    themeColor = themeColor,
    beforeHeaderMsg = beforeHeaderMsg,
    headerMsg = headerMsg,
    afterHeaderMsg = afterHeaderMsg,
    beforeItemsMsg = beforeItemsMsg,
    afterItemsMsg = afterItemsMsg,
    beforeTotalsMsg = beforeTotalsMsg,
    afterTotalsMsg = afterTotalsMsg,
    beforeQrMsg = beforeQrMsg,
    footerMsg = footerMsg,
    useForceDarkTheme = useForceDarkTheme,
    customBackgroundColorHex = customBackgroundColorHex,
    customCardTopBorderColorHex = customCardTopBorderColorHex,
    ofdTicketAds = ofdTicketAds,
    printOfdTicketAds = printOfdTicketAds
)

fun ReceiptBrandingDto.toDomain(): io.github.texport.superkassa.core.domain.model.receipt.ReceiptBranding = io.github.texport.superkassa.core.domain.model.receipt.ReceiptBranding(
    language = io.github.texport.superkassa.core.domain.model.receipt.ReceiptLanguage.valueOf(language.name),
    headerLogoUrl = headerLogoUrl,
    paperWidthMm = paperWidthMm,
    themeColor = themeColor,
    beforeHeaderMsg = beforeHeaderMsg,
    headerMsg = headerMsg,
    afterHeaderMsg = afterHeaderMsg,
    beforeItemsMsg = beforeItemsMsg,
    afterItemsMsg = afterItemsMsg,
    beforeTotalsMsg = beforeTotalsMsg,
    afterTotalsMsg = afterTotalsMsg,
    beforeQrMsg = beforeQrMsg,
    footerMsg = footerMsg,
    useForceDarkTheme = useForceDarkTheme,
    customBackgroundColorHex = customBackgroundColorHex,
    customCardTopBorderColorHex = customCardTopBorderColorHex,
    ofdTicketAds = ofdTicketAds,
    printOfdTicketAds = printOfdTicketAds
)

/**
 * Информация о ККМ, возвращаемая API.
 */
@Serializable
@Schema(description = "Информация о ККМ")
data class KkmResponse(
    @Schema(description = "ID ККМ", example = "kkm-123") val kkmId: String,
    @Schema(description = "Метка времени создания (epoch ms)", example = "1700000000000")
    val createdAt: Long,
    @Schema(
        description = "Метка времени последнего обновления (epoch ms)",
        example = "1700000000000"
    )
    val updatedAt: Long,
    @Schema(description = "Режим работы ККМ", example = "REGISTRATION") val mode: String,
    @Schema(description = "Состояние ККМ", example = "ACTIVE") val state: String,
    @Schema(description = "ID провайдера ОФД", example = "kazakhtelecom")
    val ofdId: String? = null,
    @Schema(description = "Среда ОФД", example = "test") val ofdEnvironment: String? = null,
    @Schema(description = "Регистрационный номер ККМ (КГД)", example = "123456789012")
    val kkmKgdId: String? = null,
    @Schema(description = "Заводской номер ККМ", example = "SWK-0001")
    val factoryNumber: String? = null,
    @Schema(description = "Год выпуска", example = "2024") val manufactureYear: Int? = null,
    @Schema(description = "Системный ID в ОФД", example = "sys-123")
    val ofdSystemId: String? = null,
    @Schema(description = "Сервисная информация ОФД")
    val ofdServiceInfo: OfdServiceInfoDto? = null,
    @Schema(
        description = "Зашифрованный токен ОФД (Base64)",
        hidden = true
    )
    val tokenEncryptedBase64: String? = null,
    @Schema(description = "Время обновления токена", example = "1700000000000")
    val tokenUpdatedAt: Long? = null,
    @Schema(description = "Номер последней смены", example = "10") val lastShiftNo: Int? = null,
    @Schema(description = "Номер последнего чека", example = "150")
    val lastReceiptNo: Int? = null,
    @Schema(description = "Номер последнего Z-отчета", example = "10")
    val lastZReportNo: Int? = null,
    @Schema(
        description = "Время начала автономного режима (если активен)",
        example = "1700000000000"
    )
    val autonomousSince: Long? = null,
    @Schema(description = "Автоматическое закрытие смены", example = "false")
    val autoCloseShift: Boolean = false,
    @Schema(description = "Хэш последней фискальной операции", example = "base64hash==")
    val lastFiscalHashBase64: String? = null,
    @Schema(
        description = "Налоговый режим ККМ (NO_VAT, VAT_PAYER, MIXED)",
        example = "NO_VAT"
    )
    val taxRegime: String? = null,
    @Schema(
        description = "Базовая группа НДС по умолчанию (NO_VAT, VAT_0, VAT_16)",
        example = "NO_VAT"
    )
    val defaultVatGroup: String? = null,
    @Schema(description = "Настройки брендирования чеков")
    val branding: ReceiptBrandingDto? = null
)

/**
 * Результат листинга ККМ.
 */
data class KkmListResult(
    val items: List<KkmInfo>,
    val total: Int
)

/**
 * Параметры листинга ККМ.
 */
@Serializable
data class KkmListParams(
    val limit: Int = 50,
    val offset: Int = 0,
    val state: String? = null,
    val search: String? = null,
    val sortBy: String = "createdAt",
    val sortOrder: String = "DESC"
) {
    init {
        require(limit in 1..1000) { "limit должен быть от 1 до 1000" }
        require(offset >= 0) { "offset должен быть >= 0" }
        require(sortOrder in listOf("ASC", "DESC")) { "sortOrder должен быть ASC или DESC" }
        require(sortBy in listOf("createdAt", "updatedAt", "state", "registrationNumber")) {
            "sortBy должен быть одним из: createdAt, updatedAt, state, registrationNumber"
        }
    }
}

/**
 * Запрос на обновление общих настроек ККМ.
 */
@Serializable
data class KkmSettingsUpdateRequest(
    val autoCloseShift: Boolean? = null
)

/**
 * Запрос на обновление параметров черновика ККМ.
 */
@Serializable
@Schema(description = "Запрос на обновление параметров черновика ККМ")
data class KkmDraftUpdateRequest(
    @Schema(description = "ID ОФД провайдера", example = "kazakhtelecom")
    val ofdId: String? = null,
    @Schema(description = "Среда ОФД", example = "test") val ofdEnvironment: String? = null,
    @Schema(description = "Системный ID в ОФД", example = "system-id-123")
    val ofdSystemId: String? = null,
    @Schema(description = "Заводской номер ККМ", example = "ZAVOD-001")
    val factoryNumber: String? = null,
    @Schema(description = "Год выпуска", example = "2024") val manufactureYear: Int? = null
)

/**
 * Запрос на обновление налоговых настроек ККМ.
 */
@Serializable
@Schema(description = "Обновление налогового режима и базовой группы НДС ККМ")
data class KkmTaxSettingsUpdateRequest(
    @Schema(
        description = "Налоговый режим ККМ",
        example = "NO_VAT",
        allowableValues = ["NO_VAT", "VAT_PAYER", "MIXED"]
    )
    val taxRegime: TaxRegimeDto,
    @Schema(
        description = "Базовая группа НДС по умолчанию",
        example = "NO_VAT",
        allowableValues = ["NO_VAT", "VAT_0", "VAT_16"]
    )
    val defaultVatGroup: VatGroupDto
)

/**
 * Упрощенный запрос на инициализацию ККМ без черновика.
 */
@Serializable
@Schema(description = "Упрощенный запрос на инициализацию ККМ (данные получаются из ОФД)")
data class KkmInitSimpleRequest(
    @Schema(description = "ID провайдера ОФД", example = "kazakhtelecom")
    @field:NotBlank(message = "OFD provider ID is required")
    val ofdId: String,

    @Schema(description = "Среда ОФД (test/prod)", example = "test")
    @field:NotBlank(message = "OFD environment is required")
    val ofdEnvironment: String,

    @Schema(description = "Системный ID ККМ в ОФД", example = "200367")
    @field:NotBlank(message = "OFD System ID is required")
    val ofdSystemId: String,

    @Schema(description = "Токен доступа ОФД", example = "32876190")
    @field:NotBlank(message = "OFD Token is required")
    val ofdToken: String,

    @Schema(
        description = "Базовая группа НДС по умолчанию: NO_VAT, VAT_0, VAT_5, VAT_10, VAT_16. " +
            "Если не указана — считается NO_VAT (касса не плательщик НДС).",
        example = "NO_VAT"
    )
    val defaultVatGroup: VatGroupDto = VatGroupDto.NO_VAT,
    @Schema(description = "Ручной ввод ОКЭД при отсутствии данных от ОФД")
    val okved: String? = null
)

/**
 * Запрос на прямую инициализацию ККМ (без черновика).
 */
@Serializable
@Schema(description = "Запрос на прямую инициализацию ККМ (без черновика)")
data class KkmInitDirectRequest(
    @Schema(description = "ID провайдера ОФД", example = "kazakhtelecom")
    @field:NotBlank(message = "OFD provider ID is required")
    val ofdId: String,
    @Schema(description = "Среда ОФД (test/prod)", example = "test")
    @field:NotBlank(message = "OFD environment is required")
    val ofdEnvironment: String,
    @Schema(description = "Системный ID ККМ в ОФД", example = "system-id-12345")
    @field:NotBlank(message = "OFD System ID is required")
    val ofdSystemId: String,
    @Schema(description = "Токен доступа ОФД", example = "token-abc-123")
    @field:NotBlank(message = "OFD Token is required")
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
    @Schema(description = "Сервисная информация ОФД") val serviceInfo: OfdServiceInfoDto? = null,
    @Schema(description = "Ручной ввод ОКЭД при отсутствии данных от ОФД") val okved: String? = null,
    @Schema(
        description = "Не используется. ПИН-код администратора передаётся только в заголовке Authorization",
        example = "deprecated"
    )
    val _unused: String? = null
)

/**
 * Запрос на фискализацию ранее созданного черновика ККМ.
 */
@Serializable
@Schema(description = "Запрос на фискализацию ранее созданного черновика ККМ")
data class KkmInitDraftRequest(
    @Schema(description = "ID черновика ККМ", example = "kkm-draft-1")
    @field:NotBlank(message = "KKM ID is required")
    val kkmId: String,
    @Schema(description = "Системный ID ККМ в ОФД", example = "system-id-12345")
    @field:NotBlank(message = "OFD System ID is required")
    val ofdSystemId: String,
    @Schema(description = "Токен доступа ОФД", example = "token-abc-123")
    @field:NotBlank(message = "OFD Token is required")
    val ofdToken: String,
    @Schema(description = "Регистрационный номер ККМ (КГД)", example = "123456789012")
    @field:NotBlank(message = "Registration number is required")
    val kkmKgdId: String,
    @Schema(description = "Сервисная информация ОФД") val serviceInfo: OfdServiceInfoDto? = null,
    @Schema(
        description = "Не используется. ПИН-код администратора передаётся только в заголовке Authorization",
        example = "deprecated"
    )
    val _unused: String? = null
)
