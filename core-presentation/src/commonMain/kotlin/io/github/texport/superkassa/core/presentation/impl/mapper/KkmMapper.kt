package io.github.texport.superkassa.core.presentation.impl.mapper

import io.github.texport.superkassa.core.domain.api.model.receipt.TicketAd as DomainTicketAd
import io.github.texport.superkassa.core.domain.api.model.common.CounterSnapshot
import io.github.texport.superkassa.core.domain.api.model.kkm.CashOperationRequest as DomainCashOperationRequest
import io.github.texport.superkassa.core.domain.api.model.kkm.CashOperationResult
import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdServiceInfo
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptBranding
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptDocumentTypes
import io.github.texport.superkassa.core.presentation.api.model.kkm.*
import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus

object KkmMapper {

    fun toResponse(ofd: OfdServiceInfo): OfdServiceInfoResponse = OfdServiceInfoResponse(
        orgTitle = ofd.orgTitle,
        orgAddress = ofd.orgAddress,
        orgAddressKz = ofd.orgAddressKz,
        orgInn = ofd.orgInn,
        orgOkved = ofd.orgOkved,
        geoLatitude = ofd.geoLatitude,
        geoLongitude = ofd.geoLongitude,
        geoSource = ofd.geoSource
    )

    fun toDomain(dto: OfdServiceInfoResponse): OfdServiceInfo = OfdServiceInfo(
        orgTitle = dto.orgTitle,
        orgAddress = dto.orgAddress,
        orgAddressKz = dto.orgAddressKz,
        orgInn = dto.orgInn,
        orgOkved = dto.orgOkved,
        geoLatitude = dto.geoLatitude,
        geoLongitude = dto.geoLongitude,
        geoSource = dto.geoSource
    )

    fun toResponse(branding: ReceiptBranding): ReceiptBrandingResponse = ReceiptBrandingResponse(
        language = ReceiptLanguage.valueOf(branding.language.name),
        headerLogoUrl = branding.headerLogoUrl,
        paperWidthMm = branding.paperWidthMm,
        themeColor = branding.themeColor,
        beforeHeaderMsg = branding.beforeHeaderMsg,
        headerMsg = branding.headerMsg,
        afterHeaderMsg = branding.afterHeaderMsg,
        beforeItemsMsg = branding.beforeItemsMsg,
        afterItemsMsg = branding.afterItemsMsg,
        beforeTotalsMsg = branding.beforeTotalsMsg,
        afterTotalsMsg = branding.afterTotalsMsg,
        beforeQrMsg = branding.beforeQrMsg,
        footerMsg = branding.footerMsg,
        useForceDarkTheme = branding.useForceDarkTheme,
        customBackgroundColorHex = branding.customBackgroundColorHex,
        customCardTopBorderColorHex = branding.customCardTopBorderColorHex,
        ofdTicketAds = branding.ofdTicketAds.map { TicketAdDto(it.type, it.version, it.text) },
        printOfdTicketAds = branding.printOfdTicketAds
    )

    fun toDomain(dto: ReceiptBrandingRequest): ReceiptBranding = ReceiptBranding(
        language = io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptLanguage.valueOf(
            dto.language.name
        ),
        headerLogoUrl = dto.headerLogoUrl,
        paperWidthMm = dto.paperWidthMm,
        themeColor = dto.themeColor,
        beforeHeaderMsg = dto.beforeHeaderMsg,
        headerMsg = dto.headerMsg,
        afterHeaderMsg = dto.afterHeaderMsg,
        beforeItemsMsg = dto.beforeItemsMsg,
        afterItemsMsg = dto.afterItemsMsg,
        beforeTotalsMsg = dto.beforeTotalsMsg,
        afterTotalsMsg = dto.afterTotalsMsg,
        beforeQrMsg = dto.beforeQrMsg,
        footerMsg = dto.footerMsg,
        useForceDarkTheme = dto.useForceDarkTheme,
        customBackgroundColorHex = dto.customBackgroundColorHex,
        customCardTopBorderColorHex = dto.customCardTopBorderColorHex,
        ofdTicketAds = dto.ofdTicketAds.map { DomainTicketAd(it.type, it.version, it.text) },
        printOfdTicketAds = dto.printOfdTicketAds
    )

    fun toResponse(counter: CounterSnapshot): CounterSnapshotResponse = CounterSnapshotResponse(
        scope = counter.scope,
        shiftId = counter.shiftId,
        key = counter.key,
        value = counter.value,
        updatedAt = counter.updatedAt
    )

    fun toResponse(res: CashOperationResult): CashOperationResponse = CashOperationResponse(
        documentId = res.documentId,
        deliveryStatus = DeliveryStatus.valueOf(res.deliveryStatus.name),
        deliveryError = res.deliveryError
    )

    fun toDomain(dto: CashOperationRequest, pin: String): DomainCashOperationRequest = DomainCashOperationRequest(
        pin = pin,
        amount = dto.amount,
        idempotencyKey = dto.idempotencyKey
    )

    fun toResponse(doc: FiscalDocumentSnapshot): FiscalDocumentResponse = FiscalDocumentResponse(
        id = doc.id,
        cashboxId = doc.cashboxId,
        shiftId = doc.shiftId,
        docType = ReceiptDocumentTypes.canonical(doc.docType),
        docNo = doc.docNo,
        // Номер, которым чек назван на бумаге: у ОФД номер свой, и у чека,
        // который ОФД отверг, его нет вовсе — кассиру не с чем сверять.
        printedDocumentNumber = doc.printedDocumentNumber,
        shiftNo = doc.shiftNo,
        createdAt = doc.createdAt,
        totalAmount = doc.totalAmount,
        currency = doc.currency,
        fiscalSign = doc.fiscalSign,
        autonomousSign = doc.autonomousSign,
        isAutonomous = doc.isAutonomous,
        ofdStatus = doc.ofdStatus,
        // Код отказа доходил до представления, но в ответ не попадал:
        // кассир видел «ошибка» и ни слова о причине.
        ofdErrorCode = doc.ofdErrorCode,
        // Причина словами: по одному коду обслуживание её не находит.
        ofdErrorText = doc.ofdErrorText,
        deliveredAt = doc.deliveredAt,
        receiptUrl = doc.receiptUrl,
        registrationNumber = doc.registrationNumber,
        taxpayerName = doc.taxpayerName,
        taxpayerBin = doc.taxpayerBin,
        taxpayerAddress = doc.taxpayerAddress,
        factoryNumber = doc.factoryNumber,
        ofdProvider = doc.ofdProvider
    )

    fun toResponse(kkm: KkmInfo): KkmResponse {
        val (ofdId, ofdEnvironment) = splitOfdTag(kkm.ofdProvider)
        return KkmResponse(
            kkmId = kkm.id,
            createdAt = kkm.createdAt,
            updatedAt = kkm.updatedAt,
            name = kkm.name,
            mode = kkm.mode,
            state = kkm.state,
            ofdId = ofdId,
            ofdEnvironment = ofdEnvironment,
            kkmKgdId = kkm.registrationNumber,
            factoryNumber = kkm.factoryNumber,
            manufactureYear = kkm.manufactureYear,
            ofdSystemId = kkm.systemId,
            ofdServiceInfo = kkm.ofdServiceInfo?.let { toResponse(it) },
            tokenEncryptedBase64 = kkm.tokenEncryptedBase64,
            tokenUpdatedAt = kkm.tokenUpdatedAt,
            lastShiftNo = kkm.lastShiftNo,
            lastReceiptNo = kkm.lastReceiptNo,
            lastZReportNo = kkm.lastZReportNo,
            autonomousSince = kkm.autonomousSince,
            autoCloseShift = kkm.autoCloseShift,
            autoCashout = kkm.autoCashout,
            lastFiscalHashBase64 = kkm.lastFiscalHashBase64,
            taxRegime = kkm.taxRegime.name,
            defaultVatGroup = kkm.defaultVatGroup.name,
            branding = kkm.branding.let { toResponse(it) },
            blockReasonCode = kkm.blockReasonCode,
            isProgrammingMode = kkm.mode == io.github.texport.superkassa.core.domain.api.model.kkm.KkmMode.PROGRAMMING.name || kkm.state == io.github.texport.superkassa.core.domain.api.model.kkm.KkmState.PROGRAMMING.name,
            isTokenValid = !kkm.tokenEncryptedBase64.isNullOrEmpty()
        )
    }

    private fun splitOfdTag(tag: String?): Pair<String?, String?> {
        if (tag == null) return null to null
        val parts = tag.split(":")
        return if (parts.size == 2) parts[0] to parts[1] else tag to null
    }
}
