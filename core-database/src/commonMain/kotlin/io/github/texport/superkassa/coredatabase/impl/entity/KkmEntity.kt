package io.github.texport.superkassa.coredatabase.impl.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.texport.superkassa.core.domain.api.model.common.TaxRegime
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdServiceInfo

/**
 * Сущность Room для хранения настроек и состояния ККМ.
 */
@Entity(tableName = "kkms")
data class KkmEntity(
    @PrimaryKey val id: String,
    val registrationNumber: String?,
    val factoryNumber: String?,
    val state: String,
    val mode: String,
    val autoCloseShift: Boolean,
    val autoCashout: Boolean,
    val tokenEncryptedBase64: String? = null,
    val tokenUpdatedAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val ofdProvider: String? = null,
    val systemId: String? = null,
    val manufactureYear: Int? = null,
    val blockReasonCode: Int? = null,
    val lastShiftNo: Int? = null,
    val lastReceiptNo: Int? = null,
    val lastZReportNo: Int? = null,
    val autonomousSince: Long? = null,
    val lastFiscalHashBase64: String? = null,
    val taxRegime: String? = null,
    val defaultVatGroup: String? = null,
    val orgTitle: String? = null,
    val orgAddress: String? = null,
    val orgAddressKz: String? = null,
    val orgInn: String? = null,
    val orgOkved: String? = null,
    val geoLatitude: Int? = null,
    val geoLongitude: Int? = null,
    val geoSource: String? = null,
    /** Название кассы, данное владельцем; у заведённых раньше его нет. */
    val name: String? = null
) {
    fun toDomain(): KkmInfo {
        val ofdInfo = if (orgTitle != null) {
            OfdServiceInfo(
                orgTitle = orgTitle,
                orgAddress = orgAddress ?: "",
                orgAddressKz = orgAddressKz ?: "",
                orgInn = orgInn ?: "",
                orgOkved = orgOkved ?: "",
                geoLatitude = geoLatitude ?: 0,
                geoLongitude = geoLongitude ?: 0,
                geoSource = geoSource ?: "UNKNOWN"
            )
        } else {
            null
        }

        return KkmInfo(
            id = id,
            createdAt = createdAt,
            updatedAt = updatedAt,
            mode = mode,
            state = state,
            ofdProvider = ofdProvider,
            registrationNumber = registrationNumber,
            factoryNumber = factoryNumber,
            manufactureYear = manufactureYear,
            systemId = systemId,
            ofdServiceInfo = ofdInfo,
            tokenEncryptedBase64 = tokenEncryptedBase64,
            tokenUpdatedAt = tokenUpdatedAt,
            lastShiftNo = lastShiftNo,
            lastReceiptNo = lastReceiptNo,
            lastZReportNo = lastZReportNo,
            autonomousSince = autonomousSince,
            autoCloseShift = autoCloseShift,
            autoCashout = autoCashout,
            lastFiscalHashBase64 = lastFiscalHashBase64,
            taxRegime = taxRegime?.let {
                try { TaxRegime.valueOf(it) } catch (e: Exception) { TaxRegime.NO_VAT }
            } ?: TaxRegime.NO_VAT,
            defaultVatGroup = defaultVatGroup?.let {
                try { VatGroup.valueOf(it) } catch (e: Exception) { VatGroup.NO_VAT }
            } ?: VatGroup.NO_VAT,
            blockReasonCode = blockReasonCode,
            name = name
        )
    }

    companion object {
        fun fromDomain(domain: KkmInfo): KkmEntity {
            val s = domain.ofdServiceInfo
            return KkmEntity(
                id = domain.id,
                registrationNumber = domain.registrationNumber,
                factoryNumber = domain.factoryNumber,
                state = domain.state,
                mode = domain.mode,
                autoCloseShift = domain.autoCloseShift,
                autoCashout = domain.autoCashout,
                tokenEncryptedBase64 = domain.tokenEncryptedBase64,
                tokenUpdatedAt = domain.tokenUpdatedAt,
                createdAt = domain.createdAt,
                updatedAt = domain.updatedAt,
                ofdProvider = domain.ofdProvider,
                systemId = domain.systemId,
                manufactureYear = domain.manufactureYear,
                blockReasonCode = domain.blockReasonCode,
                lastShiftNo = domain.lastShiftNo,
                lastReceiptNo = domain.lastReceiptNo,
                lastZReportNo = domain.lastZReportNo,
                autonomousSince = domain.autonomousSince,
                lastFiscalHashBase64 = domain.lastFiscalHashBase64,
                taxRegime = domain.taxRegime.name,
                defaultVatGroup = domain.defaultVatGroup.name,
                orgTitle = s?.orgTitle,
                orgAddress = s?.orgAddress,
                orgAddressKz = s?.orgAddressKz,
                orgInn = s?.orgInn,
                orgOkved = s?.orgOkved,
                geoLatitude = s?.geoLatitude,
                geoLongitude = s?.geoLongitude,
                geoSource = s?.geoSource,
                name = domain.name
            )
        }
    }
}
