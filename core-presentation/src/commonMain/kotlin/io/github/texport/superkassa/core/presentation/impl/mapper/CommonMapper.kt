package io.github.texport.superkassa.core.presentation.impl.mapper

import io.github.texport.superkassa.core.domain.api.model.common.VatGroup
import io.github.texport.superkassa.core.domain.api.model.common.UnitOfMeasurement
import io.github.texport.superkassa.core.presentation.api.model.common.VatRateResponse
import io.github.texport.superkassa.core.presentation.api.model.common.UnitOfMeasurementResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import io.github.texport.superkassa.core.string.api.CoreStrings

object CommonMapper {
    fun toResponse(vatGroup: VatGroup): VatRateResponse = VatRateResponse(
        code = vatGroup.name,
        percent = vatGroup.percent,
        description = vatGroup.description,
        name = TrilingualMessageResponse.from(CoreStrings.vatGroup(vatGroup.name))
    )

    fun toResponse(uom: UnitOfMeasurement): UnitOfMeasurementResponse = UnitOfMeasurementResponse(
        code = uom.code,
        nameShort = uom.shortRus,
        nameFull = uom.nameRus
    )
}
