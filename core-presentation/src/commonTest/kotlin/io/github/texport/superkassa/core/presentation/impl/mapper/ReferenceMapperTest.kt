package io.github.texport.superkassa.core.presentation.impl.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

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
import io.github.texport.superkassa.core.presentation.api.model.receipt.DocumentType as PresDocumentType
import io.github.texport.superkassa.core.presentation.api.model.receipt.BrandingColor as PresBrandingColor
import io.github.texport.superkassa.core.presentation.api.model.receipt.PaperWidth as PresPaperWidth

class ReferenceMapperTest {

    @Test
    fun testPaymentTypeMapping() {
        val domain = DomainPaymentType.entries.first()
        val pres = domain.toPresentation()
        assertEquals(domain.name, pres.name)
        assertEquals(domain, pres.toDomain())
        val response = ReferenceMapper.toResponse(pres, supported = true)
        assertEquals(pres.name, response.code)
        assertTrue(response.supported)
    }

    @Test
    fun testPaymentTypeUnsupportedByProtocol() {
        val pres = DomainPaymentType.CREDIT.toPresentation()
        val response = ReferenceMapper.toResponse(pres, supported = false)
        assertEquals("CREDIT", response.code)
        assertFalse(response.supported)
    }

    @Test
    fun testUserRoleMapping() {
        val domain = DomainUserRole.entries.first()
        val pres = domain.toPresentation()
        assertEquals(domain.name, pres.name)
        assertEquals(domain, pres.toDomain())
        val response = ReferenceMapper.toResponse(pres)
        assertEquals(pres.name, response.code)
    }

    @Test
    fun testTaxRegimeMapping() {
        val domain = DomainTaxRegime.entries.first()
        val pres = domain.toPresentation()
        assertEquals(domain.name, pres.name)
        assertEquals(domain, pres.toDomain())
        val response = ReferenceMapper.toResponse(pres)
        assertEquals(pres.name, response.code)
    }

    @Test
    fun testVatGroupMapping() {
        val domain = DomainVatGroup.entries.first()
        val pres = domain.toPresentation()
        assertEquals(domain.name, pres.name)
        assertEquals(domain, pres.toDomain())
    }

    @Test
    fun testUnitOfMeasurementMapping() {
        val domain = DomainUnitOfMeasurement.entries.first()
        val pres = domain.toPresentation()
        assertEquals(domain.name, pres.name)
        assertEquals(domain, pres.toDomain())
    }

    @Test
    fun testCashOperationTypeMapping() {
        val domain = DomainCashOperationType.entries.first()
        val pres = domain.toPresentation()
        assertEquals(domain.name, pres.name)
        assertEquals(domain, pres.toDomain())
        val response = ReferenceMapper.toResponse(pres)
        assertEquals(pres.name, response.code)
    }

    @Test
    fun testKkmModeMapping() {
        val domain = DomainKkmMode.entries.first()
        try {
            domain.toPresentation()
        } catch (e: IllegalArgumentException) {
            // expected due to concept mismatch
        }
        val pres = PresKkmMode.entries.first()
        try {
            pres.toDomain()
        } catch (e: IllegalArgumentException) {
            // expected due to concept mismatch
        }
        val response = ReferenceMapper.toResponse(pres)
        assertEquals(pres.name, response.code)
    }

    @Test
    fun testKkmStateMapping() {
        val domain = DomainKkmState.ACTIVE
        val pres = domain.toPresentation()
        assertEquals(PresKkmState.ACTIVE, pres)
        assertEquals(domain, pres.toDomain())

        try {
            DomainKkmState.IDLE.toPresentation()
        } catch (e: IllegalArgumentException) {
            // expected
        }
        try {
            PresKkmState.REGISTRATION.toDomain()
        } catch (e: IllegalArgumentException) {
            // expected
        }

        val response = ReferenceMapper.toResponse(pres)
        assertEquals(pres.name, response.code)
    }

    @Test
    fun testOfdCommandStatusMapping() {
        val domain = DomainOfdCommandStatus.entries.first()
        val pres = domain.toPresentation()
        assertEquals(domain.name, pres.name)
        assertEquals(domain, pres.toDomain())
        val response = ReferenceMapper.toResponse(pres)
        assertEquals(pres.name, response.code)
    }

    @Test
    fun testOfdCommandTypeMapping() {
        val domain = DomainOfdCommandType.entries.first()
        val pres = domain.toPresentation()
        assertEquals(domain.name, pres.name)
        assertEquals(domain, pres.toDomain())
        val response = ReferenceMapper.toResponse(pres)
        assertEquals(pres.name, response.code)
    }

    @Test
    fun testOfdEnvironmentMapping() {
        val domain = DomainOfdEnvironment.entries.first()
        val pres = domain.toPresentation()
        assertEquals(domain.name, pres.name)
        assertEquals(domain, pres.toDomain())
        val response = ReferenceMapper.toResponse(pres)
        assertEquals(pres.name, response.code)
    }

    @Test
    fun testOfdProviderMapping() {
        val domain = DomainOfdProvider.entries.first()
        val pres = domain.toPresentation()
        assertEquals(domain.name, pres.name)
        assertEquals(domain, pres.toDomain())
        val response = ReferenceMapper.toResponse(pres)
        assertEquals(pres.name, response.code)
        assertNotNull(response.website)
    }

    @Test
    fun testReceiptLanguageMapping() {
        val domain = DomainReceiptLanguage.entries.first()
        val pres = domain.toPresentation()
        assertEquals(domain.name, pres.name)
        assertEquals(domain, pres.toDomain())
        val response = ReferenceMapper.toResponse(pres)
        assertEquals(pres.name, response.code)
    }

    @Test
    fun testReceiptLayoutTypeMapping() {
        val domain = DomainReceiptLayoutType.entries.first()
        val pres = domain.toPresentation()
        assertEquals(domain.name, pres.name)
        assertEquals(domain, pres.toDomain())
        val response = ReferenceMapper.toResponse(pres)
        assertEquals(pres.name, response.code)
    }

    @Test
    fun testReceiptOperationTypeMapping() {
        val domain = DomainReceiptOperationType.entries.first()
        val pres = domain.toPresentation()
        assertEquals(domain.name, pres.name)
        assertEquals(domain, pres.toDomain())
        val response = ReferenceMapper.toResponse(pres)
        assertEquals(pres.name, response.code)
    }

    @Test
    fun testPrintDocumentTypeMapping() {
        val domain = DomainPrintDocumentType.entries.first()
        val pres = domain.toPresentation()
        assertEquals(domain.name, pres.name)
        assertEquals(domain, pres.toDomain())
        val response = ReferenceMapper.toResponse(pres)
        assertEquals(pres.name, response.code)
    }

    @Test
    fun testCoreModeMapping() {
        val domain = DomainCoreMode.entries.first()
        val pres = domain.toPresentation()
        assertEquals(domain.name, pres.name)
        assertEquals(domain, pres.toDomain())
        val response = ReferenceMapper.toResponse(pres)
        assertEquals(pres.name, response.code)
    }

    @Test
    fun testShiftStatusMapping() {
        val domain = DomainShiftStatus.entries.first()
        val pres = domain.toPresentation()
        assertEquals(domain.name, pres.name)
        assertEquals(domain, pres.toDomain())
        val response = ReferenceMapper.toResponse(pres)
        assertEquals(pres.name, response.code)
    }

    @Test
    fun testDeliveryStatusMapping() {
        val domain = DomainDeliveryStatus.entries.first()
        val pres = domain.toPresentation()
        assertEquals(domain.name, pres.name)
        assertEquals(domain, pres.toDomain())
        val response = ReferenceMapper.toResponse(pres)
        assertEquals(pres.name, response.code)
    }

    @Test
    fun testAuthModeMapping() {
        val pres = PresAuthMode.entries.first()
        val response = ReferenceMapper.toResponse(pres)
        assertEquals(pres.name, response.code)
    }

    @Test
    fun testDocumentTypeMapping() {
        val pres = PresDocumentType.entries.first()
        val response = ReferenceMapper.toResponse(pres)
        assertEquals(pres.name, response.code)
    }

    @Test
    fun testBrandingColorMapping() {
        val pres = PresBrandingColor.entries.first()
        val response = ReferenceMapper.toResponse(pres)
        assertEquals(pres.hexCode, response.code)
    }

    @Test
    fun testPaperWidthMapping() {
        val pres = PresPaperWidth.entries.first()
        val response = ReferenceMapper.toResponse(pres)
        assertEquals(pres.widthCode, response.code)
    }
}
