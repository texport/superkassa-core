package kz.mybrain.superkassa.core.domain.model.ofd

import kotlinx.serialization.Serializable

/**
 * Служебная информация о налогоплательщике/организации в ОФД.
 *
 * @property orgTitle Официальное наименование организации (налогоплательщика).
 * @property orgAddress Адрес использования ККМ на русском языке.
 * @property orgAddressKz Адрес использования ККМ на казахском языке.
 * @property orgInn БИН/ИИН организации (налогоплательщика).
 * @property orgOkved Код ОКЭД (Общий классификатор видов экономической деятельности).
 * @property geoLatitude Географическая широта места установки ККМ (умноженная на 1 000 000 для целочисленного формата).
 * @property geoLongitude Географическая долгота места установки ККМ (умноженная на 1 000 000 для целочисленного формата).
 * @property geoSource Источник получения координат (например, GPS, MANUAL).
 */
@Serializable
data class OfdServiceInfo(
    val orgTitle: String,
    val orgAddress: String,
    val orgAddressKz: String,
    val orgInn: String,
    val orgOkved: String,
    val geoLatitude: Int,
    val geoLongitude: Int,
    val geoSource: String
)
