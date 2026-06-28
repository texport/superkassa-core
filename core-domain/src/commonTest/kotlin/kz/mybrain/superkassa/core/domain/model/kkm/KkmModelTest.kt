package kz.mybrain.superkassa.core.domain.model.kkm

import kz.mybrain.superkassa.core.domain.model.common.TaxRegime
import kz.mybrain.superkassa.core.domain.model.common.VatGroup
import kz.mybrain.superkassa.core.domain.model.delivery.DeliveryStatus
import kz.mybrain.superkassa.core.domain.model.ofd.OfdServiceInfo
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptBranding
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class KkmModelTest {

    @Test
    fun testCashOperationTypeEnum() {
        for (item in CashOperationType.entries) {
            val value = CashOperationType.valueOf(item.name)
            assertEquals(item, value)
        }
    }

    @Test
    fun testKkmStateEnum() {
        for (item in KkmState.entries) {
            val value = KkmState.valueOf(item.name)
            assertEquals(item, value)
        }
    }

    @Test
    fun testKkmModeEnum() {
        for (item in KkmMode.entries) {
            val value = KkmMode.valueOf(item.name)
            assertEquals(item, value)
        }
    }

    @Test
    fun testCashOperationResult() {
        val res1 = CashOperationResult("doc-1", DeliveryStatus.ONLINE_OK, null)
        val res1Copy = res1.copy()
        val resSame = CashOperationResult("doc-1", DeliveryStatus.ONLINE_OK, null)
        val resDiffDoc = res1.copy(documentId = "doc-2")
        val resDiffStatus = res1.copy(deliveryStatus = DeliveryStatus.NOT_SENT)
        val resDiffError = res1.copy(deliveryError = "error")

        assertEquals(res1, res1)
        assertEquals(res1, res1Copy)
        assertEquals(res1, resSame)
        assertEquals(res1.hashCode(), resSame.hashCode())

        assertNotEquals(res1, Any())
        assertFalse(res1.equals(null))
        assertNotEquals(res1, resDiffDoc)
        assertNotEquals(res1, resDiffStatus)
        assertNotEquals(res1, resDiffError)

        assertTrue(res1.toString().contains("documentId=doc-1"))
    }

    @Test
    fun testFactoryInfo() {
        val f1 = FactoryInfo("fac-1", 2026)
        val f1Copy = f1.copy()
        val fSame = FactoryInfo("fac-1", 2026)
        val fDiffNum = f1.copy(factoryNumber = "fac-2")
        val fDiffYear = f1.copy(manufactureYear = 2027)

        assertEquals(f1, f1)
        assertEquals(f1, f1Copy)
        assertEquals(f1, fSame)
        assertEquals(f1.hashCode(), fSame.hashCode())

        assertNotEquals(f1, Any())
        assertFalse(f1.equals(null))
        assertNotEquals(f1, fDiffNum)
        assertNotEquals(f1, fDiffYear)

        assertTrue(f1.toString().contains("factoryNumber=fac-1"))
    }

    @Test
    fun testCashOperationRequest() {
        val req1 = CashOperationRequest("1111", 500.0, "key-1")
        val req1Copy = req1.copy()
        val reqSame = CashOperationRequest("1111", 500.0, "key-1")
        val reqDiffPin = req1.copy(pin = "2222")
        val reqDiffAmount = req1.copy(amount = 600.0)
        val reqDiffKey = req1.copy(idempotencyKey = "key-2")

        assertEquals(req1, req1)
        assertEquals(req1, req1Copy)
        assertEquals(req1, reqSame)
        assertEquals(req1.hashCode(), reqSame.hashCode())

        assertNotEquals(req1, Any())
        assertFalse(req1.equals(null))
        assertNotEquals(req1, reqDiffPin)
        assertNotEquals(req1, reqDiffAmount)
        assertNotEquals(req1, reqDiffKey)

        assertTrue(req1.toString().contains("pin=1111"))

        // test custom copy method
        val reqCustomCopy = req1.copy("9999")
        assertEquals("9999", reqCustomCopy.pin)
        assertEquals(req1.amount, reqCustomCopy.amount)
        assertEquals(req1.idempotencyKey, reqCustomCopy.idempotencyKey)
    }

    @Test
    fun testFiscalDocumentSnapshot() {
        val doc1 = FiscalDocumentSnapshot(
            id = "id1", cashboxId = "c1", shiftId = "s1", docType = "TICKET",
            docNo = 10L, shiftNo = 2L, createdAt = 1000L, totalAmount = 500L,
            currency = "KZT", fiscalSign = "fs", autonomousSign = "as",
            isAutonomous = false, ofdStatus = "status", deliveredAt = 2000L,
            receiptUrl = "url", registrationNumber = "reg", taxpayerName = "taxName",
            taxpayerBin = "taxBin", taxpayerAddress = "addr", factoryNumber = "fac",
            ofdProvider = "provider"
        )
        val doc1Copy = doc1.copy()
        val docSame = doc1.copy()
        val docDiffId = doc1.copy(id = "id2")
        val docDiffCashbox = doc1.copy(cashboxId = "c2")
        val docDiffShift = doc1.copy(shiftId = "s2")
        val docDiffType = doc1.copy(docType = "REPORT")
        val docDiffDocNo = doc1.copy(docNo = 11L)
        val docDiffShiftNo = doc1.copy(shiftNo = 3L)
        val docDiffCreated = doc1.copy(createdAt = 1001L)
        val docDiffTotal = doc1.copy(totalAmount = 600L)
        val docDiffCurrency = doc1.copy(currency = "USD")
        val docDiffFs = doc1.copy(fiscalSign = "fs2")
        val docDiffAs = doc1.copy(autonomousSign = "as2")
        val docDiffAuto = doc1.copy(isAutonomous = true)
        val docDiffOfd = doc1.copy(ofdStatus = "status2")
        val docDiffDelivered = doc1.copy(deliveredAt = 2001L)
        val docDiffUrl = doc1.copy(receiptUrl = "url2")
        val docDiffReg = doc1.copy(registrationNumber = "reg2")
        val docDiffTaxName = doc1.copy(taxpayerName = "taxName2")
        val docDiffTaxBin = doc1.copy(taxpayerBin = "taxBin2")
        val docDiffAddr = doc1.copy(taxpayerAddress = "addr2")
        val docDiffFac = doc1.copy(factoryNumber = "fac2")
        val docDiffProvider = doc1.copy(ofdProvider = "provider2")

        assertEquals(doc1, doc1)
        assertEquals(doc1, doc1Copy)
        assertEquals(doc1, docSame)
        assertEquals(doc1.hashCode(), docSame.hashCode())

        assertNotEquals(doc1, Any())
        assertFalse(doc1.equals(null))
        assertNotEquals(doc1, docDiffId)
        assertNotEquals(doc1, docDiffCashbox)
        assertNotEquals(doc1, docDiffShift)
        assertNotEquals(doc1, docDiffType)
        assertNotEquals(doc1, docDiffDocNo)
        assertNotEquals(doc1, docDiffShiftNo)
        assertNotEquals(doc1, docDiffCreated)
        assertNotEquals(doc1, docDiffTotal)
        assertNotEquals(doc1, docDiffCurrency)
        assertNotEquals(doc1, docDiffFs)
        assertNotEquals(doc1, docDiffAs)
        assertNotEquals(doc1, docDiffAuto)
        assertNotEquals(doc1, docDiffOfd)
        assertNotEquals(doc1, docDiffDelivered)
        assertNotEquals(doc1, docDiffUrl)
        assertNotEquals(doc1, docDiffReg)
        assertNotEquals(doc1, docDiffTaxName)
        assertNotEquals(doc1, docDiffTaxBin)
        assertNotEquals(doc1, docDiffAddr)
        assertNotEquals(doc1, docDiffFac)
        assertNotEquals(doc1, docDiffProvider)

        assertTrue(doc1.toString().contains("id=id1"))
    }

    @Test
    fun testKkmInfo() {
        val service = OfdServiceInfo("title", "addr", "addrKz", "inn", "okved", 10, 20, "GPS")
        val branding = ReceiptBranding(headerLogoUrl = "logo", headerMsg = "header", footerMsg = "footer")
        val k1 = KkmInfo(
            id = "kkm-1", createdAt = 1000L, updatedAt = 2000L, mode = "ACTIVE", state = "ACTIVE",
            ofdProvider = "telecom", registrationNumber = "reg", factoryNumber = "fac",
            manufactureYear = 2026, systemId = "sys", ofdServiceInfo = service,
            tokenEncryptedBase64 = "token", tokenUpdatedAt = 3000L, lastShiftNo = 5,
            lastReceiptNo = 10, lastZReportNo = 4, autonomousSince = 4000L,
            autoCloseShift = true, lastFiscalHashBase64 = "hash", taxRegime = TaxRegime.VAT_PAYER,
            defaultVatGroup = VatGroup.VAT_10, branding = branding
        )
        // Note: VatGroup.VAT_12 is not in VatGroup, let's look up VatGroup enum values:
        // NO_VAT, VAT_0, VAT_5, VAT_10, VAT_16. Let's use VAT_10.
        val k1Correct = k1.copy(defaultVatGroup = VatGroup.VAT_10)
        
        val k1Copy = k1Correct.copy()
        val kSame = k1Correct.copy()
        val kDiffId = k1Correct.copy(id = "kkm-2")
        val kDiffCreated = k1Correct.copy(createdAt = 1001L)
        val kDiffUpdated = k1Correct.copy(updatedAt = 2001L)
        val kDiffMode = k1Correct.copy(mode = "PROGRAMMING")
        val kDiffState = k1Correct.copy(state = "BLOCKED")
        val kDiffProvider = k1Correct.copy(ofdProvider = "other")
        val kDiffReg = k1Correct.copy(registrationNumber = "reg2")
        val kDiffFac = k1Correct.copy(factoryNumber = "fac2")
        val kDiffYear = k1Correct.copy(manufactureYear = 2027)
        val kDiffSys = k1Correct.copy(systemId = "sys2")
        val kDiffService = k1Correct.copy(ofdServiceInfo = null)
        val kDiffTokenEnc = k1Correct.copy(tokenEncryptedBase64 = "token2")
        val kDiffTokenUpd = k1Correct.copy(tokenUpdatedAt = 3001L)
        val kDiffShift = k1Correct.copy(lastShiftNo = 6)
        val kDiffReceipt = k1Correct.copy(lastReceiptNo = 11)
        val kDiffZReport = k1Correct.copy(lastZReportNo = 5)
        val kDiffAutonomous = k1Correct.copy(autonomousSince = 4001L)
        val kDiffAutoClose = k1Correct.copy(autoCloseShift = false)
        val kDiffHash = k1Correct.copy(lastFiscalHashBase64 = "hash2")
        val kDiffTaxRegime = k1Correct.copy(taxRegime = TaxRegime.MIXED)
        val kDiffVatGroup = k1Correct.copy(defaultVatGroup = VatGroup.NO_VAT)
        val kDiffBranding = k1Correct.copy(branding = ReceiptBranding())

        assertEquals(k1Correct, k1Correct)
        assertEquals(k1Correct, k1Copy)
        assertEquals(k1Correct, kSame)
        assertEquals(k1Correct.hashCode(), kSame.hashCode())

        assertNotEquals(k1Correct, Any())
        assertFalse(k1Correct.equals(null))
        assertNotEquals(k1Correct, kDiffId)
        assertNotEquals(k1Correct, kDiffCreated)
        assertNotEquals(k1Correct, kDiffUpdated)
        assertNotEquals(k1Correct, kDiffMode)
        assertNotEquals(k1Correct, kDiffState)
        assertNotEquals(k1Correct, kDiffProvider)
        assertNotEquals(k1Correct, kDiffReg)
        assertNotEquals(k1Correct, kDiffFac)
        assertNotEquals(k1Correct, kDiffYear)
        assertNotEquals(k1Correct, kDiffSys)
        assertNotEquals(k1Correct, kDiffService)
        assertNotEquals(k1Correct, kDiffTokenEnc)
        assertNotEquals(k1Correct, kDiffTokenUpd)
        assertNotEquals(k1Correct, kDiffShift)
        assertNotEquals(k1Correct, kDiffReceipt)
        assertNotEquals(k1Correct, kDiffZReport)
        assertNotEquals(k1Correct, kDiffAutonomous)
        assertNotEquals(k1Correct, kDiffAutoClose)
        assertNotEquals(k1Correct, kDiffHash)
        assertNotEquals(k1Correct, kDiffTaxRegime)
        assertNotEquals(k1Correct, kDiffVatGroup)
        assertNotEquals(k1Correct, kDiffBranding)

        assertTrue(k1Correct.toString().contains("id=kkm-1"))
    }
}
