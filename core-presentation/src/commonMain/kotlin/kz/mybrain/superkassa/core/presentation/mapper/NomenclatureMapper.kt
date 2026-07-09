package kz.mybrain.superkassa.core.presentation.mapper

import kz.mybrain.superkassa.core.domain.model.ofd.OfdNomenclatureItem
import kz.mybrain.superkassa.core.domain.model.ofd.OfdNomenclatureLookupResult
import kz.mybrain.superkassa.core.presentation.model.NomenclatureItemDto
import kz.mybrain.superkassa.core.presentation.model.NomenclatureLookupResponse

/**
 * Маппер для преобразования доменных моделей номенклатуры в презентационные DTO.
 */
object NomenclatureMapper {
    /**
     * Преобразует доменный результат поиска номенклатуры в презентационный DTO.
     */
    fun toDto(result: OfdNomenclatureLookupResult): NomenclatureLookupResponse {
        return NomenclatureLookupResponse(
            found = result.found,
            item = result.item?.let { toDto(it) },
            resultCode = result.resultCode,
            resultText = result.resultText
        )
    }

    private fun toDto(item: OfdNomenclatureItem): NomenclatureItemDto {
        return NomenclatureItemDto(
            id = item.id,
            barcode = item.barcode,
            name = item.name,
            nameKk = item.nameKk,
            ntin = item.ntin,
            price = item.price,
            measureUnitCode = item.measureUnitCode,
            vatGroup = item.vatGroup
        )
    }
}
