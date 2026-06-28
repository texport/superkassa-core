package kz.mybrain.superkassa.core.domain.usecase.shift

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kz.mybrain.superkassa.core.domain.exception.ConflictException
import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.model.common.CounterKeyFormats
import kz.mybrain.superkassa.core.domain.model.common.CounterScopes
import kz.mybrain.superkassa.core.domain.model.common.Money
import kz.mybrain.superkassa.core.domain.model.common.TaxRegime
import kz.mybrain.superkassa.core.domain.model.common.VatGroup
import kz.mybrain.superkassa.core.domain.model.delivery.DeliveryStatus
import kz.mybrain.superkassa.core.domain.model.kkm.FiscalDocumentSnapshot
import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kz.mybrain.superkassa.core.domain.model.kkm.KkmState
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandResult
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandStatus
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandType
import kz.mybrain.superkassa.core.domain.model.receipt.PaymentType
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptItem
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptOperationType
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptPayment
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptRequest
import kz.mybrain.superkassa.core.domain.model.shift.ShiftInfo
import kz.mybrain.superkassa.core.domain.model.shift.ShiftStatus
import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.IdGeneratorPort
import kz.mybrain.superkassa.core.domain.port.OfflineQueuePort
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.usecase.auth.AuthorizeUserUseCase
import kz.mybrain.superkassa.core.domain.usecase.ofd.SendFiscalCommandUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ShiftUseCasesTest {

    private val storage = mockk<StoragePort>(relaxed = true)
    private val idGenerator = mockk<IdGeneratorPort>()
    private val clock = mockk<ClockPort>(relaxed = true)
    private val authorizeUser = mockk<AuthorizeUserUseCase>(relaxed = true)
    private val queue = mockk<OfflineQueuePort>(relaxed = true)
    private val sendFiscalCommandUseCase = mockk<SendFiscalCommandUseCase>()

    private val openShift = OpenShiftUseCase(storage, idGenerator, clock, authorizeUser)
    private val closeShift = CloseShiftUseCase(storage, queue, sendFiscalCommandUseCase, idGenerator, clock, authorizeUser)
    private val recalculateShiftCounters = RecalculateShiftCountersUseCase(storage)

    private val kkm = KkmInfo(
        id = "kkm-1",
        createdAt = 0,
        updatedAt = 0,
        mode = "ACTIVE",
        state = KkmState.ACTIVE.name,
        lastShiftNo = 5
    )

    init {
        every { clock.now() } returns 1000L
        every { idGenerator.nextId() } returns "id-gen"
        every { storage.findKkmForUpdate(any()) } answers { storage.findKkm(firstArg()) }
        every { storage.inTransaction<Any?>(any()) } answers {
            @Suppress("UNCHECKED_CAST")
            val block = firstArg<() -> Any?>()
            block()
        }
    }

    // ==========================================
    // OpenShiftUseCase Tests
    // ==========================================

    @Test
    fun testOpenShiftSuccess() {
        every { storage.findKkm("kkm-1") } returns kkm
        every { storage.findOpenShift("kkm-1") } returns null
        every { idGenerator.nextId() } returns "shift-id"
        every { clock.now() } returns 1000L
        every { storage.listShifts("kkm-1", 1, 0) } returns emptyList()
        every { storage.loadCounters("kkm-1", CounterScopes.GLOBAL, null) } returns mapOf(
            CounterKeyFormats.NON_NULLABLE_SUM.format("OPERATION_SELL") to 5000L,
            CounterKeyFormats.NON_NULLABLE_SUM.format("OPERATION_SELL_RETURN") to 0L
        )

        val shift = openShift.execute("kkm-1", "1234")
        assertEquals("shift-id", shift.id)
        assertEquals(6L, shift.shiftNo)
        assertEquals(ShiftStatus.OPEN, shift.status)

        verify {
            storage.createShift(match { it.id == "shift-id" && it.shiftNo == 6L })
            storage.updateKkm(match { it.id == "kkm-1" && it.lastShiftNo == 6 })
            storage.upsertCounter("kkm-1", CounterScopes.SHIFT, "shift-id", CounterKeyFormats.START_SHIFT_NON_NULLABLE_SUM.format("OPERATION_SELL"), 5000L)
            storage.upsertCounter("kkm-1", CounterScopes.SHIFT, "shift-id", CounterKeyFormats.NON_NULLABLE_SUM.format("OPERATION_SELL"), 5000L)
        }
    }

    @Test
    fun testOpenShiftKkmNotFound() {
        every { storage.findKkm("kkm-1") } returns null

        val ex = assertFailsWith<ValidationException> {
            openShift.execute("kkm-1", "1234")
        }
        assertEquals("KKM_NOT_FOUND", ex.code)
    }

    @Test
    fun testOpenShiftKkmInProgrammingMode() {
        val programmingKkm = kkm.copy(state = KkmState.PROGRAMMING.name)
        every { storage.findKkm("kkm-1") } returns programmingKkm

        val ex = assertFailsWith<ValidationException> {
            openShift.execute("kkm-1", "1234")
        }
        assertEquals("KKM_IN_PROGRAMMING", ex.code)
    }

    @Test
    fun testOpenShiftAlreadyOpen() {
        every { storage.findKkm("kkm-1") } returns kkm
        every { storage.findOpenShift("kkm-1") } returns mockk()

        assertFailsWith<ConflictException> {
            openShift.execute("kkm-1", "1234")
        }
    }

    @Test
    fun testOpenShiftCalculateShiftNoFromLastLocalShift() {
        every { storage.findKkm("kkm-1") } returns kkm
        every { storage.findOpenShift("kkm-1") } returns null
        every { storage.listShifts("kkm-1", 1, 0) } returns listOf(
            ShiftInfo(id = "last-shift-id", kkmId = "kkm-1", shiftNo = 10L, status = ShiftStatus.CLOSED, openedAt = 500L)
        )
        every { storage.loadCounters("kkm-1", any(), null) } returns emptyMap()

        val shift = openShift.execute("kkm-1", "1234")
        assertEquals(11L, shift.shiftNo)
    }

    @Test
    fun testOpenShiftCalculateShiftNoWhenLastShiftNoIsOne() {
        val kkmWithLastShiftNoOne = kkm.copy(lastShiftNo = 1)
        every { storage.findKkm("kkm-1") } returns kkmWithLastShiftNoOne
        every { storage.findOpenShift("kkm-1") } returns null
        every { storage.listShifts("kkm-1", 1, 0) } returns emptyList()
        every { storage.loadCounters("kkm-1", any(), null) } returns emptyMap()

        val shift = openShift.execute("kkm-1", "1234")
        assertEquals(1L, shift.shiftNo)
    }

    @Test
    fun testOpenShiftCalculateShiftNoWhenLastShiftNoIsNull() {
        val kkmWithNullShiftNo = kkm.copy(lastShiftNo = null)
        every { storage.findKkm("kkm-1") } returns kkmWithNullShiftNo
        every { storage.findOpenShift("kkm-1") } returns null
        every { storage.listShifts("kkm-1", 1, 0) } returns emptyList()
        every { storage.loadCounters("kkm-1", any(), null) } returns emptyMap()

        val shift = openShift.execute("kkm-1", "1234")
        assertEquals(1L, shift.shiftNo)
    }

    // ==========================================
    // CloseShiftUseCase Tests
    // ==========================================

    @Test
    fun testCloseShiftSuccess() {
        every { storage.findKkm("kkm-1") } returns kkm
        val shift = ShiftInfo(id = "shift-1", kkmId = "kkm-1", shiftNo = 6L, status = ShiftStatus.OPEN, openedAt = 1000L)
        every { storage.findOpenShift("kkm-1") } returns shift
        every { idGenerator.nextId() } returns "doc-1"
        every { clock.now() } returns 2000L
        every { queue.canSendDirectly("kkm-1") } returns true
        every { sendFiscalCommandUseCase.execute("kkm-1", OfdCommandType.CLOSE_SHIFT, "doc-1") } returns OfdCommandResult(status = OfdCommandStatus.OK)

        val res = closeShift.execute("kkm-1", "1234")
        assertEquals("doc-1", res.documentId)
        assertEquals(DeliveryStatus.ONLINE_OK, res.deliveryStatus)
        verify {
            storage.closeShift("shift-1", ShiftStatus.CLOSED, 2000L, "doc-1")
        }
    }

    @Test
    fun testCloseShiftKkmNotFound() {
        every { storage.findKkm("kkm-1") } returns null

        val ex = assertFailsWith<ValidationException> {
            closeShift.execute("kkm-1", "1234")
        }
        assertEquals("KKM_NOT_FOUND", ex.code)
    }

    @Test
    fun testCloseShiftKkmInProgrammingMode() {
        val programmingKkm = kkm.copy(state = KkmState.PROGRAMMING.name)
        every { storage.findKkm("kkm-1") } returns programmingKkm

        val ex = assertFailsWith<ValidationException> {
            closeShift.execute("kkm-1", "1234")
        }
        assertEquals("KKM_IN_PROGRAMMING", ex.code)
    }

    @Test
    fun testCloseShiftNotOpen() {
        every { storage.findKkm("kkm-1") } returns kkm
        every { storage.findOpenShift("kkm-1") } returns null

        assertFailsWith<ConflictException> {
            closeShift.execute("kkm-1", "1234")
        }
    }

    @Test
    fun testCloseShiftOfflineQueued() {
        every { storage.findKkm("kkm-1") } returns kkm
        val shift = ShiftInfo(id = "shift-1", kkmId = "kkm-1", shiftNo = 6L, status = ShiftStatus.OPEN, openedAt = 1000L)
        every { storage.findOpenShift("kkm-1") } returns shift
        every { idGenerator.nextId() } returns "doc-1"
        every { clock.now() } returns 2000L
        every { queue.canSendDirectly("kkm-1") } returns false

        val res = closeShift.execute("kkm-1", "1234")
        assertEquals("doc-1", res.documentId)
        assertEquals(DeliveryStatus.OFFLINE_QUEUED, res.deliveryStatus)
        verify {
            queue.enqueueOffline(match { it.kkmId == "kkm-1" && it.type == OfdCommandType.CLOSE_SHIFT.value && it.payloadRef == "doc-1" })
            storage.closeShift("shift-1", ShiftStatus.CLOSED, 2000L, "doc-1")
        }
    }

    @Test
    fun testCloseShiftOfdTimeout() {
        every { storage.findKkm("kkm-1") } returns kkm
        val shift = ShiftInfo(id = "shift-1", kkmId = "kkm-1", shiftNo = 6L, status = ShiftStatus.OPEN, openedAt = 1000L)
        every { storage.findOpenShift("kkm-1") } returns shift
        every { idGenerator.nextId() } returns "doc-1"
        every { clock.now() } returns 2000L
        every { queue.canSendDirectly("kkm-1") } returns true
        every { sendFiscalCommandUseCase.execute("kkm-1", OfdCommandType.CLOSE_SHIFT, "doc-1") } returns OfdCommandResult(
            status = OfdCommandStatus.TIMEOUT,
            errorMessage = "Connection timed out"
        )

        val res = closeShift.execute("kkm-1", "1234")
        assertEquals("doc-1", res.documentId)
        assertEquals(DeliveryStatus.OFFLINE_QUEUED, res.deliveryStatus)
        assertEquals("Connection timed out", res.deliveryError)
    }

    @Test
    fun testCloseShiftOfdFailed() {
        every { storage.findKkm("kkm-1") } returns kkm
        val shift = ShiftInfo(id = "shift-1", kkmId = "kkm-1", shiftNo = 6L, status = ShiftStatus.OPEN, openedAt = 1000L)
        every { storage.findOpenShift("kkm-1") } returns shift
        every { idGenerator.nextId() } returns "doc-1"
        every { clock.now() } returns 2000L
        every { queue.canSendDirectly("kkm-1") } returns true
        every { sendFiscalCommandUseCase.execute("kkm-1", OfdCommandType.CLOSE_SHIFT, "doc-1") } returns OfdCommandResult(
            status = OfdCommandStatus.FAILED,
            errorMessage = "Invalid request format"
        )

        val res = closeShift.execute("kkm-1", "1234")
        assertEquals("doc-1", res.documentId)
        assertEquals(DeliveryStatus.ONLINE_ERROR, res.deliveryStatus)
        assertEquals("Invalid request format", res.deliveryError)
    }

    // ==========================================
    // RecalculateShiftCountersUseCase Tests
    // ==========================================

    @Test
    fun testRecalculateShiftCountersSuccess() {
        val shift = ShiftInfo(id = "shift-1", kkmId = "kkm-1", shiftNo = 6L, status = ShiftStatus.CLOSED, openedAt = 1000L)
        every { storage.loadCounters("kkm-1", CounterScopes.SHIFT, "shift-1") } returns mapOf(
            CounterKeyFormats.START_SHIFT_NON_NULLABLE_SUM.format("OPERATION_SELL") to 10000L
        )

        // Mock document page 1
        val checkDoc1 = FiscalDocumentSnapshot(
            id = "doc-check-1", cashboxId = "kkm-1", shiftId = "shift-1",
            docType = "CHECK", docNo = 1L, shiftNo = 6L, createdAt = 1100L,
            totalAmount = 5000L, currency = "KZT", fiscalSign = "fs1", autonomousSign = "as1",
            isAutonomous = false, ofdStatus = "DELIVERED", deliveredAt = 1200L
        )
        val checkDoc2 = FiscalDocumentSnapshot(
            id = "doc-check-2", cashboxId = "kkm-1", shiftId = "shift-1",
            docType = "CHECK", docNo = 2L, shiftNo = 6L, createdAt = 1300L,
            totalAmount = 3000L, currency = "KZT", fiscalSign = "fs2", autonomousSign = "as2",
            isAutonomous = true, ofdStatus = "TIMEOUT", deliveredAt = null
        )
        val cashInDoc = FiscalDocumentSnapshot(
            id = "doc-cashin", cashboxId = "kkm-1", shiftId = "shift-1",
            docType = "CASH_IN", docNo = 3L, shiftNo = 6L, createdAt = 1400L,
            totalAmount = 2000L, currency = "KZT", fiscalSign = "fs3", autonomousSign = "as3",
            isAutonomous = false, ofdStatus = "DELIVERED", deliveredAt = 1500L
        )
        val cashOutDoc = FiscalDocumentSnapshot(
            id = "doc-cashout", cashboxId = "kkm-1", shiftId = "shift-1",
            docType = "CASH_OUT", docNo = 4L, shiftNo = 6L, createdAt = 1600L,
            totalAmount = 1000L, currency = "KZT", fiscalSign = "fs4", autonomousSign = "as4",
            isAutonomous = false, ofdStatus = "DELIVERED", deliveredAt = 1700L
        )
        val unknownDoc = FiscalDocumentSnapshot(
            id = "doc-unknown", cashboxId = "kkm-1", shiftId = "shift-1",
            docType = "UNKNOWN", docNo = 5L, shiftNo = 6L, createdAt = 1800L,
            totalAmount = 1000L, currency = "KZT", fiscalSign = null, autonomousSign = null,
            isAutonomous = false, ofdStatus = null, deliveredAt = null
        )

        every { storage.listFiscalDocumentsByShift("kkm-1", "shift-1", limit = 500, offset = 0) } returns listOf(
            checkDoc1, checkDoc2, cashInDoc, cashOutDoc, unknownDoc
        )
        every { storage.listFiscalDocumentsByShift("kkm-1", "shift-1", limit = 500, offset = 500) } returns emptyList()

        // Mock ReceiptPayload for checkDoc1 (SELL, discount=null, markup=null, change=null)
        val item1 = ReceiptItem(
            name = "Item 1", sectionCode = "", quantity = 1000L,
            price = Money(5000L, 0), sum = Money(5000L, 0),
            vatGroup = VatGroup.VAT_16
        )
        val request1 = ReceiptRequest(
            kkmId = "kkm-1", pin = "1234", operation = ReceiptOperationType.SELL,
            items = listOf(item1),
            payments = listOf(ReceiptPayment(PaymentType.CASH, Money(5000L, 0))),
            total = Money(5000L, 0), idempotencyKey = "key-1",
            taxRegime = TaxRegime.VAT_PAYER, defaultVatGroup = VatGroup.VAT_16
        )
        every { storage.findFiscalDocumentWithReceiptPayload("doc-check-1") } returns (checkDoc1 to request1)

        // Mock ReceiptPayload for checkDoc2 (SELL_RETURN, discount=500, markup=200, change=100)
        val item2 = ReceiptItem(
            name = "Item 2", sectionCode = "002", quantity = 1000L,
            price = Money(3000L, 0), sum = Money(3000L, 0),
            vatGroup = VatGroup.VAT_10, discount = Money(500L, 0), markup = Money(200L, 0)
        )
        val request2 = ReceiptRequest(
            kkmId = "kkm-1", pin = "1234", operation = ReceiptOperationType.SELL_RETURN,
            items = listOf(item2),
            payments = listOf(
                ReceiptPayment(PaymentType.CARD, Money(1000L, 0)),
                ReceiptPayment(PaymentType.ELECTRONIC, Money(1000L, 0)),
                ReceiptPayment(PaymentType.MOBILE, Money(1000L, 0))
            ),
            total = Money(3000L, 0), change = Money(100L, 0), idempotencyKey = "key-2",
            taxRegime = TaxRegime.MIXED, defaultVatGroup = VatGroup.NO_VAT,
            discount = Money(500L, 0), markup = Money(200L, 0)
        )
        every { storage.findFiscalDocumentWithReceiptPayload("doc-check-2") } returns (checkDoc2 to request2)

        // Run the usecase
        val result = recalculateShiftCounters.execute("kkm-1", shift)

        // Verify updates
        verify {
            storage.upsertCounter("kkm-1", CounterScopes.SHIFT, "shift-1", any(), any())
        }

        // Assert some key values in the result map
        // Cash in register: initial = 0L, cash payment in check 1 = +5000L, cashInDoc = +2000L, cashOutDoc = -1000L -> total 6000L
        assertEquals(6000L, result[CounterKeyFormats.CASH_SUM])

        // Check SELL count and sum
        assertEquals(1L, result[CounterKeyFormats.OPERATION_COUNT.format("OPERATION_SELL")])
        assertEquals(5000L, result[CounterKeyFormats.OPERATION_SUM.format("OPERATION_SELL")])

        // Check SELL_RETURN count and sum
        assertEquals(1L, result[CounterKeyFormats.OPERATION_COUNT.format("OPERATION_SELL_RETURN")])
        assertEquals(3000L, result[CounterKeyFormats.OPERATION_SUM.format("OPERATION_SELL_RETURN")])

        // Check start and end shift values
        assertEquals(10000L, result[CounterKeyFormats.START_SHIFT_NON_NULLABLE_SUM.format("OPERATION_SELL")])
        // Non-nullable sum = start + opSum = 10000L + 5000L = 15000L
        assertEquals(15000L, result[CounterKeyFormats.NON_NULLABLE_SUM.format("OPERATION_SELL")])

        // Revenue sum: SELL = +5000L, SELL_RETURN = -3000L -> total = 2000L
        assertEquals(2000L, result[CounterKeyFormats.REVENUE_SUM])
        assertEquals(0L, result[CounterKeyFormats.REVENUE_IS_NEGATIVE])
    }

    @Test
    fun testRecalculateShiftCountersZeroRevenueAndEmptyPayload() {
        val shift = ShiftInfo(id = "shift-1", kkmId = "kkm-1", shiftNo = 6L, status = ShiftStatus.CLOSED, openedAt = 1000L)
        
        // Mock a check document that fails to load its payload
        val checkDoc = FiscalDocumentSnapshot(
            id = "doc-check-empty", cashboxId = "kkm-1", shiftId = "shift-1",
            docType = "CHECK", docNo = 1L, shiftNo = 6L, createdAt = 1100L,
            totalAmount = 0L, currency = "KZT", fiscalSign = null, autonomousSign = null,
            isAutonomous = false, ofdStatus = null, deliveredAt = null
        )
        // Mock a cash operation with zero amount
        val zeroCashIn = FiscalDocumentSnapshot(
            id = "doc-cashin-zero", cashboxId = "kkm-1", shiftId = "shift-1",
            docType = "CASH_IN", docNo = 2L, shiftNo = 6L, createdAt = 1200L,
            totalAmount = 0L, currency = "KZT", fiscalSign = null, autonomousSign = null,
            isAutonomous = false, ofdStatus = null, deliveredAt = null
        )

        every { storage.listFiscalDocumentsByShift("kkm-1", "shift-1", limit = 500, offset = 0) } returns listOf(
            checkDoc, zeroCashIn
        )
        every { storage.listFiscalDocumentsByShift("kkm-1", "shift-1", limit = 500, offset = 500) } returns emptyList()
        every { storage.findFiscalDocumentWithReceiptPayload("doc-check-empty") } returns null // Return null to cover early return

        val result = recalculateShiftCounters.execute("kkm-1", shift)
        assertTrue(CounterKeyFormats.REVENUE_IS_NEGATIVE !in result)
    }

    @Test
    fun testRecalculateShiftCountersNegativeRevenue() {
        val shift = ShiftInfo(id = "shift-1", kkmId = "kkm-1", shiftNo = 6L, status = ShiftStatus.CLOSED, openedAt = 1000L)

        // Mock a check document with SELL_RETURN (resulting in negative revenue)
        val returnDoc = FiscalDocumentSnapshot(
            id = "doc-return", cashboxId = "kkm-1", shiftId = "shift-1",
            docType = "CHECK", docNo = 1L, shiftNo = 6L, createdAt = 1100L,
            totalAmount = 5000L, currency = "KZT", fiscalSign = "fs", autonomousSign = "as",
            isAutonomous = false, ofdStatus = "DELIVERED", deliveredAt = 1200L
        )

        every { storage.listFiscalDocumentsByShift("kkm-1", "shift-1", limit = 500, offset = 0) } returns listOf(returnDoc)
        every { storage.listFiscalDocumentsByShift("kkm-1", "shift-1", limit = 500, offset = 500) } returns emptyList()

        val item = ReceiptItem(
            name = "Returned Item", sectionCode = "001", quantity = 1000L,
            price = Money(5000L, 0), sum = Money(5000L, 0)
        )
        val request = ReceiptRequest(
            kkmId = "kkm-1", pin = "1234", operation = ReceiptOperationType.SELL_RETURN,
            items = listOf(item),
            payments = listOf(ReceiptPayment(PaymentType.CASH, Money(5000L, 0))),
            total = Money(5000L, 0), idempotencyKey = "key-1"
        )
        every { storage.findFiscalDocumentWithReceiptPayload("doc-return") } returns (returnDoc to request)

        val result = recalculateShiftCounters.execute("kkm-1", shift)
        assertEquals(-5000L, result[CounterKeyFormats.REVENUE_SUM])
        assertEquals(1L, result[CounterKeyFormats.REVENUE_IS_NEGATIVE])
    }
}
