package kz.mybrain.superkassa.core.domain.model.receipt

import kz.mybrain.superkassa.core.domain.model.common.Money
import kz.mybrain.superkassa.core.domain.model.common.TaxRegime
import kz.mybrain.superkassa.core.domain.model.common.VatGroup
import kz.mybrain.superkassa.core.domain.model.delivery.DeliveryStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ReceiptModelTest {

    @Test
    fun testEnums() {
        for (item in PaymentType.entries) {
            assertEquals(item, PaymentType.valueOf(item.name))
        }
        for (item in ReceiptLanguage.entries) {
            assertEquals(item, ReceiptLanguage.valueOf(item.name))
        }
        for (item in ReceiptLayoutType.entries) {
            assertEquals(item, ReceiptLayoutType.valueOf(item.name))
        }
        for (item in ReceiptOperationType.entries) {
            assertEquals(item, ReceiptOperationType.valueOf(item.name))
        }
    }

    @Test
    fun testParentTicket() {
        val money = Money(100L, 50)
        val p1 = ParentTicket(1L, 1000L, "kkm-1", money, false)
        val p1Copy = p1.copy()
        val pSame = ParentTicket(1L, 1000L, "kkm-1", money, false)
        val pDiffNum = p1.copy(parentTicketNumber = 2L)
        val pDiffTime = p1.copy(parentTicketDateTimeMillis = 2000L)
        val pDiffKgd = p1.copy(kgdKkmId = "kkm-2")
        val pDiffTotal = p1.copy(parentTicketTotal = Money(200L, 0))
        val pDiffOffline = p1.copy(parentTicketIsOffline = true)

        assertEquals(p1, p1)
        assertEquals(p1, p1Copy)
        assertEquals(p1, pSame)
        assertEquals(p1.hashCode(), pSame.hashCode())

        assertNotEquals(p1, Any())
        assertFalse(p1.equals(null))
        assertNotEquals(p1, pDiffNum)
        assertNotEquals(p1, pDiffTime)
        assertNotEquals(p1, pDiffKgd)
        assertNotEquals(p1, pDiffTotal)
        assertNotEquals(p1, pDiffOffline)

        assertTrue(p1.toString().contains("parentTicketNumber=1"))
    }

    @Test
    fun testReceiptBranding() {
        val b1 = ReceiptBranding(ReceiptLanguage.MIXED, "logo1", 80, "indigo")
        val b1Copy = b1.copy()
        val bSame = ReceiptBranding(ReceiptLanguage.MIXED, "logo1", 80, "indigo")
        val bDiffLogo = b1.copy(headerLogoUrl = "logo2")
        val bDiffHeader = b1.copy(headerMsg = "header2")
        val bDiffFooter = b1.copy(footerMsg = "footer2")

        assertEquals(b1, b1)
        assertEquals(b1, b1Copy)
        assertEquals(b1, bSame)
        assertEquals(b1.hashCode(), bSame.hashCode())

        assertNotEquals(b1, Any())
        assertFalse(b1.equals(null))
        assertNotEquals(b1, bDiffLogo)
        assertNotEquals(b1, bDiffHeader)
        assertNotEquals(b1, bDiffFooter)

        assertTrue(b1.toString().contains("headerLogoUrl=logo1"))
    }

    @Test
    fun testReceiptItem() {
        val price = Money(100L, 0)
        val sum = Money(200L, 0)
        val disc = Money(10L, 0)
        val mark = Money(5L, 0)
        val i1 = ReceiptItem(
            name = "item1", sectionCode = "001", quantity = 2000L, price = price, sum = sum,
            barcode = "bar", vatGroup = VatGroup.VAT_10, discount = disc, markup = mark,
            measureUnitCode = "796", listExciseStamp = listOf("excise"), ntin = "ntin1", isStorno = false
        )
        val i1Copy = i1.copy()
        val iSame = i1.copy()
        val iDiffName = i1.copy(name = "item2")
        val iDiffSection = i1.copy(sectionCode = "002")
        val iDiffQty = i1.copy(quantity = 3000L)
        val iDiffPrice = i1.copy(price = Money(150L, 0))
        val iDiffSum = i1.copy(sum = Money(300L, 0))
        val iDiffBarcode = i1.copy(barcode = "bar2")
        val iDiffVat = i1.copy(vatGroup = VatGroup.VAT_16)
        val iDiffDisc = i1.copy(discount = null)
        val iDiffMark = i1.copy(markup = null)
        val iDiffUnit = i1.copy(measureUnitCode = "116")
        val iDiffExcise = i1.copy(listExciseStamp = null)
        val iDiffNtin = i1.copy(ntin = null)
        val iDiffStorno = i1.copy(isStorno = true)

        assertEquals(i1, i1)
        assertEquals(i1, i1Copy)
        assertEquals(i1, iSame)
        assertEquals(i1.hashCode(), iSame.hashCode())

        assertNotEquals(i1, Any())
        assertFalse(i1.equals(null))
        assertNotEquals(i1, iDiffName)
        assertNotEquals(i1, iDiffSection)
        assertNotEquals(i1, iDiffQty)
        assertNotEquals(i1, iDiffPrice)
        assertNotEquals(i1, iDiffSum)
        assertNotEquals(i1, iDiffBarcode)
        assertNotEquals(i1, iDiffVat)
        assertNotEquals(i1, iDiffDisc)
        assertNotEquals(i1, iDiffMark)
        assertNotEquals(i1, iDiffUnit)
        assertNotEquals(i1, iDiffExcise)
        assertNotEquals(i1, iDiffNtin)
        assertNotEquals(i1, iDiffStorno)

        assertTrue(i1.toString().contains("name=item1"))
    }

    @Test
    fun testReceiptPayment() {
        val sum = Money(100L, 0)
        val p1 = ReceiptPayment(PaymentType.CASH, sum)
        val p1Copy = p1.copy()
        val pSame = ReceiptPayment(PaymentType.CASH, sum)
        val pDiffType = p1.copy(type = PaymentType.CARD)
        val pDiffSum = p1.copy(sum = Money(200L, 0))

        assertEquals(p1, p1)
        assertEquals(p1, p1Copy)
        assertEquals(p1, pSame)
        assertEquals(p1.hashCode(), pSame.hashCode())

        assertNotEquals(p1, Any())
        assertFalse(p1.equals(null))
        assertNotEquals(p1, pDiffType)
        assertNotEquals(p1, pDiffSum)

        assertTrue(p1.toString().contains("type=CASH"))
    }

    @Test
    fun testTaxLine() {
        val taxBase = Money(1000L, 0)
        val taxSum = Money(120L, 0)
        val t1 = TaxLine(VatGroup.VAT_10, 10, taxBase, taxSum)
        val t1Copy = t1.copy()
        val tSame = TaxLine(VatGroup.VAT_10, 10, taxBase, taxSum)
        val tDiffVat = t1.copy(vatGroup = VatGroup.VAT_16)
        val tDiffPercent = t1.copy(percent = 16)
        val tDiffBase = t1.copy(taxBase = Money(2000L, 0))
        val tDiffTaxSum = t1.copy(taxSum = Money(240L, 0))

        assertEquals(t1, t1)
        assertEquals(t1, t1Copy)
        assertEquals(t1, tSame)
        assertEquals(t1.hashCode(), tSame.hashCode())

        assertNotEquals(t1, Any())
        assertFalse(t1.equals(null))
        assertNotEquals(t1, tDiffVat)
        assertNotEquals(t1, tDiffPercent)
        assertNotEquals(t1, tDiffBase)
        assertNotEquals(t1, tDiffTaxSum)

        assertTrue(t1.toString().contains("vatGroup=VAT_10"))
    }

    @Test
    fun testTicketTaxResult() {
        val taxLines = listOf(TaxLine(VatGroup.VAT_10, 10, Money(100L, 0), Money(10L, 0)))
        val r1 = TicketTaxResult(taxLines)
        val r1Copy = r1.copy()
        val rSame = TicketTaxResult(taxLines)
        val rDiffLines = r1.copy(ticketTaxes = emptyList())

        assertEquals(r1, r1)
        assertEquals(r1, r1Copy)
        assertEquals(r1, rSame)
        assertEquals(r1.hashCode(), rSame.hashCode())

        assertNotEquals(r1, Any())
        assertFalse(r1.equals(null))
        assertNotEquals(r1, rDiffLines)

        assertTrue(r1.toString().contains("ticketTaxes="))
    }

    @Test
    fun testReceiptRequest() {
        val total = Money(100L, 0)
        val items = listOf(ReceiptItem("item", "001", 1000L, total, total))
        val payments = listOf(ReceiptPayment(PaymentType.CASH, total))
        val parent = ParentTicket(1L, 1000L, "kkm-1", total, false)
        val taxes = listOf(TaxLine(VatGroup.VAT_10, 10, total, Money(10L, 0)))

        val req1 = ReceiptRequest(
            kkmId = "kkm-1", pin = "1111", operation = ReceiptOperationType.SELL,
            items = items, payments = payments, total = total, taken = total, change = Money(0L, 0),
            idempotencyKey = "key-1", parentTicket = parent, taxRegime = TaxRegime.VAT_PAYER,
            defaultVatGroup = VatGroup.VAT_10, discount = Money(5L, 0), markup = Money(2L, 0),
            customerBin = "bin1", ticketTaxes = taxes
        )
        val req1Copy = req1.copy()
        val reqSame = req1.copy()
        val reqDiffKkm = req1.copy(kkmId = "kkm-2")
        val reqDiffPin = req1.copy(pin = "2222")
        val reqDiffOp = req1.copy(operation = ReceiptOperationType.BUY)
        val reqDiffItems = req1.copy(items = emptyList())
        val reqDiffPayments = req1.copy(payments = emptyList())
        val reqDiffTotal = req1.copy(total = Money(200L, 0))
        val reqDiffTaken = req1.copy(taken = null)
        val reqDiffChange = req1.copy(change = null)
        val reqDiffKey = req1.copy(idempotencyKey = "key-2")
        val reqDiffParent = req1.copy(parentTicket = null)
        val reqDiffTaxRegime = req1.copy(taxRegime = TaxRegime.NO_VAT)
        val reqDiffDefaultVat = req1.copy(defaultVatGroup = null)
        val reqDiffDisc = req1.copy(discount = null)
        val reqDiffMark = req1.copy(markup = null)
        val reqDiffBin = req1.copy(customerBin = null)
        val reqDiffTaxes = req1.copy(ticketTaxes = null)

        assertEquals(req1, req1)
        assertEquals(req1, req1Copy)
        assertEquals(req1, reqSame)
        assertEquals(req1.hashCode(), reqSame.hashCode())

        assertNotEquals(req1, Any())
        assertFalse(req1.equals(null))
        assertNotEquals(req1, reqDiffKkm)
        assertNotEquals(req1, reqDiffPin)
        assertNotEquals(req1, reqDiffOp)
        assertNotEquals(req1, reqDiffItems)
        assertNotEquals(req1, reqDiffPayments)
        assertNotEquals(req1, reqDiffTotal)
        assertNotEquals(req1, reqDiffTaken)
        assertNotEquals(req1, reqDiffChange)
        assertNotEquals(req1, reqDiffKey)
        assertNotEquals(req1, reqDiffParent)
        assertNotEquals(req1, reqDiffTaxRegime)
        assertNotEquals(req1, reqDiffDefaultVat)
        assertNotEquals(req1, reqDiffDisc)
        assertNotEquals(req1, reqDiffMark)
        assertNotEquals(req1, reqDiffBin)
        assertNotEquals(req1, reqDiffTaxes)

        assertTrue(req1.toString().contains("kkmId=kkm-1"))
    }

    @Test
    fun testReceiptResultEqualsAndHashCode() {
        val payload1 = byteArrayOf(1, 2, 3)
        val payload2 = byteArrayOf(1, 2, 3)
        val payload3 = byteArrayOf(4, 5, 6)

        val res1 = ReceiptResult("doc-1", "fs1", "as1", payload1, DeliveryStatus.ONLINE_OK, "error1")
        val res1Copy = res1.copy()
        val resSame = ReceiptResult("doc-1", "fs1", "as1", payload2, DeliveryStatus.ONLINE_OK, "error1")
        val resDiffDoc = res1.copy(documentId = "doc-2")
        val resDiffFs = res1.copy(fiscalSign = "fs2")
        val resDiffAs = res1.copy(autonomousSign = "as2")
        val resDiffPayload = res1.copy(deliveryPayload = payload3)
        val resDiffStatus = res1.copy(deliveryStatus = DeliveryStatus.OFFLINE_QUEUED)
        val resDiffError = res1.copy(deliveryError = "error2")
        val resNullPayload = res1.copy(deliveryPayload = null)

        assertEquals(res1, res1)
        assertEquals(res1, res1Copy)
        assertEquals(res1, resSame)
        assertEquals(res1.hashCode(), resSame.hashCode())

        assertNotEquals(res1, Any())
        assertFalse(res1.equals(null))
        assertNotEquals(res1, resDiffDoc)
        assertNotEquals(res1, resDiffFs)
        assertNotEquals(res1, resDiffAs)
        assertNotEquals(res1, resDiffPayload)
        assertNotEquals(res1, resDiffStatus)
        assertNotEquals(res1, resDiffError)
        assertNotEquals(res1, resNullPayload)
        assertNotEquals(resNullPayload, res1)

        assertTrue(res1.toString().contains("documentId=doc-1"))
        assertTrue(resNullPayload.toString().contains("deliveryPayload=null"))
    }

    @Test
    fun testReceiptStoredPayload() {
        val total = Money(100L, 0)
        val items = listOf(ReceiptItem("item", "001", 1000L, total, total))
        val payments = listOf(ReceiptPayment(PaymentType.CASH, total))
        val parent = ParentTicket(1L, 1000L, "kkm-1", total, false)
        val taxes = listOf(TaxLine(VatGroup.VAT_10, 10, total, Money(10L, 0)))

        val req = ReceiptRequest(
            kkmId = "kkm-1", pin = "1111", operation = ReceiptOperationType.SELL,
            items = items, payments = payments, total = total, taken = total, change = Money(0L, 0),
            idempotencyKey = "key-1", parentTicket = parent, taxRegime = TaxRegime.VAT_PAYER,
            defaultVatGroup = VatGroup.VAT_10, discount = Money(5L, 0), markup = Money(2L, 0),
            customerBin = "bin1", ticketTaxes = taxes
        )

        // fromReceiptRequest helper
        val stored = ReceiptStoredPayload.fromReceiptRequest(req)
        assertEquals("kkm-1", stored.kkmId)
        assertEquals(ReceiptOperationType.SELL, stored.operation)
        assertEquals(items, stored.items)
        assertEquals(payments, stored.payments)
        assertEquals(total, stored.total)
        assertEquals(total, stored.taken)
        assertEquals(Money(0L, 0), stored.change)
        assertEquals(parent, stored.parentTicket)
        assertEquals(TaxRegime.VAT_PAYER, stored.taxRegime)
        assertEquals(VatGroup.VAT_10, stored.defaultVatGroup)
        assertEquals(Money(5L, 0), stored.discount)
        assertEquals(Money(2L, 0), stored.markup)
        assertEquals("bin1", stored.customerBin)
        assertEquals(taxes, stored.ticketTaxes)

        // toReceiptRequest conversion
        val converted = stored.toReceiptRequest()
        assertEquals("kkm-1", converted.kkmId)
        assertEquals("", converted.pin) // Pin is expected empty since it's confidential
        assertEquals(ReceiptOperationType.SELL, converted.operation)
        assertEquals(items, converted.items)
        assertEquals(payments, converted.payments)
        assertEquals(total, converted.total)
        assertEquals(total, converted.taken)
        assertEquals(Money(0L, 0), converted.change)
        assertEquals("", converted.idempotencyKey) // Key is expected empty since it's confidential
        assertEquals(parent, converted.parentTicket)
        assertEquals(TaxRegime.VAT_PAYER, converted.taxRegime)
        assertEquals(VatGroup.VAT_10, converted.defaultVatGroup)
        assertEquals(Money(5L, 0), converted.discount)
        assertEquals(Money(2L, 0), converted.markup)
        assertEquals("bin1", converted.customerBin)
        assertEquals(taxes, converted.ticketTaxes)

        // stored payload equals & hashCode
        val storedCopy = stored.copy()
        val storedSame = stored.copy()
        val storedDiffKkm = stored.copy(kkmId = "kkm-2")
        val storedDiffOp = stored.copy(operation = ReceiptOperationType.BUY)
        val storedDiffItems = stored.copy(items = emptyList())
        val storedDiffPayments = stored.copy(payments = emptyList())
        val storedDiffTotal = stored.copy(total = Money(200L, 0))
        val storedDiffTaken = stored.copy(taken = null)
        val storedDiffChange = stored.copy(change = null)
        val storedDiffParent = stored.copy(parentTicket = null)
        val storedDiffTaxRegime = stored.copy(taxRegime = TaxRegime.NO_VAT)
        val storedDiffDefaultVat = stored.copy(defaultVatGroup = VatGroup.NO_VAT)
        val storedDiffDisc = stored.copy(discount = null)
        val storedDiffMark = stored.copy(markup = null)
        val storedDiffBin = stored.copy(customerBin = null)
        val storedDiffTaxes = stored.copy(ticketTaxes = null)

        assertEquals(stored, stored)
        assertEquals(stored, storedCopy)
        assertEquals(stored, storedSame)
        assertEquals(stored.hashCode(), storedSame.hashCode())

        assertNotEquals(stored, Any())
        assertFalse(stored.equals(null))
        assertNotEquals(stored, storedDiffKkm)
        assertNotEquals(stored, storedDiffOp)
        assertNotEquals(stored, storedDiffItems)
        assertNotEquals(stored, storedDiffPayments)
        assertNotEquals(stored, storedDiffTotal)
        assertNotEquals(stored, storedDiffTaken)
        assertNotEquals(stored, storedDiffChange)
        assertNotEquals(stored, storedDiffParent)
        assertNotEquals(stored, storedDiffTaxRegime)
        assertNotEquals(stored, storedDiffDefaultVat)
        assertNotEquals(stored, storedDiffDisc)
        assertNotEquals(stored, storedDiffMark)
        assertNotEquals(stored, storedDiffBin)
        assertNotEquals(stored, storedDiffTaxes)

        assertTrue(stored.toString().contains("kkmId=kkm-1"))
    }
}
