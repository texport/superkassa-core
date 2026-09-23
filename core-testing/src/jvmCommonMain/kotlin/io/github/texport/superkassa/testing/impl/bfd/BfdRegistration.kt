package io.github.texport.superkassa.testing.impl.bfd

import io.github.texport.superkassa.testing.api.bfd.BfdOrganization
import kz.kazakhtelecom.proto.v203.KkmRegInfo
import kz.kazakhtelecom.proto.v203.OrgRegInfo
import kz.kazakhtelecom.proto.v203.PosRegInfo
import kz.kazakhtelecom.proto.v203.ServiceResponse

/**
 * Регистрационные сведения кассы, как их отдаёт БФД на запрос сведений.
 *
 * Регистрационный номер КГД и заводской номер выводятся из номера кассы
 * в БФД: у каждой кассы свои, и совпасть у двух касс они не могут.
 */
internal object BfdRegistration {
    private const val KGD_PREFIX = "0101"
    private const val FACTORY_PREFIX = "SK"
    private const val SERIAL_DIGITS = 8
    private const val LATITUDE = 43_238_949
    private const val LONGITUDE = 76_889_709

    /** Регистрационный номер КГД кассы [kassa]: 12 цифр. */
    fun kgdNumber(kassa: Long): String = KGD_PREFIX + serial(kassa)

    fun of(kassa: Long, organization: BfdOrganization) = ServiceResponse(
        reg_info = ServiceResponse.RegInfo(
            kkm = KkmRegInfo(
                fns_kkm_id = kgdNumber(kassa),
                serial_number = FACTORY_PREFIX + serial(kassa),
                kkm_id = kassa.toString()
            ),
            pos = PosRegInfo(
                title = organization.title,
                address = organization.address,
                address_kz = organization.addressKz,
                latitude = LATITUDE,
                longitude = LONGITUDE
            ),
            org = OrgRegInfo(
                title = organization.title,
                address = organization.address,
                address_kz = organization.addressKz,
                inn = organization.bin,
                okved = organization.oked
            )
        )
    )

    private fun serial(kassa: Long): String = kassa.toString().padStart(SERIAL_DIGITS, '0')
}
