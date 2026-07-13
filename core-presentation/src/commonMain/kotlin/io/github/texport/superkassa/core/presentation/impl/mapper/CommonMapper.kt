package io.github.texport.superkassa.core.presentation.impl.mapper

import io.github.texport.superkassa.core.domain.api.model.common.VatGroup
import io.github.texport.superkassa.core.domain.api.model.common.UnitOfMeasurement
import io.github.texport.superkassa.core.presentation.api.model.common.VatRateResponse
import io.github.texport.superkassa.core.presentation.api.model.common.UnitOfMeasurementResponse

object CommonMapper {
    fun toResponse(vatGroup: VatGroup): VatRateResponse = VatRateResponse(
        code = vatGroup.name,
        percent = vatGroup.percent,
        description = vatGroup.description
    )

    fun toResponse(uom: UnitOfMeasurement): UnitOfMeasurementResponse = UnitOfMeasurementResponse(
        code = uom.code,
        nameShort = uom.shortRus,
        nameFull = uom.nameRus
    )
}
