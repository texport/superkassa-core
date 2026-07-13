package io.github.texport.superkassa.core.presentation.impl.mapper

import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdNomenclatureItem
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdNomenclatureLookupResult
import io.github.texport.superkassa.core.presentation.api.model.ofd.*

object OfdMapper {

    fun toResponse(res: OfdCommandResult): OfdCommandResponse = OfdCommandResponse(
        status = OfdCommandStatus.valueOf(res.status.name),
        responseBin = res.responseBin,
        responseJson = res.responseJson,
        responseToken = res.responseToken,
        responseReqNum = res.responseReqNum,
        resultCode = res.resultCode,
        resultText = res.resultText,
        fiscalSign = res.fiscalSign,
        autonomousSign = res.autonomousSign,
        errorMessage = res.errorMessage,
        receiptUrl = res.receiptUrl
    )

    fun toResponse(result: OfdNomenclatureLookupResult): NomenclatureLookupResponse = NomenclatureLookupResponse(
        found = result.found,
        item = result.item?.let { toResponse(it) },
        resultCode = result.resultCode,
        resultText = result.resultText
    )

    fun toResponse(item: OfdNomenclatureItem): NomenclatureItemResponse = NomenclatureItemResponse(
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
