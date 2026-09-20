package io.github.texport.superkassa.core.presentation.api.model.receipt

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

/**
 * Отраслевые реквизиты чека.
 *
 * Вид отрасли задаёт, какой подблок обязателен: услуги и гостиницы — services,
 * нефтепродукты — gasOil, такси — taxi, стоянки — parking. Торговля не требует
 * ни одного, и подблок должен быть ровно один.
 */
@Serializable
@Schema(description = "Отраслевые реквизиты чека")
data class ReceiptDomainRequest(
    @Schema(description = "Вид отрасли", example = "DOMAIN_TAXI")
    val type: String,
    val services: ServicesRequest? = null,
    val gasOil: GasOilRequest? = null,
    val taxi: TaxiRequest? = null,
    val parking: ParkingRequest? = null
) {
    @Serializable
    data class ServicesRequest(val accountNumber: String)

    @Serializable
    data class GasOilRequest(
        val correctionNumber: String? = null,
        val correctionSum: Decimal? = null,
        val cardNumber: String? = null
    )

    @Serializable
    data class TaxiRequest(val carNumber: String, val isOrder: Boolean, val currentFee: Decimal)

    @Serializable
    data class ParkingRequest(val beginTimeMillis: Long, val endTimeMillis: Long)
}
