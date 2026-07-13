package io.github.texport.superkassa.core.presentation.api.model.kkm

import io.github.texport.superkassa.core.presentation.api.annotations.Schema
import kotlinx.serialization.Serializable

@Serializable
@Schema(description = "Сервисная информация об организации и местоположении ККМ, полученная от ОФД")
data class OfdServiceInfoResponse(
    @Schema(description = "Название организации налогоплательщика", example = "ТОО Ромашка")
    val orgTitle: String,
    @Schema(description = "Юридический адрес на русском языке", example = "г. Алматы, ул. Абая, 10")
    val orgAddress: String,
    @Schema(description = "Юридический адрес на казахском языке", example = "Алматы қ., Абай к-сі, 10")
    val orgAddressKz: String,
    @Schema(description = "ИИН/БИН организации налогоплательщика", example = "123456789012")
    val orgInn: String,
    @Schema(description = "Код ОКЭД организации", example = "47111")
    val orgOkved: String,
    @Schema(description = "Географическая широта места установки ККМ (в миллионных долях градуса)", example = "43250000")
    val geoLatitude: Int,
    @Schema(description = "Географическая долгота места установки ККМ (в миллионных долях градуса)", example = "76900000")
    val geoLongitude: Int,
    @Schema(description = "Источник получения географических координат (например, GPS, GEOIP, MANUAL)", example = "GEOIP")
    val geoSource: String
)
