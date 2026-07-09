package kz.mybrain.superkassa.core.domain.model.ofd

/**
 * Доменная модель номенклатурной позиции, полученной от ОФД.
 */
data class OfdNomenclatureItem(
    val id: Long,
    val barcode: String,
    val name: String,
    val nameKk: String?,
    val ntin: String?,
    val price: Double,
    val measureUnitCode: String?,
    val vatGroup: String?
)

/**
 * Доменный результат поиска номенклатуры в ОФД.
 */
data class OfdNomenclatureLookupResult(
    val found: Boolean,
    val item: OfdNomenclatureItem?,
    val resultCode: Int,
    val resultText: String
)
