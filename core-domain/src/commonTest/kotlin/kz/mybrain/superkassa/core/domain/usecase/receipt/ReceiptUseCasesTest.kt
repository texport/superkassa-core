package kz.mybrain.superkassa.core.domain.usecase.receipt

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kz.mybrain.superkassa.core.domain.exception.ConflictException
import kz.mybrain.superkassa.core.domain.exception.NotFoundException
import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.helper.KkmCommonHelper
import kz.mybrain.superkassa.core.domain.helper.ReceiptDeliveryHelper
import kz.mybrain.superkassa.core.domain.model.common.CounterKeyFormats
import kz.mybrain.superkassa.core.domain.model.common.Money
import kz.mybrain.superkassa.core.domain.model.kkm.CashOperationRequest
import kz.mybrain.superkassa.core.domain.model.kkm.CashOperationType
import kz.mybrain.superkassa.core.domain.model.kkm.FiscalDocumentSnapshot
import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kz.mybrain.superkassa.core.domain.model.kkm.KkmState
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandResult
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandStatus
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandType
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptOperationType
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptRequest
import kz.mybrain.superkassa.core.domain.model.shift.ShiftInfo
import kz.mybrain.superkassa.core.domain.model.shift.ShiftStatus
import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.IdGeneratorPort
import kz.mybrain.superkassa.core.domain.port.OfflineQueuePort
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.helper.common.IdempotentOperationExecutor
import kz.mybrain.superkassa.core.domain.usecase.auth.AuthorizeUserUseCase
import kz.mybrain.superkassa.core.domain.usecase.counter.UpdateCountersUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReceiptUseCasesTest {

    private val storage = mockk<StoragePort>(relaxed = true)
    private val queue = mockk<OfflineQueuePort>(relaxed = true)
    private val clock = mockk<ClockPort>()
    private val idGenerator = mockk<IdGeneratorPort>()
    private val authorizeUserUseCase = mockk<AuthorizeUserUseCase>()
    private val executor = IdempotentOperationExecutor(storage, idGenerator, clock, authorizeUserUseCase)
    private val kkmCommonHelper = mockk<KkmCommonHelper>(relaxed = true)
    private val receiptDeliveryHelper = mockk<ReceiptDeliveryHelper>(relaxed = true)
    private val updateCountersUseCase = mockk<UpdateCountersUseCase>(relaxed = true)

    private val deliverReceipt = DeliverReceiptUseCase(receiptDeliveryHelper)
    private val processOfdDocumentResult = ProcessOfdDocumentResultUseCase(
        storage, queue, clock, updateCountersUseCase, deliverReceipt
    )
    private val createCashOperation = CreateCashOperationUseCase(
        storage, queue, executor, kkmCommonHelper, processOfdDocumentResult
    )
    private val processReceipt = ProcessReceiptUseCase(
        storage, queue, executor, kkmCommonHelper, receiptDeliveryHelper,
        processOfdDocumentResult = { a, b, c, d, e, f, g ->
            processOfdDocumentResult.execute(a, b, c, d, e, f, g)
        },
        ofdResultQueuedOffline = { OfdCommandResult(status = OfdCommandStatus.OK) }
    )
    private val retryReceiptDelivery = RetryReceiptDeliveryUseCase(storage, authorizeUserUseCase, receiptDeliveryHelper)

    private val kkm = KkmInfo(id = "kkm-1", createdAt = 0, updatedAt = 0, mode = "ACTIVE", state = KkmState.ACTIVE.name)

    init {
        every { storage.findKkmForUpdate(any()) } answers { storage.findKkm(firstArg()) }
        every { authorizeUserUseCase.requireKkm(any(), any()) } answers { authorizeUserUseCase.requireKkm(firstArg()) }
        every { authorizeUserUseCase.requireRole(any(), any(), any(), any()) } answers { authorizeUserUseCase.requireRole(firstArg(), secondArg(), thirdArg()) }
        every { storage.inTransaction<Any?>(any()) } answers {
            val block = firstArg<() -> Any?>()
            block()
        }
    }

    // --- CreateCashOperationUseCase Tests ---

    @Test
    fun testCreateCashOperationSumNegative() {
        val req = CashOperationRequest(pin = "1234", amount = -10.0, idempotencyKey = "key-1")
        assertFailsWith<ValidationException> {
            createCashOperation.execute("kkm-1", req, CashOperationType.CASH_IN)
        }
    }

    @Test
    fun testCreateCashOperationShiftNotOpen() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-1") } returns null
        every { storage.findOpenShift("kkm-1") } returns null

        val req = CashOperationRequest(pin = "1234", amount = 100.0, idempotencyKey = "key-1")
        assertFailsWith<ConflictException> {
            createCashOperation.execute("kkm-1", req, CashOperationType.CASH_IN)
        }
    }

    @Test
    fun testCreateCashOperationSuccess() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-1") } returns null
        every { idGenerator.nextId() } returns "doc-1"
        every { clock.now() } returns 1000L
        val shift = ShiftInfo(id = "shift-1", kkmId = "kkm-1", shiftNo = 1L, status = ShiftStatus.OPEN, openedAt = 100L)
        every { storage.findOpenShift("kkm-1") } returns shift
        every { queue.canSendDirectly("kkm-1") } returns true
        val ofdResult = OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0)
        every { kkmCommonHelper.sendOfdCommand(kkm, OfdCommandType.MONEY_PLACEMENT, "doc-1") } returns ofdResult

        val req = CashOperationRequest(pin = "1234", amount = 100.0, idempotencyKey = "key-1")
        val res = createCashOperation.execute("kkm-1", req, CashOperationType.CASH_IN)
        assertEquals("doc-1", res.documentId)
        verify {
            storage.saveCashOperation("kkm-1", "CASH_IN", any(), "doc-1", "shift-1", 1000L)
        }
    }

    @Test
    fun testCreateCashOperationOffline() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-1") } returns null
        every { idGenerator.nextId() } returns "doc-1"
        every { clock.now() } returns 1000L
        val shift = ShiftInfo(id = "shift-1", kkmId = "kkm-1", shiftNo = 1L, status = ShiftStatus.OPEN, openedAt = 100L)
        every { storage.findOpenShift("kkm-1") } returns shift
        every { queue.canSendDirectly("kkm-1") } returns false

        val req = CashOperationRequest(pin = "1234", amount = 100.0, idempotencyKey = "key-1")
        val res = createCashOperation.execute("kkm-1", req, CashOperationType.CASH_IN)
        assertEquals("doc-1", res.documentId)
        verify {
            queue.enqueueOffline(match { it.kkmId == "kkm-1" && it.type == OfdCommandType.MONEY_PLACEMENT.value && it.payloadRef == "doc-1" })
        }
    }

    // --- ProcessReceiptUseCase Tests ---

    @Test
    fun testProcessReceiptShiftNotOpen() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-1") } returns null
        every { storage.findOpenShift("kkm-1") } returns null

        val req = ReceiptRequest(
            kkmId = "kkm-1",
            pin = "1234",
            idempotencyKey = "key-1",
            operation = ReceiptOperationType.SELL,
            items = emptyList(),
            payments = emptyList(),
            total = Money(0, 0)
        )
        assertFailsWith<ConflictException> {
            processReceipt.execute(req, kkm)
        }
    }

    @Test
    fun testProcessReceiptOffline() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-1") } returns null
        every { idGenerator.nextId() } returns "doc-2"
        every { clock.now() } returns 1000L
        val shift = ShiftInfo(id = "shift-1", kkmId = "kkm-1", shiftNo = 1L, status = ShiftStatus.OPEN, openedAt = 100L)
        every { storage.findOpenShift("kkm-1") } returns shift
        every { queue.canSendDirectly("kkm-1") } returns false

        val req = ReceiptRequest(
            kkmId = "kkm-1",
            pin = "1234",
            idempotencyKey = "key-1",
            operation = ReceiptOperationType.SELL,
            items = emptyList(),
            payments = emptyList(),
            total = Money(0, 0)
        )
        val res = processReceipt.execute(req, kkm)
        assertEquals("doc-2", res.documentId)
        verify {
            queue.enqueueOffline(match { it.kkmId == "kkm-1" && it.type == OfdCommandType.TICKET.value && it.payloadRef == "doc-2" })
        }
    }

    @Test
    fun testProcessReceiptSuccess() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-1") } returns null
        every { idGenerator.nextId() } returns "doc-2"
        every { clock.now() } returns 1000L
        val shift = ShiftInfo(id = "shift-1", kkmId = "kkm-1", shiftNo = 1L, status = ShiftStatus.OPEN, openedAt = 100L)
        every { storage.findOpenShift("kkm-1") } returns shift
        every { queue.canSendDirectly("kkm-1") } returns true
        val ofdResult = OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0, receiptUrl = "http://ofd/receipt", responseBin = byteArrayOf(1))
        every { kkmCommonHelper.sendOfdCommand(kkm, OfdCommandType.TICKET, "doc-2") } returns ofdResult

        val snapshot = FiscalDocumentSnapshot(
            id = "doc-2",
            cashboxId = "kkm-1",
            shiftId = "shift-1",
            docType = "CHECK",
            docNo = 1L,
            shiftNo = 1L,
            createdAt = 1000L,
            totalAmount = 0L,
            currency = "KZT",
            fiscalSign = "fs",
            autonomousSign = "as",
            isAutonomous = false,
            ofdStatus = "DELIVERED",
            deliveredAt = 1000L
        )
        every { storage.findFiscalDocumentById("doc-2") } returns snapshot

        val req = ReceiptRequest(
            kkmId = "kkm-1",
            pin = "1234",
            idempotencyKey = "key-1",
            operation = ReceiptOperationType.SELL,
            items = emptyList(),
            payments = emptyList(),
            total = Money(0, 0)
        )
        val res = processReceipt.execute(req, kkm)
        assertEquals("doc-2", res.documentId)
        verify {
            storage.saveReceipt(any(), "doc-2", "shift-1", 1000L)
            receiptDeliveryHelper.deliverReceipt("kkm-1", "doc-2", any(), snapshot, "http://ofd/receipt", any())
        }
    }

    @Test
    fun testProcessReceiptSuccessDocNull() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-1") } returns null
        every { idGenerator.nextId() } returns "doc-2"
        every { clock.now() } returns 1000L
        val shift = ShiftInfo(id = "shift-1", kkmId = "kkm-1", shiftNo = 1L, status = ShiftStatus.OPEN, openedAt = 100L)
        every { storage.findOpenShift("kkm-1") } returns shift
        every { queue.canSendDirectly("kkm-1") } returns true
        val ofdResult = OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0, fiscalSign = "fs", autonomousSign = "as")
        every { kkmCommonHelper.sendOfdCommand(kkm, OfdCommandType.TICKET, "doc-2") } returns ofdResult
        every { storage.findFiscalDocumentById("doc-2") } returns null

        val req = ReceiptRequest(
            kkmId = "kkm-1",
            pin = "1234",
            idempotencyKey = "key-1",
            operation = ReceiptOperationType.SELL,
            items = emptyList(),
            payments = emptyList(),
            total = Money(0, 0)
        )
        val res = processReceipt.execute(req, kkm)
        assertEquals("doc-2", res.documentId)
        assertEquals("fs", res.fiscalSign)
        assertEquals("as", res.autonomousSign)
    }

    // --- DeliverReceiptUseCase Tests ---

    @Test
    fun testDeliverReceipt() {
        val snapshot = FiscalDocumentSnapshot(
            id = "doc-1",
            cashboxId = "kkm-1",
            shiftId = "shift-1",
            docType = "CHECK",
            docNo = 1L,
            shiftNo = 1L,
            createdAt = 1000L,
            totalAmount = 100L,
            currency = "KZT",
            fiscalSign = "fs",
            autonomousSign = "as",
            isAutonomous = false,
            ofdStatus = "DELIVERED",
            deliveredAt = 1000L
        )
        val receiptReq = mockk<ReceiptRequest>()
        deliverReceipt.execute("kkm-1", "doc-1", receiptReq, snapshot, "http://ofd/receipt", null)
        verify {
            receiptDeliveryHelper.deliverReceipt("kkm-1", "doc-1", receiptReq, snapshot, "http://ofd/receipt", null)
        }
    }

    // --- RetryReceiptDeliveryUseCase Tests ---

    @Test
    fun testRetryReceiptDeliverySuccess() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        val snapshot = FiscalDocumentSnapshot(
            id = "doc-1",
            cashboxId = "kkm-1",
            shiftId = "shift-1",
            docType = "CHECK",
            docNo = 1L,
            shiftNo = 1L,
            createdAt = 1000L,
            totalAmount = 100L,
            currency = "KZT",
            fiscalSign = "fs",
            autonomousSign = "as",
            isAutonomous = false,
            ofdStatus = "DELIVERED",
            deliveredAt = 1000L
        )
        val receiptReq = mockk<ReceiptRequest>()
        every { storage.findFiscalDocumentWithReceiptPayload("doc-1") } returns (snapshot to receiptReq)
        every { receiptDeliveryHelper.retryDelivery("kkm-1", "doc-1", receiptReq, snapshot) } returns listOf("SMS" to true)

        val res = retryReceiptDelivery.execute("kkm-1", "doc-1", "1234")
        assertEquals(1, res.size)
        assertEquals("SMS", res[0].first)
        assertEquals(true, res[0].second)
    }

    @Test
    fun testRetryReceiptDeliveryNotFound() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findFiscalDocumentWithReceiptPayload("doc-1") } returns null

        assertFailsWith<NotFoundException> {
            retryReceiptDelivery.execute("kkm-1", "doc-1", "1234")
        }
    }

    @Test
    fun testRetryReceiptDeliveryCashboxIdMismatch() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        val snapshot = FiscalDocumentSnapshot(
            id = "doc-1",
            cashboxId = "kkm-2",
            shiftId = "shift-1",
            docType = "CHECK",
            docNo = 1L,
            shiftNo = 1L,
            createdAt = 1000L,
            totalAmount = 100L,
            currency = "KZT",
            fiscalSign = "fs",
            autonomousSign = "as",
            isAutonomous = false,
            ofdStatus = "DELIVERED",
            deliveredAt = 1000L
        )
        val receiptReq = mockk<ReceiptRequest>()
        every { storage.findFiscalDocumentWithReceiptPayload("doc-1") } returns (snapshot to receiptReq)

        assertFailsWith<NotFoundException> {
            retryReceiptDelivery.execute("kkm-1", "doc-1", "1234")
        }
    }

    // --- ProcessOfdDocumentResultUseCase Tests ---

    @Test
    fun testProcessOfdDocumentResultSuccess() {
        val ofdResult = OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0, receiptUrl = "http://ofd/receipt", responseBin = byteArrayOf(2))
        val receiptReq = mockk<ReceiptRequest>()
        val shiftId = "shift-1"
        val doc = FiscalDocumentSnapshot(
            id = "doc-3",
            cashboxId = "kkm-1",
            shiftId = "shift-1",
            docType = "CHECK",
            docNo = 1L,
            shiftNo = 1L,
            createdAt = 1000L,
            totalAmount = 100L,
            currency = "KZT",
            fiscalSign = "fs",
            autonomousSign = "as",
            isAutonomous = false,
            ofdStatus = "DELIVERED",
            deliveredAt = 1000L
        )
        every { storage.findFiscalDocumentById("doc-3") } returns doc

        processOfdDocumentResult.execute(
            kkm = kkm,
            documentId = "doc-3",
            kkmId = "kkm-1",
            ofdResult = ofdResult,
            commandType = OfdCommandType.TICKET,
            now = 1200L,
            receiptContext = receiptReq to shiftId
        )

        verify {
            storage.updateReceiptStatus("doc-3", ofdResult.fiscalSign, ofdResult.autonomousSign, "SENT", 1200L, false)
            updateCountersUseCase.execute("kkm-1", "shift-1", receiptReq, false)
            receiptDeliveryHelper.deliverReceipt("kkm-1", "doc-3", receiptReq, doc, "http://ofd/receipt", any())
        }
    }

    @Test
    fun testProcessOfdDocumentResultSuccessDocNull() {
        val ofdResult = OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0, receiptUrl = "http://ofd/receipt", responseBin = byteArrayOf(2))
        val receiptReq = mockk<ReceiptRequest>()
        val shiftId = "shift-1"
        every { storage.findFiscalDocumentById("doc-3") } returns null

        processOfdDocumentResult.execute(
            kkm = kkm,
            documentId = "doc-3",
            kkmId = "kkm-1",
            ofdResult = ofdResult,
            commandType = OfdCommandType.TICKET,
            now = 1200L,
            receiptContext = receiptReq to shiftId
        )

        verify {
            storage.updateReceiptStatus("doc-3", ofdResult.fiscalSign, ofdResult.autonomousSign, "SENT", 1200L, false)
            updateCountersUseCase.execute("kkm-1", "shift-1", receiptReq, false)
        }
        verify(exactly = 0) {
            receiptDeliveryHelper.deliverReceipt(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun testProcessOfdDocumentResultKkmBlockedAndUnblocked() {
        val ofdResultBlocked = OfdCommandResult(status = OfdCommandStatus.FAILED, resultCode = 15)
        processOfdDocumentResult.execute(
            kkm = kkm,
            documentId = "doc-3",
            kkmId = "kkm-1",
            ofdResult = ofdResultBlocked,
            commandType = OfdCommandType.TICKET,
            now = 1200L,
            receiptContext = null
        )
        verify {
            storage.updateKkm(match { it.state == KkmState.BLOCKED.name })
        }

        val blockedKkm = kkm.copy(state = KkmState.BLOCKED.name)
        val ofdResultUnblocked = OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0)
        processOfdDocumentResult.execute(
            kkm = blockedKkm,
            documentId = "doc-3",
            kkmId = "kkm-1",
            ofdResult = ofdResultUnblocked,
            commandType = OfdCommandType.TICKET,
            now = 1200L,
            receiptContext = null
        )
        verify {
            storage.updateKkm(match { it.state == KkmState.ACTIVE.name })
        }
    }

    @Test
    fun testProcessOfdDocumentResultClearAutonomousIfReady() {
        val ofdResult = OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0)
        val autonomousKkm = kkm.copy(autonomousSince = 500L)
        every { queue.canSendDirectly("kkm-1") } returns true

        processOfdDocumentResult.execute(
            kkm = autonomousKkm,
            documentId = "doc-3",
            kkmId = "kkm-1",
            ofdResult = ofdResult,
            commandType = OfdCommandType.TICKET,
            now = 1200L,
            receiptContext = null
        )

        verify {
            storage.updateKkm(match { it.autonomousSince == null && it.state == KkmState.ACTIVE.name })
        }
    }

    @Test
    fun testProcessOfdDocumentResultTimeoutAndOffline() {
        val ofdResult = OfdCommandResult(status = OfdCommandStatus.TIMEOUT, resultCode = null)
        val receiptReq = mockk<ReceiptRequest>()
        val shiftId = "shift-1"
        every { clock.now() } returns 1300L

        processOfdDocumentResult.execute(
            kkm = kkm,
            documentId = "doc-3",
            kkmId = "kkm-1",
            ofdResult = ofdResult,
            commandType = OfdCommandType.TICKET,
            now = 1200L,
            receiptContext = receiptReq to shiftId
        )

        verify {
            storage.updateReceiptStatus("doc-3", null, "1300", "PENDING", null, true)
            queue.enqueueOffline(match { it.kkmId == "kkm-1" && it.type == OfdCommandType.TICKET.value && it.payloadRef == "doc-3" })
            storage.updateKkm(match { it.autonomousSince == 1200L })
            updateCountersUseCase.execute("kkm-1", "shift-1", receiptReq, true)
        }
    }

    @Test
    fun testUpdateCashSumForOperationZero() {
        processOfdDocumentResult.updateCashSumForOperation("kkm-1", "shift-1", CashOperationType.CASH_IN, 0L)
        verify(exactly = 0) {
            storage.upsertCounter(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun testUpdateCashSumForOperationSuccess() {
        every { storage.loadCounters("kkm-1", "SHIFT", "shift-1") } returnsMany listOf(
            mapOf(CounterKeyFormats.CASH_SUM to 1000L),
            mapOf(CounterKeyFormats.CASH_SUM to 1500L)
        )
        every { storage.loadCounters("kkm-1", "GLOBAL", null) } returnsMany listOf(
            mapOf(CounterKeyFormats.CASH_SUM to 5000L),
            mapOf(CounterKeyFormats.CASH_SUM to 5500L)
        )

        processOfdDocumentResult.updateCashSumForOperation("kkm-1", "shift-1", CashOperationType.CASH_IN, 500L)

        verify {
            storage.upsertCounter("kkm-1", "SHIFT", "shift-1", CounterKeyFormats.CASH_SUM, 1500L)
            storage.upsertCounter("kkm-1", "GLOBAL", null, CounterKeyFormats.CASH_SUM, 5500L)
        }

        processOfdDocumentResult.updateCashSumForOperation("kkm-1", "shift-1", CashOperationType.CASH_OUT, 300L)

        verify {
            storage.upsertCounter("kkm-1", "SHIFT", "shift-1", CounterKeyFormats.CASH_SUM, 1200L)
            storage.upsertCounter("kkm-1", "GLOBAL", null, CounterKeyFormats.CASH_SUM, 5200L)
        }
    }

    @Test
    fun testUpdateMoneyPlacementCountersFromDocumentNullOrEmpty() {
        every { storage.findFiscalDocumentById("doc-3") } returns null
        processOfdDocumentResult.updateMoneyPlacementCountersFromDocument("doc-3", false)
        verify(exactly = 0) {
            storage.upsertCounter(any(), any(), any(), any(), any())
        }

        val emptyShiftDoc = FiscalDocumentSnapshot(
            id = "doc-3",
            cashboxId = "kkm-1",
            shiftId = "",
            docType = "CASH_IN",
            docNo = 1L,
            shiftNo = 1L,
            createdAt = 1000L,
            totalAmount = 100L,
            currency = "KZT",
            fiscalSign = "fs",
            autonomousSign = "as",
            isAutonomous = false,
            ofdStatus = "DELIVERED",
            deliveredAt = 1000L
        )
        every { storage.findFiscalDocumentById("doc-3") } returns emptyShiftDoc
        processOfdDocumentResult.updateMoneyPlacementCountersFromDocument("doc-3", false)
        verify(exactly = 0) {
            storage.upsertCounter(any(), any(), any(), any(), any())
        }

        val zeroAmountDoc = emptyShiftDoc.copy(shiftId = "shift-1", totalAmount = 0L)
        every { storage.findFiscalDocumentById("doc-3") } returns zeroAmountDoc
        processOfdDocumentResult.updateMoneyPlacementCountersFromDocument("doc-3", false)
        verify(exactly = 0) {
            storage.upsertCounter(any(), any(), any(), any(), any())
        }

        val invalidTypeDoc = emptyShiftDoc.copy(shiftId = "shift-1", totalAmount = 100L, docType = "INVALID")
        every { storage.findFiscalDocumentById("doc-3") } returns invalidTypeDoc
        processOfdDocumentResult.updateMoneyPlacementCountersFromDocument("doc-3", false)
        verify(exactly = 0) {
            storage.upsertCounter(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun testUpdateMoneyPlacementCountersFromDocumentSuccess() {
        val doc = FiscalDocumentSnapshot(
            id = "doc-3",
            cashboxId = "kkm-1",
            shiftId = "shift-1",
            docType = "CASH_IN",
            docNo = 1L,
            shiftNo = 1L,
            createdAt = 1000L,
            totalAmount = 500L,
            currency = "KZT",
            fiscalSign = "fs",
            autonomousSign = "as",
            isAutonomous = false,
            ofdStatus = "DELIVERED",
            deliveredAt = 1000L
        )
        every { storage.findFiscalDocumentById("doc-3") } returns doc
        every { storage.loadCounters("kkm-1", any(), any()) } returns emptyMap()

        processOfdDocumentResult.updateMoneyPlacementCountersFromDocument("doc-3", true)

        val opKey = "MONEY_PLACEMENT_DEPOSIT"
        verify {
            storage.upsertCounter("kkm-1", "SHIFT", "shift-1", CounterKeyFormats.MONEY_PLACEMENT_TOTAL_COUNT.format(opKey), 1L)
            storage.upsertCounter("kkm-1", "SHIFT", "shift-1", CounterKeyFormats.MONEY_PLACEMENT_COUNT.format(opKey), 1L)
            storage.upsertCounter("kkm-1", "SHIFT", "shift-1", CounterKeyFormats.MONEY_PLACEMENT_SUM.format(opKey), 500L)
            storage.upsertCounter("kkm-1", "SHIFT", "shift-1", CounterKeyFormats.MONEY_PLACEMENT_OFFLINE_COUNT.format(opKey), 1L)

            storage.upsertCounter("kkm-1", "GLOBAL", null, CounterKeyFormats.MONEY_PLACEMENT_TOTAL_COUNT.format(opKey), 1L)
            storage.upsertCounter("kkm-1", "GLOBAL", null, CounterKeyFormats.MONEY_PLACEMENT_COUNT.format(opKey), 1L)
            storage.upsertCounter("kkm-1", "GLOBAL", null, CounterKeyFormats.MONEY_PLACEMENT_SUM.format(opKey), 500L)
            storage.upsertCounter("kkm-1", "GLOBAL", null, CounterKeyFormats.MONEY_PLACEMENT_OFFLINE_COUNT.format(opKey), 1L)
        }
    }
}
