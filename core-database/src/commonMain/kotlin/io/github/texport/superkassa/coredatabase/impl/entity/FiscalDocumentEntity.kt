package io.github.texport.superkassa.coredatabase.impl.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot

/**
 * Сущность Room для хранения фискальных документов ККМ (чеков, отчетов, кассовых операций).
 */
@Entity(tableName = "fiscal_documents")
data class FiscalDocumentEntity(
    @PrimaryKey val id: String,
    val cashboxId: String,
    val shiftId: String,
    val docType: String,
    val docNo: Long?,
    val printedDocumentNumber: Long? = null,
    val shiftNo: Long?,
    val createdAt: Long,
    val totalAmount: Long?,
    val currency: String?,
    val fiscalSign: String?,
    val autonomousSign: String?,
    val isAutonomous: Boolean,
    val ofdStatus: String?,
    val ofdErrorCode: Int?,
    val ofdErrorText: String? = null,
    val deliveredAt: Long?,
    val registrationNumber: String?,
    val taxpayerName: String?,
    val taxpayerBin: String?,
    val taxpayerAddress: String?,
    val factoryNumber: String?,
    val ofdProvider: String?,
    val receiptPayloadJson: String? = null,
    val receiptUrl: String? = null
) {
    fun toDomain(): FiscalDocumentSnapshot {
        return FiscalDocumentSnapshot(
            id = id,
            cashboxId = cashboxId,
            shiftId = shiftId,
            docType = docType,
            docNo = docNo,
            printedDocumentNumber = printedDocumentNumber,
            shiftNo = shiftNo,
            createdAt = createdAt,
            totalAmount = totalAmount,
            currency = currency,
            fiscalSign = fiscalSign,
            autonomousSign = autonomousSign,
            isAutonomous = isAutonomous,
            ofdStatus = ofdStatus,
            ofdErrorCode = ofdErrorCode,
            ofdErrorText = ofdErrorText,
            receiptUrl = receiptUrl,
            deliveredAt = deliveredAt,
            registrationNumber = registrationNumber,
            taxpayerName = taxpayerName,
            taxpayerBin = taxpayerBin,
            taxpayerAddress = taxpayerAddress,
            factoryNumber = factoryNumber,
            ofdProvider = ofdProvider
        )
    }

    companion object {
        fun fromDomain(domain: FiscalDocumentSnapshot, receiptPayloadJson: String? = null): FiscalDocumentEntity {
            return FiscalDocumentEntity(
                id = domain.id,
                cashboxId = domain.cashboxId,
                shiftId = domain.shiftId,
                docType = domain.docType,
                docNo = domain.docNo,
                printedDocumentNumber = domain.printedDocumentNumber,
                shiftNo = domain.shiftNo,
                createdAt = domain.createdAt,
                totalAmount = domain.totalAmount,
                currency = domain.currency,
                fiscalSign = domain.fiscalSign,
                autonomousSign = domain.autonomousSign,
                isAutonomous = domain.isAutonomous,
                ofdStatus = domain.ofdStatus,
                ofdErrorCode = domain.ofdErrorCode,
                ofdErrorText = domain.ofdErrorText,
                receiptUrl = domain.receiptUrl,
                deliveredAt = domain.deliveredAt,
                registrationNumber = domain.registrationNumber,
                taxpayerName = domain.taxpayerName,
                taxpayerBin = domain.taxpayerBin,
                taxpayerAddress = domain.taxpayerAddress,
                factoryNumber = domain.factoryNumber,
                ofdProvider = domain.ofdProvider,
                receiptPayloadJson = receiptPayloadJson
            )
        }
    }
}
