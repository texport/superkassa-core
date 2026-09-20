package io.github.texport.superkassa.core.domain.api.model.ofd

import io.github.texport.superkassa.core.domain.api.model.common.Decimal

/**
 * Доменная модель номенклатурной позиции, полученной от ОФД.
 */
data class OfdNomenclatureItem(
    val id: Long,
    val barcode: String,
    val name: String,
    val nameKk: String?,
    val ntin: String?,
    val price: Decimal,
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
