package io.github.texport.superkassa.core.presentation.impl.mapper

import io.github.texport.superkassa.core.presentation.api.model.reference.*
import io.github.texport.superkassa.core.presentation.api.model.receipt.DocumentType
import io.github.texport.superkassa.core.presentation.api.model.receipt.BrandingColor
import io.github.texport.superkassa.core.presentation.api.model.receipt.PaperWidth
import io.github.texport.superkassa.core.string.api.CoreStrings

// Domain Enums
import io.github.texport.superkassa.core.domain.api.model.receipt.PaymentType as DomainPaymentType
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole as DomainUserRole
import io.github.texport.superkassa.core.domain.api.model.common.TaxRegime as DomainTaxRegime
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup as DomainVatGroup
import io.github.texport.superkassa.core.domain.api.model.common.UnitOfMeasurement as DomainUnitOfMeasurement
import io.github.texport.superkassa.core.domain.api.model.kkm.CashOperationType as DomainCashOperationType
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmMode as DomainKkmMode
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState as DomainKkmState
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus as DomainOfdCommandStatus
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType as DomainOfdCommandType
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdEnvironment as DomainOfdEnvironment
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdProvider as DomainOfdProvider
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptLanguage as DomainReceiptLanguage
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptLayoutType as DomainReceiptLayoutType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType as DomainReceiptOperationType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptDomainType as DomainReceiptDomainType
import io.github.texport.superkassa.core.domain.api.model.report.PrintDocumentType as DomainPrintDocumentType
import io.github.texport.superkassa.core.domain.api.model.settings.CoreMode as DomainCoreMode
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftStatus as DomainShiftStatus
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryStatus as DomainDeliveryStatus

// Presentation Enums
import io.github.texport.superkassa.core.presentation.api.model.receipt.PaymentType as PresPaymentType
import io.github.texport.superkassa.core.presentation.api.model.user.UserRole as PresUserRole
import io.github.texport.superkassa.core.presentation.api.model.kkm.TaxRegime as PresTaxRegime
import io.github.texport.superkassa.core.presentation.api.model.kkm.VatGroup as PresVatGroup
import io.github.texport.superkassa.core.presentation.api.model.common.UnitOfMeasurement as PresUnitOfMeasurement
import io.github.texport.superkassa.core.presentation.api.model.kkm.CashOperationType as PresCashOperationType
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmMode as PresKkmMode
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmState as PresKkmState
import io.github.texport.superkassa.core.presentation.api.model.ofd.OfdCommandStatus as PresOfdCommandStatus
import io.github.texport.superkassa.core.presentation.api.model.ofd.OfdCommandType as PresOfdCommandType
import io.github.texport.superkassa.core.presentation.api.model.ofd.OfdEnvironment as PresOfdEnvironment
import io.github.texport.superkassa.core.presentation.api.model.ofd.OfdProvider as PresOfdProvider
import io.github.texport.superkassa.core.presentation.api.model.kkm.ReceiptLanguage as PresReceiptLanguage
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptLayoutType as PresReceiptLayoutType
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptOperationType as PresReceiptOperationType
import io.github.texport.superkassa.core.presentation.api.model.receipt.PrintDocumentType as PresPrintDocumentType
import io.github.texport.superkassa.core.presentation.api.model.kkm.CoreMode as PresCoreMode
import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftStatus as PresShiftStatus
import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus as PresDeliveryStatus
import io.github.texport.superkassa.core.presentation.api.model.auth.AuthMode as PresAuthMode

// Mapping Extensions: Domain to Presentation
fun DomainPaymentType.toPresentation(): PresPaymentType = PresPaymentType.valueOf(this.name)
fun DomainUserRole.toPresentation(): PresUserRole = PresUserRole.valueOf(this.name)
fun DomainTaxRegime.toPresentation(): PresTaxRegime = PresTaxRegime.valueOf(this.name)
fun DomainVatGroup.toPresentation(): PresVatGroup = PresVatGroup.valueOf(this.name)
fun DomainUnitOfMeasurement.toPresentation(): PresUnitOfMeasurement = PresUnitOfMeasurement.valueOf(this.name)
fun DomainCashOperationType.toPresentation(): PresCashOperationType = PresCashOperationType.valueOf(this.name)
fun DomainKkmMode.toPresentation(): PresKkmMode = PresKkmMode.valueOf(this.name)
fun DomainKkmState.toPresentation(): PresKkmState = PresKkmState.valueOf(this.name)
fun DomainOfdCommandStatus.toPresentation(): PresOfdCommandStatus = PresOfdCommandStatus.valueOf(this.name)
fun DomainOfdCommandType.toPresentation(): PresOfdCommandType = PresOfdCommandType.valueOf(this.name)
fun DomainOfdEnvironment.toPresentation(): PresOfdEnvironment = PresOfdEnvironment.valueOf(this.name)
fun DomainOfdProvider.toPresentation(): PresOfdProvider = PresOfdProvider.valueOf(this.name)
fun DomainReceiptLanguage.toPresentation(): PresReceiptLanguage = PresReceiptLanguage.valueOf(this.name)
fun DomainReceiptLayoutType.toPresentation(): PresReceiptLayoutType = PresReceiptLayoutType.valueOf(this.name)
fun DomainReceiptOperationType.toPresentation(): PresReceiptOperationType = PresReceiptOperationType.valueOf(this.name)
fun DomainPrintDocumentType.toPresentation(): PresPrintDocumentType = PresPrintDocumentType.valueOf(this.name)
fun DomainCoreMode.toPresentation(): PresCoreMode = PresCoreMode.valueOf(this.name)
fun DomainShiftStatus.toPresentation(): PresShiftStatus = PresShiftStatus.valueOf(this.name)
fun DomainDeliveryStatus.toPresentation(): PresDeliveryStatus = PresDeliveryStatus.valueOf(this.name)

// Mapping Extensions: Presentation to Domain
fun PresPaymentType.toDomain(): DomainPaymentType = DomainPaymentType.valueOf(this.name)
fun PresUserRole.toDomain(): DomainUserRole = DomainUserRole.valueOf(this.name)
fun PresTaxRegime.toDomain(): DomainTaxRegime = DomainTaxRegime.valueOf(this.name)
fun PresVatGroup.toDomain(): DomainVatGroup = DomainVatGroup.valueOf(this.name)
fun PresUnitOfMeasurement.toDomain(): DomainUnitOfMeasurement = DomainUnitOfMeasurement.valueOf(this.name)
fun PresCashOperationType.toDomain(): DomainCashOperationType = DomainCashOperationType.valueOf(this.name)
fun PresKkmMode.toDomain(): DomainKkmMode = DomainKkmMode.valueOf(this.name)
fun PresKkmState.toDomain(): DomainKkmState = DomainKkmState.valueOf(this.name)
fun PresOfdCommandStatus.toDomain(): DomainOfdCommandStatus = DomainOfdCommandStatus.valueOf(this.name)
fun PresOfdCommandType.toDomain(): DomainOfdCommandType = DomainOfdCommandType.valueOf(this.name)
fun PresOfdEnvironment.toDomain(): DomainOfdEnvironment = DomainOfdEnvironment.valueOf(this.name)
fun PresOfdProvider.toDomain(): DomainOfdProvider = DomainOfdProvider.valueOf(this.name)
fun PresReceiptLanguage.toDomain(): DomainReceiptLanguage = DomainReceiptLanguage.valueOf(this.name)
fun PresReceiptLayoutType.toDomain(): DomainReceiptLayoutType = DomainReceiptLayoutType.valueOf(this.name)
fun PresReceiptOperationType.toDomain(): DomainReceiptOperationType = DomainReceiptOperationType.valueOf(this.name)
fun PresPrintDocumentType.toDomain(): DomainPrintDocumentType = DomainPrintDocumentType.valueOf(this.name)
fun PresCoreMode.toDomain(): DomainCoreMode = DomainCoreMode.valueOf(this.name)
fun PresShiftStatus.toDomain(): DomainShiftStatus = DomainShiftStatus.valueOf(this.name)
fun PresDeliveryStatus.toDomain(): DomainDeliveryStatus = DomainDeliveryStatus.valueOf(this.name)

object ReferenceMapper {
    fun toResponse(value: PresPaymentType, supported: Boolean): PaymentTypeResponse = PaymentTypeResponse(
        code = value.name,
        name = TrilingualMessageResponse.from(CoreStrings.paymentType(value.name)),
        supported = supported
    )

    fun toResponse(value: DomainReceiptDomainType): ReceiptDomainTypeResponse = ReceiptDomainTypeResponse(
        code = value.name,
        name = TrilingualMessageResponse.from(CoreStrings.receiptDomainType(value.name))
    )

    fun toResponse(value: PresUserRole): UserRoleResponse = UserRoleResponse(
        code = value.name,
        name = TrilingualMessageResponse.from(CoreStrings.userRole(value.name))
    )

    fun toResponse(value: PresTaxRegime): TaxRegimeResponse = TaxRegimeResponse(
        code = value.name,
        name = TrilingualMessageResponse.from(CoreStrings.taxRegime(value.name))
    )

    fun toResponse(value: PresCashOperationType): CashOperationTypeResponse = CashOperationTypeResponse(
        code = value.name,
        name = TrilingualMessageResponse.from(CoreStrings.cashOperationType(value.name))
    )

    fun toResponse(value: PresKkmMode): KkmModeResponse = KkmModeResponse(
        code = value.name,
        name = TrilingualMessageResponse.from(CoreStrings.kkmMode(value.name))
    )

    fun toResponse(value: PresKkmState): KkmStateResponse = KkmStateResponse(
        code = value.name,
        name = TrilingualMessageResponse.from(CoreStrings.kkmState(value.name))
    )

    fun toResponse(value: PresOfdCommandStatus): OfdCommandStatusResponse = OfdCommandStatusResponse(
        code = value.name,
        name = TrilingualMessageResponse.from(CoreStrings.ofdCommandStatus(value.name))
    )

    fun toResponse(value: PresOfdCommandType): OfdCommandTypeResponse = OfdCommandTypeResponse(
        code = value.name,
        name = TrilingualMessageResponse.from(CoreStrings.ofdCommandType(value.name))
    )

    fun toResponse(value: PresOfdEnvironment): OfdEnvironmentResponse = OfdEnvironmentResponse(
        code = value.name,
        name = TrilingualMessageResponse.from(CoreStrings.ofdEnvironment(value.name))
    )

    fun toResponse(value: PresOfdProvider): OfdProviderResponse {
        val domain = value.toDomain()
        return OfdProviderResponse(
            code = value.name,
            name = TrilingualMessageResponse.from(CoreStrings.ofdProvider(value.name)),
            website = domain.website
        )
    }

    fun toResponse(value: PresReceiptLanguage): ReceiptLanguageResponse = ReceiptLanguageResponse(
        code = value.name,
        name = TrilingualMessageResponse.from(CoreStrings.receiptLanguage(value.name))
    )

    fun toResponse(value: PresReceiptLayoutType): ReceiptLayoutTypeResponse = ReceiptLayoutTypeResponse(
        code = value.name,
        name = TrilingualMessageResponse.from(CoreStrings.receiptLayoutType(value.name))
    )

    fun toResponse(value: PresReceiptOperationType): ReceiptOperationTypeResponse = ReceiptOperationTypeResponse(
        code = value.name,
        name = TrilingualMessageResponse.from(CoreStrings.receiptOperationType(value.name))
    )

    fun toResponse(value: PresPrintDocumentType): PrintDocumentTypeResponse = PrintDocumentTypeResponse(
        code = value.name,
        name = TrilingualMessageResponse.from(CoreStrings.printDocumentType(value.name))
    )

    fun toResponse(value: PresCoreMode): CoreModeResponse = CoreModeResponse(
        code = value.name,
        name = TrilingualMessageResponse.from(CoreStrings.coreMode(value.name))
    )

    fun toResponse(value: PresShiftStatus): ShiftStatusResponse = ShiftStatusResponse(
        code = value.name,
        name = TrilingualMessageResponse.from(CoreStrings.shiftStatus(value.name))
    )

    fun toResponse(value: PresDeliveryStatus): DeliveryStatusResponse = DeliveryStatusResponse(
        code = value.name,
        name = TrilingualMessageResponse.from(CoreStrings.deliveryStatus(value.name))
    )

    fun toResponse(value: PresAuthMode): AuthModeResponse = AuthModeResponse(
        code = value.name,
        name = TrilingualMessageResponse.from(CoreStrings.authMode(value.name))
    )

    fun toResponse(value: DocumentType): DocumentTypeResponse = DocumentTypeResponse(
        code = value.name,
        name = TrilingualMessageResponse.from(CoreStrings.documentType(value.name))
    )

    fun toResponse(value: BrandingColor): BrandingColorResponse = BrandingColorResponse(
        code = value.hexCode,
        name = TrilingualMessageResponse.from(CoreStrings.brandingColor(value.name))
    )

    fun toResponse(value: PaperWidth): PaperWidthResponse = PaperWidthResponse(
        code = value.widthCode,
        name = TrilingualMessageResponse.from(CoreStrings.paperWidth(value.name))
    )
}
