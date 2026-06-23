package kz.mybrain.superkassa.core.application.model

import kz.mybrain.superkassa.core.domain.model.VatGroup
import kotlinx.serialization.Serializable

/**
 * DTO для API справочника ставок НДС.
 */
@Serializable
data class VatRateResponse(
    val code: String,
    val percent: Int,
    val description: String
) {
    companion object {
        fun from(vatGroup: VatGroup): VatRateResponse =
            VatRateResponse(
                code = vatGroup.name,
                percent = vatGroup.percent,
                description = vatGroup.description
            )
    }
}
