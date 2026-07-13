package io.github.texport.superkassa.core.presentation.api.model.kkm

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

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
    val taxRegime: TaxRegime,
    @Schema(
        description = "Базовая группа НДС по умолчанию",
        example = "NO_VAT",
        allowableValues = ["NO_VAT", "VAT_0", "VAT_16"]
    )
    val defaultVatGroup: VatGroup
)
