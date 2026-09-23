package io.github.texport.superkassa.core.domain.impl.usecase.receipt

import io.github.texport.superkassa.core.domain.api.model.receipt.ParentTicket
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.model.auth.KkmUser
import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.github.texport.superkassa.core.domain.api.exception.ConflictException
import io.github.texport.superkassa.core.domain.api.exception.NotFoundException
import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.impl.helper.KkmCommonHelper
import io.github.texport.superkassa.core.domain.impl.helper.ReceiptDeliveryHelper
import io.github.texport.superkassa.core.domain.api.model.common.CounterKeyFormats
import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup
import io.github.texport.superkassa.core.domain.api.model.common.TaxRegime
import io.github.texport.superkassa.core.domain.api.model.kkm.CashOperationRequest
import io.github.texport.superkassa.core.domain.api.model.kkm.CashOperationType
import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandType
import io.github.texport.superkassa.core.domain.api.model.receipt.PaymentType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftStatus
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.internal.IdGeneratorPort
import io.github.texport.superkassa.core.domain.api.port.internal.OfflineQueuePort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.api.port.integration.inTransaction
import io.github.texport.superkassa.core.domain.impl.helper.common.IdempotentOperationExecutor
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.kkm.RequireOperationalUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.counter.UpdateCountersUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.shift.RecalculateShiftCountersUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertFailsWith

class ReceiptUseCasesTest {

    private val storage = mockk<StoragePort>(relaxed = true)
    private val queue = mockk<OfflineQueuePort>(relaxed = true)
    private val clock = mockk<ClockPort>()
    private val idGenerator = mockk<IdGeneratorPort>()
    private val authorizeUserUseCase = mockk<AuthorizeUserUseCase>()
    private val requireOperationalUseCase = mockk<RequireOperationalUseCase>(relaxed = true)
    private val executor = IdempotentOperationExecutor(storage, idGenerator, clock, authorizeUserUseCase, requireOperationalUseCase)
    private val kkmCommonHelper = mockk<KkmCommonHelper>(relaxed = true)
    private val receiptDeliveryHelper = mockk<ReceiptDeliveryHelper>(relaxed = true)
    private val updateCountersUseCase = mockk<UpdateCountersUseCase>(relaxed = true)

    private val deliverReceipt = DeliverReceiptUseCase(receiptDeliveryHelper)
    private val processOfdDocumentResult = ProcessOfdDocumentResultUseCase(
        storage, queue, clock, updateCountersUseCase, deliverReceipt
    )
    private val createCashOperation = CreateCashOperationUseCase(
        storage, queue, executor, kkmCommonHelper, processOfdDocumentResult,
            recalculateShiftCounters = RecalculateShiftCountersUseCase(storage))
    /** Пересчёт сменных счётчиков: покупке он говорит, сколько наличных в ящике. */
    private val shiftCounters = mockk<RecalculateShiftCountersUseCase>()

    private val processReceipt = ProcessReceiptUseCase(
        storage = storage,
        queue = queue,
        fiscalOperationExecutor = executor,
        kkmCommonHelper = kkmCommonHelper,
        authorizeUser = authorizeUserUseCase,
        requireOperational = requireOperationalUseCase,
        recalculateShiftCounters = shiftCounters,
        processOfdDocumentResult = { a, b, c, d, e, f, g ->
            processOfdDocumentResult.execute(a, b, c, d, e, f, g)
        },
        // Как в сборке: поставленное в очередь — не доставленное.
        ofdResultQueuedOffline = { OfdCommandResult(status = OfdCommandStatus.TIMEOUT) }
    )
    private val retryReceiptDelivery = RetryReceiptDeliveryUseCase(storage, authorizeUserUseCase, receiptDeliveryHelper)

    private val kkm = KkmInfo(id = "kkm-1", createdAt = 0, updatedAt = 0, mode = "ACTIVE", state = KkmState.ACTIVE.name)

    init {
        // Кассы в хранилище нет: сценарии работают с той, что им передана.
        every { storage.findKkm(any()) } returns null
        every { storage.findKkmForUpdate(any()) } answers { storage.findKkm(firstArg()) }
        every { authorizeUserUseCase.requireKkm(any(), any()) } answers { authorizeUserUseCase.requireKkm(firstArg()) }
        every { authorizeUserUseCase.requireRole(any(), any(), any(), any()) } answers { authorizeUserUseCase.requireRole(firstArg(), secondArg(), thirdArg()) }
        // Имя оформившего сохраняется вместе с чеком: авторизация отвечает
        // тем же кассиром на всех проверках.
        every { authorizeUserUseCase.identify(any(), any(), any()) } returns KkmUser(
            id = "user-1",
            name = "Айгүл",
            role = UserRole.CASHIER,
            createdAt = 0L
        )
    }

    @Test
    fun testPaymentTypeAbsentInProtocolIsRefusedBeforeFiscalisation() {
        // 2.0.4 не содержит оплаты в кредит. Принять такой чек значило бы
        // оформить документ, который невозможно доставить ни сейчас, ни
        // повтором из очереди, поэтому отказ приходит до фискализации.
        val supported = PaymentType.supportedBy("204")

        assertEquals(false, supported.contains(PaymentType.CREDIT))
        assertEquals(false, supported.contains(PaymentType.TARE))
        assertEquals(true, PaymentType.supportedBy("203").contains(PaymentType.CREDIT))
        assertEquals(true, PaymentType.supportedBy("203").contains(PaymentType.TARE))
    }

    // --- Обработка кодов результата ОФД ---

    @Test
    fun testDeregisteredKkmIsBlocked() {
        // Код 18 добавлен протоколом 2.0.4: касса снята с учёта в налоговом
        // органе. Без него снятая с учёта касса продолжала выпускать чеки.
        val updated = slot<KkmInfo>()
        every { storage.updateKkm(capture(updated)) } returns true

        processOfdDocumentResult.execute(
            kkm, "doc-1", kkm.id,
            OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 18),
            OfdCommandType.TICKET, 100L, null
        )

        assertEquals(KkmState.BLOCKED.name, updated.captured.state)
    }

    @Test
    fun testDisconnectedKkmIsBlocked() {
        val updated = slot<KkmInfo>()
        every { storage.updateKkm(capture(updated)) } returns true

        processOfdDocumentResult.execute(
            kkm, "doc-2", kkm.id,
            OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 19),
            OfdCommandType.TICKET, 100L, null
        )

        assertEquals(KkmState.BLOCKED.name, updated.captured.state)
    }

    @Test
    fun testRefusalCodeIsKeptForAnyNonZeroResult() {
        // Раньше код отказа сохранялся только для 13, 14 и 17, а причина
        // любого другого отказа терялась.
        var recorded: Int? = null
        every {
            storage.updateReceiptStatus(any(), any(), any(), any(), any(), any(), any())
        } answers {
            recorded = arg<Int?>(4)
            true
        }

        processOfdDocumentResult.execute(
            kkm, "doc-3", kkm.id,
            OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 9),
            OfdCommandType.TICKET, 100L, null
        )

        assertEquals(9, recorded)
    }

    @Test
    fun `причина отказа сохраняется словами ОФД`() {
        // По одному коду обслуживание причину не находит: «Код отказа 15»
        // стоит и за снятой с учёта кассой, и за нехваткой обязательного
        // реквизита в позиции. Слова ОФД оставались только в журнале узла.
        var reason: String? = null
        every {
            storage.updateReceiptStatus(any(), any(), any(), any(), any(), any(), any(), any())
        } answers {
            reason = arg<String?>(7)
            true
        }

        processOfdDocumentResult.execute(
            kkm, "doc-reason", kkm.id,
            OfdCommandResult(
                status = OfdCommandStatus.OK,
                resultCode = 15,
                resultText = "Commodity with ntin has no commodity type"
            ),
            OfdCommandType.TICKET, 100L, null
        )

        assertEquals("Commodity with ntin has no commodity type", reason)
    }

    @Test
    fun `принятый чек причины отказа не несёт`() {
        var reason: String? = "осталось с прошлого раза"
        every {
            storage.updateReceiptStatus(any(), any(), any(), any(), any(), any(), any(), any())
        } answers {
            reason = arg<String?>(7)
            true
        }

        processOfdDocumentResult.execute(
            kkm, "doc-ok", kkm.id,
            OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0, resultText = "OK"),
            OfdCommandType.TICKET, 100L, null
        )

        assertEquals(null, reason)
    }

    @Test
    fun `отвергнутый ОФД чек не попадает в счётчики`() {
        // Отказ ОФД — не продажа: ни выручка, ни налоги, ни наличные
        // от него не меняются. Иначе Z-отчёт разошёлся бы с ОФД на сумму
        // чека, которого у ОФД нет.
        processOfdDocumentResult.execute(
            kkm, "doc-refused-counters", kkm.id,
            OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 16),
            OfdCommandType.TICKET, 100L, Pair(mockk<ReceiptRequest>(), "shift-1")
        )

        verify(exactly = 0) { updateCountersUseCase.execute(any(), any(), any(), any()) }
    }

    @Test
    fun testRefusedDocumentIsMarkedFailedInsteadOfPendingForever() {
        // Отказ ОФД с кодом вне списка 13/14/17 оставлял документ в PENDING:
        // в очередь он не попадал, повтор его не подхватывал, и чек висел
        // «ожидает отправки» бессрочно. Ответ получен — значит документ
        // либо принят, либо отвергнут.
        var status: String? = null
        every {
            storage.updateReceiptStatus(any(), any(), any(), any(), any(), any(), any())
        } answers {
            status = arg<String>(3)
            true
        }

        processOfdDocumentResult.execute(
            kkm, "doc-refused", kkm.id,
            OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 16),
            OfdCommandType.TICKET, 100L, null
        )

        assertEquals("FAILED", status)
        // Отвергнутый документ в очередь не ставится: повторять нечего.
        verify(exactly = 0) { queue.enqueueOffline(any()) }
    }

    @Test
    fun testUnsendableCommandIsNotDressedUpAsAutonomousFiscalisation() {
        // Узел не смог построить запрос к ОФД: связи не теряли, ответа нет.
        // Раньше это шло по автономной ветке — документ получал автономный
        // признак, вставал в очередь и вечно повторялся с теми же данными,
        // а касса уходила в автономный режим на ровном месте.
        var status: String? = null
        var autonomousFlag: Boolean? = null
        every {
            storage.updateReceiptStatus(any(), any(), any(), any(), any(), any(), any())
        } answers {
            status = arg<String>(3)
            autonomousFlag = arg<Boolean?>(6)
            true
        }

        processOfdDocumentResult.execute(
            kkm, "doc-unsendable", kkm.id,
            OfdCommandResult(status = OfdCommandStatus.FAILED, resultCode = null),
            OfdCommandType.TICKET, 100L, null
        )

        assertEquals("FAILED", status)
        assertEquals(false, autonomousFlag)
        verify(exactly = 0) { queue.enqueueOffline(any()) }
    }

    @Test
    fun testLostConnectionStillFiscalisesAutonomously() {
        // Обрыв связи остаётся обрывом: документ фискализируется автономно
        // и досылается из очереди.
        every { clock.now() } returns 100L
        processOfdDocumentResult.execute(
            kkm, "doc-offline", kkm.id,
            OfdCommandResult(status = OfdCommandStatus.TIMEOUT, resultCode = null),
            OfdCommandType.TICKET, 100L, null
        )

        verify(exactly = 1) { queue.enqueueOffline(any()) }
    }

    @Test
    fun testRetryDeliveryOfNeverFiscalizedDocumentIsRefused() {
        // Повтор доставки отвечал успехом с пустым списком каналов даже для
        // чека, которого фискально не существует.
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        val refused = FiscalDocumentSnapshot(
            id = "doc-1",
            cashboxId = "kkm-1",
            shiftId = "shift-1",
            docType = "SALE",
            docNo = null,
            shiftNo = 1L,
            createdAt = 1000L,
            totalAmount = 100L,
            currency = "KZT",
            fiscalSign = null,
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = "FAILED",
            ofdErrorCode = 16,
            deliveredAt = null
        )
        every { storage.findFiscalDocumentWithReceiptPayload("doc-1") } returns (refused to mockk<ReceiptRequest>())

        val error = assertFailsWith<ConflictException> {
            retryReceiptDelivery.execute("kkm-1", "doc-1", "1234")
        }
        assertEquals("DOCUMENT_NOT_FISCALIZED", error.code)
        verify(exactly = 0) { receiptDeliveryHelper.retryDelivery(any(), any(), any(), any()) }
    }

    // --- CreateCashOperationUseCase Tests ---

    @Test
    fun testCreateCashOperationSumNegative() {
        val req = CashOperationRequest(pin = "1234", amount = Decimal.parse("-10.0"), idempotencyKey = "key-1")
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

        val req = CashOperationRequest(pin = "1234", amount = Decimal.parse("100.0"), idempotencyKey = "key-1")
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

        val req = CashOperationRequest(pin = "1234", amount = Decimal.parse("100.0"), idempotencyKey = "key-1")
        val res = createCashOperation.execute("kkm-1", req, CashOperationType.CASH_IN)
        assertEquals("doc-1", res.documentId)
        verify {
            storage.saveCashOperation("kkm-1", "CASH_IN", any(), "doc-1", "shift-1", 1000L)
        }
    }

    /**
     * Внесение уходит в счётчик наличных целиком, вместе с тиынами.
     * Пока в счётчик шли одни тенге, внесение 2500,75 добавляло в ящик
     * 25,00: сумма толковалась как тиыны.
     */
    @Test
    fun `внесение попадает в счётчик наличных в тиынах`() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-tiyn") } returns null
        every { idGenerator.nextId() } returns "doc-tiyn"
        every { clock.now() } returns 1000L
        val shift = ShiftInfo(id = "shift-1", kkmId = "kkm-1", shiftNo = 1L, status = ShiftStatus.OPEN, openedAt = 100L)
        every { storage.findOpenShift("kkm-1") } returns shift
        every { queue.canSendDirectly("kkm-1") } returns true
        every {
            kkmCommonHelper.sendOfdCommand(kkm, OfdCommandType.MONEY_PLACEMENT, "doc-tiyn")
        } returns OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0)
        every { storage.loadCounters("kkm-1", "SHIFT", "shift-1") } returns emptyMap()
        every { storage.loadCounters("kkm-1", "GLOBAL", null) } returns emptyMap()

        val req = CashOperationRequest(pin = "1234", amount = Decimal.parse("2500.75"), idempotencyKey = "key-tiyn")
        createCashOperation.execute("kkm-1", req, CashOperationType.CASH_IN)

        verify {
            storage.upsertCounter("kkm-1", "SHIFT", "shift-1", CounterKeyFormats.CASH_SUM, 250_075L)
            storage.upsertCounter("kkm-1", "GLOBAL", null, CounterKeyFormats.CASH_SUM, 250_075L)
        }
    }

    /**
     * Отвергнутое ОФД изъятие денег из ящика не забирает.
     *
     * Ящик уменьшался в момент сохранения документа, до ответа ОФД,
     * и отказ ничего не возвращал: в кассе висела недостача по операции,
     * которой не было, — до ближайшего X-отчёта, который пересчитывал
     * ящик по документам и «находил» деньги обратно.
     */
    @Test
    fun `отвергнутое изъятие не уменьшает денежный ящик`() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-refused") } returns null
        every { idGenerator.nextId() } returns "doc-refused"
        every { clock.now() } returns 1000L
        val shift = ShiftInfo(id = "shift-1", kkmId = "kkm-1", shiftNo = 1L, status = ShiftStatus.OPEN, openedAt = 100L)
        every { storage.findOpenShift("kkm-1") } returns shift
        every { queue.canSendDirectly("kkm-1") } returns true
        every {
            kkmCommonHelper.sendOfdCommand(kkm, OfdCommandType.MONEY_PLACEMENT, "doc-refused")
        } returns OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 16)
        every { storage.loadCounters("kkm-1", "SHIFT", "shift-1") } returns mapOf(
            "start_shift_cash.sum" to 500_000L,
            CounterKeyFormats.CASH_SUM to 500_000L
        )
        every { storage.loadCounters("kkm-1", "GLOBAL", null) } returns
            mapOf(CounterKeyFormats.CASH_SUM to 500_000L)
        every { storage.listFiscalDocumentsByShift("kkm-1", "shift-1", any(), any()) } returns emptyList()
        every { storage.findFiscalDocumentById("doc-refused") } returns refusedCashOut()

        val req = CashOperationRequest(pin = "1234", amount = Decimal.parse("1000.0"), idempotencyKey = "key-refused")
        createCashOperation.execute("kkm-1", req, CashOperationType.CASH_OUT)

        // Пересчёт при проверке остатка перезаписывает счётчик тем же
        // значением — это не расход. Расходом было бы 500 000 − 100 000.
        verify(exactly = 0) {
            storage.upsertCounter("kkm-1", any(), any(), CounterKeyFormats.CASH_SUM, 400_000L)
        }
    }

    /** Изъятие, которое ОФД не провёл: ни фискального признака, ни автономного. */
    private fun refusedCashOut() = FiscalDocumentSnapshot(
        id = "doc-refused",
        cashboxId = "kkm-1",
        shiftId = "shift-1",
        docType = CashOperationType.CASH_OUT.name,
        docNo = null,
        shiftNo = 1L,
        createdAt = 1000L,
        totalAmount = 100_000L,
        currency = "KZT",
        fiscalSign = null,
        autonomousSign = null,
        isAutonomous = false,
        ofdStatus = "FAILED",
        ofdErrorCode = 16,
        deliveredAt = null
    )

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

        val req = CashOperationRequest(pin = "1234", amount = Decimal.parse("100.0"), idempotencyKey = "key-1")
        val res = createCashOperation.execute("kkm-1", req, CashOperationType.CASH_IN)
        assertEquals("doc-1", res.documentId)
        verify {
            queue.enqueueOffline(match { it.kkmId == "kkm-1" && it.type == OfdCommandType.MONEY_PLACEMENT.value && it.payloadRef == "doc-1" })
        }
    }

    // --- ProcessReceiptUseCase Tests ---

    /**
     * Неплательщик НДС не пропускает ставку в чек.
     *
     * В хранимом пакете узла рядом лежали "vatGroup":"VAT_12",
     * "taxRegime":"NO_VAT" и "ticketTaxes":[] — кассир видел и печатал
     * «НДС 12%», а в ОФД уходила позиция без налога.
     */
    @Test
    fun testProcessReceiptRefusesVatRateWhenRegimeIsNoVat() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-1") } returns null
        every { clock.now() } returns 1000L
        val shift = ShiftInfo(id = "shift-1", kkmId = "kkm-1", shiftNo = 1L, status = ShiftStatus.OPEN, openedAt = 100L)
        every { storage.findOpenShift("kkm-1") } returns shift

        val req = CreateReceiptCommand(
            kkmId = "kkm-1",
            pin = "1234",
            idempotencyKey = "key-1",
            operation = ReceiptOperationType.SELL,
            items = listOf(
                CreateReceiptCommand.ItemInput(
                    name = "Item 1",
                    price = Decimal.parse("100.0"),
                    quantity = Decimal.parse("1.0"),
                    vatGroup = "VAT_12"
                )
            ),
            payments = listOf(CreateReceiptCommand.PaymentInput(type = "CASH", sum = Decimal.parse("100.0"))),
            discountPercent = null,
            discountSum = null,
            markupPercent = null,
            markupSum = null,
            taken = Decimal.parse("100.0")
        )
        val failure = assertFailsWith<ValidationException> { processReceipt.execute(req) }
        assertEquals("RECEIPT_VAT_NOT_ALLOWED", failure.code)
    }

    /** Ставка всего чека проверяется наравне со ставкой позиции. */
    @Test
    fun testProcessReceiptRefusesDefaultVatGroupWhenRegimeIsNoVat() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-1") } returns null
        every { clock.now() } returns 1000L
        val shift = ShiftInfo(id = "shift-1", kkmId = "kkm-1", shiftNo = 1L, status = ShiftStatus.OPEN, openedAt = 100L)
        every { storage.findOpenShift("kkm-1") } returns shift

        val req = CreateReceiptCommand(
            kkmId = "kkm-1",
            pin = "1234",
            idempotencyKey = "key-1",
            operation = ReceiptOperationType.SELL,
            items = listOf(
                CreateReceiptCommand.ItemInput(
                    name = "Item 1",
                    price = Decimal.parse("100.0"),
                    quantity = Decimal.parse("1.0")
                )
            ),
            payments = listOf(CreateReceiptCommand.PaymentInput(type = "CASH", sum = Decimal.parse("100.0"))),
            discountPercent = null,
            discountSum = null,
            markupPercent = null,
            markupSum = null,
            taken = Decimal.parse("100.0"),
            vatGroup = "VAT_12"
        )
        val failure = assertFailsWith<ValidationException> { processReceipt.execute(req) }
        assertEquals("RECEIPT_VAT_NOT_ALLOWED", failure.code)
    }

    /** Плательщику НДС та же ставка не мешает: правило про режим, а не про ставку. */
    @Test
    fun testProcessReceiptAcceptsVatRateWhenRegimeIsVatPayer() {
        val vatKkm = kkm.copy(taxRegime = TaxRegime.VAT_PAYER, defaultVatGroup = VatGroup.VAT_12)
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns vatKkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-1") } returns null
        every { idGenerator.nextId() } returns "doc-vat"
        every { clock.now() } returns 1000L
        val shift = ShiftInfo(id = "shift-1", kkmId = "kkm-1", shiftNo = 1L, status = ShiftStatus.OPEN, openedAt = 100L)
        every { storage.findOpenShift("kkm-1") } returns shift
        every { queue.canSendDirectly("kkm-1") } returns false

        val req = CreateReceiptCommand(
            kkmId = "kkm-1",
            pin = "1234",
            idempotencyKey = "key-1",
            operation = ReceiptOperationType.SELL,
            items = listOf(
                CreateReceiptCommand.ItemInput(
                    name = "Item 1",
                    price = Decimal.parse("100.0"),
                    quantity = Decimal.parse("1.0"),
                    vatGroup = "VAT_12"
                )
            ),
            payments = listOf(CreateReceiptCommand.PaymentInput(type = "CASH", sum = Decimal.parse("100.0"))),
            discountPercent = null,
            discountSum = null,
            markupPercent = null,
            markupSum = null,
            taken = Decimal.parse("100.0")
        )
        assertNotNull(processReceipt.execute(req))
    }

    /**
     * Покупка у населения платит из ящика, и ОФД чек на сумму больше остатка
     * отвергает. Отказ обязан приходить до фискализации: иначе в журнале
     * остаётся отклонённый документ, а кассир читает «Not enough cash»
     * от ОФД по-английски.
     */
    @Test
    fun `покупка больше остатка ящика отвергается до отправки`() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-buy") } returns null
        every { idGenerator.nextId() } returns "doc-buy"
        every { clock.now() } returns 1000L
        val shift = ShiftInfo(id = "shift-1", kkmId = "kkm-1", shiftNo = 1L, status = ShiftStatus.OPEN, openedAt = 100L)
        every { storage.findOpenShift("kkm-1") } returns shift
        every { queue.canSendDirectly("kkm-1") } returns false
        // В ящике 100,00 ₸, покупка платит 300,00 ₸.
        every { shiftCounters.execute("kkm-1", shift) } returns mapOf(CounterKeyFormats.CASH_SUM to 10_000L)

        val failure = assertFailsWith<ValidationException> { processReceipt.execute(buyFor("key-buy")) }

        assertEquals("INSUFFICIENT_CASH", failure.code)
    }

    /** Хватает наличных — покупка проходит: правило про нехватку, а не про покупку. */
    @Test
    fun `покупка в пределах остатка ящика проходит`() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-buy-ok") } returns null
        every { idGenerator.nextId() } returns "doc-buy-ok"
        every { clock.now() } returns 1000L
        val shift = ShiftInfo(id = "shift-1", kkmId = "kkm-1", shiftNo = 1L, status = ShiftStatus.OPEN, openedAt = 100L)
        every { storage.findOpenShift("kkm-1") } returns shift
        every { queue.canSendDirectly("kkm-1") } returns false
        every { shiftCounters.execute("kkm-1", shift) } returns mapOf(CounterKeyFormats.CASH_SUM to 100_000L)

        assertNotNull(processReceipt.execute(buyFor("key-buy-ok")))
    }

    /**
     * Скидка и наценка на сам чек вместе: сервис приёма отвергает такой
     * чек по существу, и на 2.0.4 кассир получал путь поля JSON
     * («payload.ticket.amounts») вместо слов. Отказ даётся до отправки.
     */
    @Test
    fun `скидка и наценка на чек вместе отвергаются словами`() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()

        val failure = assertFailsWith<ValidationException> {
            processReceipt.execute(
                CreateReceiptCommand(
                    kkmId = "kkm-1",
                    pin = "1234",
                    idempotencyKey = "key-both",
                    operation = ReceiptOperationType.SELL,
                    items = listOf(
                        CreateReceiptCommand.ItemInput(
                            name = "Чай",
                            price = Decimal.parse("800.0"),
                            quantity = Decimal.parse("1.0")
                        )
                    ),
                    payments = listOf(CreateReceiptCommand.PaymentInput(type = "CASH", sum = Decimal.parse("760.0"))),
                    discountPercent = null,
                    discountSum = Decimal.parse("80.0"),
                    markupPercent = null,
                    markupSum = Decimal.parse("40.0"),
                    taken = Decimal.parse("760.0")
                )
            )
        }

        assertEquals("RECEIPT_DISCOUNT_AND_MARKUP", failure.code)
    }

    /**
     * По чеку на 700 ₸ уже вернули 350 ₸ — второй возврат на 700 ₸
     * отвергается: касса считает возвращённое и отдаёт не больше,
     * чем получила. Ни ОФД, ни сервис приёма такой пары не ловят.
     */
    @Test
    fun `возврат больше остатка по чеку-основанию отвергается`() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        val basisMoment = 1_700_000_000_000L
        val refunded = FiscalDocumentSnapshot(
            id = "doc-refund",
            cashboxId = "kkm-1",
            shiftId = "shift-1",
            docType = "SELL_RETURN",
            docNo = 2L,
            shiftNo = 1L,
            createdAt = basisMoment + 1_000L,
            totalAmount = 35_000L,
            currency = "KZT",
            fiscalSign = "fs-refund",
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = "DELIVERED",
            deliveredAt = basisMoment + 1_000L
        )
        every {
            storage.listFiscalDocumentsByPeriod("kkm-1", basisMoment, Long.MAX_VALUE, any(), 0)
        } returns listOf(refunded)
        every {
            storage.listFiscalDocumentsByPeriod("kkm-1", basisMoment, Long.MAX_VALUE, any(), 200)
        } returns emptyList()
        every { storage.findFiscalDocumentWithReceiptPayload("doc-refund") } returns Pair(
            refunded,
            refundOf(basisMoment, Money.fromTiyn(35_000L))
        )

        val failure = assertFailsWith<ValidationException> {
            processReceipt.execute(returnCommandFor(basisMoment, "700.0"))
        }

        assertEquals("REFUND_EXCEEDS_BASIS", failure.code)
        // Сумма в отказе — деньгами, а не устройством типа: кассир читал
        // «не больше Money(bills=0, coins=0)».
        assertEquals(true, failure.message?.contains("350.00") == true)
    }

    /** Возврат в пределах остатка проходит: правило про остаток, а не про возврат. */
    @Test
    fun `возврат в пределах остатка по чеку-основанию проходит`() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-ret") } returns null
        every { idGenerator.nextId() } returns "doc-ret"
        every { clock.now() } returns 1000L
        val shift = ShiftInfo(id = "shift-1", kkmId = "kkm-1", shiftNo = 1L, status = ShiftStatus.OPEN, openedAt = 100L)
        every { storage.findOpenShift("kkm-1") } returns shift
        every { queue.canSendDirectly("kkm-1") } returns false
        val basisMoment = 1_700_000_000_000L
        every { storage.listFiscalDocumentsByPeriod("kkm-1", basisMoment, Long.MAX_VALUE, any(), any()) } returns emptyList()

        assertNotNull(processReceipt.execute(returnCommandFor(basisMoment, "350.0", "key-ret")))
    }

    /** Возвращать больше нечего — говорится словами, а не «не больше 0,00». */
    @Test
    fun `исчерпанный чек-основание объясняется словами`() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        val basisMoment = 1_700_000_000_000L
        val refunded = FiscalDocumentSnapshot(
            id = "doc-full",
            cashboxId = "kkm-1",
            shiftId = "shift-1",
            docType = "SELL_RETURN",
            docNo = 3L,
            shiftNo = 1L,
            createdAt = basisMoment + 1_000L,
            totalAmount = 70_000L,
            currency = "KZT",
            fiscalSign = "fs-full",
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = "DELIVERED",
            deliveredAt = basisMoment + 1_000L
        )
        every {
            storage.listFiscalDocumentsByPeriod("kkm-1", basisMoment, Long.MAX_VALUE, any(), 0)
        } returns listOf(refunded)
        every {
            storage.listFiscalDocumentsByPeriod("kkm-1", basisMoment, Long.MAX_VALUE, any(), 200)
        } returns emptyList()
        every { storage.findFiscalDocumentWithReceiptPayload("doc-full") } returns Pair(
            refunded,
            refundOf(basisMoment, Money.fromTiyn(70_000L))
        )

        val failure = assertFailsWith<ValidationException> {
            processReceipt.execute(returnCommandFor(basisMoment, "100.0", "key-full"))
        }

        assertEquals("REFUND_EXCEEDS_BASIS", failure.code)
        assertEquals(true, failure.message?.contains("уже возвращено всё") == true)
    }

    /** Возврат по чеку на 700 ₸ на заданную сумму. */
    private fun returnCommandFor(
        basisMoment: Long,
        sum: String,
        key: String = "key-over"
    ): CreateReceiptCommand = CreateReceiptCommand(
        kkmId = "kkm-1",
        pin = "1234",
        idempotencyKey = key,
        operation = ReceiptOperationType.SELL_RETURN,
        items = listOf(
            CreateReceiptCommand.ItemInput(
                name = "Сок",
                price = Decimal.parse(sum),
                quantity = Decimal.parse("1.0")
            )
        ),
        payments = listOf(CreateReceiptCommand.PaymentInput(type = "CASH", sum = Decimal.parse(sum))),
        discountPercent = null,
        discountSum = null,
        markupPercent = null,
        markupSum = null,
        taken = Decimal.parse(sum),
        parentTicket = ParentTicket(
            parentTicketNumber = 27367405L,
            parentTicketDateTimeMillis = basisMoment,
            kgdKkmId = "260940000021",
            parentTicketTotal = Money.fromTiyn(70_000L),
            parentTicketIsOffline = false
        )
    )

    /** Прежний возврат по тому же чеку-основанию. */
    private fun refundOf(basisMoment: Long, total: Money): ReceiptRequest = ReceiptRequest(
        kkmId = "kkm-1",
        pin = "1234",
        operation = ReceiptOperationType.SELL_RETURN,
        items = emptyList(),
        payments = emptyList(),
        total = total,
        taken = total,
        change = Money.fromTiyn(0L),
        idempotencyKey = "old-refund",
        parentTicket = ParentTicket(
            parentTicketNumber = 27367405L,
            parentTicketDateTimeMillis = basisMoment,
            kgdKkmId = "260940000021",
            parentTicketTotal = Money.fromTiyn(70_000L),
            parentTicketIsOffline = false
        )
    )

    /** Покупка на 300,00 ₸ наличными: одна позиция, одна оплата. */
    private fun buyFor(key: String): CreateReceiptCommand = CreateReceiptCommand(
        kkmId = "kkm-1",
        pin = "1234",
        idempotencyKey = key,
        operation = ReceiptOperationType.BUY,
        items = listOf(
            CreateReceiptCommand.ItemInput(
                name = "Макулатура",
                price = Decimal.parse("150.0"),
                quantity = Decimal.parse("2.0")
            )
        ),
        payments = listOf(CreateReceiptCommand.PaymentInput(type = "CASH", sum = Decimal.parse("300.0"))),
        discountPercent = null,
        discountSum = null,
        markupPercent = null,
        markupSum = null,
        taken = Decimal.parse("300.0")
    )

    @Test
    fun testProcessReceiptShiftNotOpen() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-1") } returns null
        every { storage.findOpenShift("kkm-1") } returns null

        val req = CreateReceiptCommand(
            kkmId = "kkm-1",
            pin = "1234",
            idempotencyKey = "key-1",
            operation = ReceiptOperationType.SELL,
            items = emptyList(),
            payments = emptyList(),
            discountPercent = null,
            discountSum = null,
            markupPercent = null,
            markupSum = null,
            taken = Decimal.parse("0.0")
        )
        assertFailsWith<ConflictException> {
            processReceipt.execute(req)
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

        val req = CreateReceiptCommand(
            kkmId = "kkm-1",
            pin = "1234",
            idempotencyKey = "key-1",
            operation = ReceiptOperationType.SELL,
            items = emptyList(),
            payments = emptyList(),
            discountPercent = null,
            discountSum = null,
            markupPercent = null,
            markupSum = null,
            taken = Decimal.parse("0.0")
        )
        val res = processReceipt.execute(req)
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

        val req = CreateReceiptCommand(
            kkmId = "kkm-1",
            pin = "1234",
            idempotencyKey = "key-1",
            operation = ReceiptOperationType.SELL,
            items = emptyList(),
            payments = emptyList(),
            discountPercent = null,
            discountSum = null,
            markupPercent = null,
            markupSum = null,
            taken = Decimal.parse("0.0")
        )
        val res = processReceipt.execute(req)
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

        val req = CreateReceiptCommand(
            kkmId = "kkm-1",
            pin = "1234",
            idempotencyKey = "key-1",
            operation = ReceiptOperationType.SELL,
            items = emptyList(),
            payments = emptyList(),
            discountPercent = null,
            discountSum = null,
            markupPercent = null,
            markupSum = null,
            taken = Decimal.parse("0.0")
        )
        val res = processReceipt.execute(req)
        assertEquals("doc-2", res.documentId)
        assertEquals("fs", res.fiscalSign)
        assertEquals("as", res.autonomousSign)
    }

    @Test
    fun testProcessReceiptRequiresParentTicketForReturns() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-1") } returns null

        val req = CreateReceiptCommand(
            kkmId = "kkm-1",
            pin = "1234",
            idempotencyKey = "key-1",
            operation = ReceiptOperationType.SELL_RETURN,
            items = listOf(
                CreateReceiptCommand.ItemInput(name = "Item 1", price = Decimal.parse("100.0"), quantity = Decimal.parse("1.0"))
            ),
            payments = listOf(
                CreateReceiptCommand.PaymentInput(type = "CASH", sum = Decimal.parse("100.0"))
            ),
            discountPercent = null,
            discountSum = null,
            markupPercent = null,
            markupSum = null,
            taken = Decimal.parse("100.0"),
            parentTicket = null
        )
        assertFailsWith<ValidationException> {
            processReceipt.execute(req)
        }
    }

    @Test
    fun testProcessReceiptChecksDiscountConflict() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-1") } returns null

        val req = CreateReceiptCommand(
            kkmId = "kkm-1",
            pin = "1234",
            idempotencyKey = "key-1",
            operation = ReceiptOperationType.SELL,
            items = listOf(
                CreateReceiptCommand.ItemInput(name = "Item 1", price = Decimal.parse("100.0"), quantity = Decimal.parse("1.0"), discountPercent = Decimal.parse("5.0"))
            ),
            payments = listOf(
                CreateReceiptCommand.PaymentInput(type = "CASH", sum = Decimal.parse("95.0"))
            ),
            discountPercent = Decimal.parse("10.0"),
            discountSum = null,
            markupPercent = null,
            markupSum = null,
            taken = Decimal.parse("95.0")
        )
        assertFailsWith<ValidationException> {
            processReceipt.execute(req)
        }
    }

    @Test
    fun testProcessReceiptThrowsWhenTakenIsLessThanCashSum() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-1") } returns null

        val req = CreateReceiptCommand(
            kkmId = "kkm-1",
            pin = "1234",
            idempotencyKey = "key-1",
            operation = ReceiptOperationType.SELL,
            items = listOf(
                CreateReceiptCommand.ItemInput(name = "Item 1", price = Decimal.parse("100.0"), quantity = Decimal.parse("1.0"))
            ),
            payments = listOf(
                CreateReceiptCommand.PaymentInput(type = "CASH", sum = Decimal.parse("100.0"))
            ),
            discountPercent = null,
            discountSum = null,
            markupPercent = null,
            markupSum = null,
            taken = Decimal.parse("50.0")
        )
        assertFailsWith<IllegalArgumentException> {
            processReceipt.execute(req)
        }
    }

    @Test
    fun testProcessReceiptThrowsOnInvalidMeasureUnit() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-1") } returns null

        val req = CreateReceiptCommand(
            kkmId = "kkm-1",
            pin = "1234",
            idempotencyKey = "key-1",
            operation = ReceiptOperationType.SELL,
            items = listOf(
                CreateReceiptCommand.ItemInput(name = "Item 1", price = Decimal.parse("100.0"), quantity = Decimal.parse("1.0"), measureUnitCode = "INVALID")
            ),
            payments = listOf(
                CreateReceiptCommand.PaymentInput(type = "CASH", sum = Decimal.parse("100.0"))
            ),
            discountPercent = null,
            discountSum = null,
            markupPercent = null,
            markupSum = null,
            taken = Decimal.parse("100.0")
        )
        assertFailsWith<ValidationException> {
            processReceipt.execute(req)
        }
    }

    @Test
    fun testProcessReceiptThrowsOnInvalidVatGroup() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-1") } returns null

        val req = CreateReceiptCommand(
            kkmId = "kkm-1",
            pin = "1234",
            idempotencyKey = "key-1",
            operation = ReceiptOperationType.SELL,
            items = listOf(
                CreateReceiptCommand.ItemInput(name = "Item 1", price = Decimal.parse("100.0"), quantity = Decimal.parse("1.0"), vatGroup = "INVALID")
            ),
            payments = listOf(
                CreateReceiptCommand.PaymentInput(type = "CASH", sum = Decimal.parse("100.0"))
            ),
            discountPercent = null,
            discountSum = null,
            markupPercent = null,
            markupSum = null,
            taken = Decimal.parse("100.0")
        )
        assertFailsWith<IllegalArgumentException> {
            processReceipt.execute(req)
        }
    }

    @Test
    fun testProcessReceiptCalculatesItemDiscountAndMarkupCorrectly() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-1") } returns null
        every { idGenerator.nextId() } returns "doc-2"
        every { clock.now() } returns 1000L
        val shift = ShiftInfo(id = "shift-1", kkmId = "kkm-1", shiftNo = 1L, status = ShiftStatus.OPEN, openedAt = 100L)
        every { storage.findOpenShift("kkm-1") } returns shift
        every { queue.canSendDirectly("kkm-1") } returns true
        val ofdResult = OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0)
        every { kkmCommonHelper.sendOfdCommand(kkm, OfdCommandType.TICKET, "doc-2") } returns ofdResult

        val req = CreateReceiptCommand(
            kkmId = "kkm-1",
            pin = "1234",
            idempotencyKey = "key-1",
            operation = ReceiptOperationType.SELL,
            items = listOf(
                CreateReceiptCommand.ItemInput(name = "Item 1", price = Decimal.parse("100.0"), quantity = Decimal.parse("2.0"), discountPercent = Decimal.parse("10.0"), measureUnitCode = "796"),
                CreateReceiptCommand.ItemInput(name = "Item 2", price = Decimal.parse("100.0"), quantity = Decimal.parse("2.0"), markupSum = Decimal.parse("15.0"))
            ),
            payments = listOf(
                CreateReceiptCommand.PaymentInput(type = "CASH", sum = Decimal.parse("395.0"))
            ),
            discountPercent = null,
            discountSum = null,
            markupPercent = null,
            markupSum = null,
            taken = Decimal.parse("400.0")
        )

        processReceipt.execute(req)

        verify {
            storage.saveReceipt(match { request ->
                request.total == Money.fromTenge(Decimal.parse("395.0")) &&
                request.items[0].sum == Money.fromTenge(Decimal.parse("180.0")) &&
                request.items[0].discount == Money.fromTenge(Decimal.parse("20.0")) &&
                request.items[0].measureUnitCode == "796" &&
                request.items[1].sum == Money.fromTenge(Decimal.parse("215.0")) &&
                request.items[1].markup == Money.fromTenge(Decimal.parse("15.0")) &&
                request.change == Money.fromTenge(Decimal.parse("5.0"))
            }, "doc-2", "shift-1", 1000L)
        }
    }

    @Test
    fun testProcessReceiptKeepsTiynOnFractionalPriceAndQuantity() {
        // 150.55 × 3 в Double даёт 451.64999999999995, а скидка 3.5 % от неё —
        // ещё одно расхождение. Чек обязан сойтись в тиынах: 45165 - 1581 = 43584.
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-1") } returns null
        every { idGenerator.nextId() } returns "doc-2"
        every { clock.now() } returns 1000L
        val shift = ShiftInfo(id = "shift-1", kkmId = "kkm-1", shiftNo = 1L, status = ShiftStatus.OPEN, openedAt = 100L)
        every { storage.findOpenShift("kkm-1") } returns shift
        every { queue.canSendDirectly("kkm-1") } returns true
        every {
            kkmCommonHelper.sendOfdCommand(kkm, OfdCommandType.TICKET, "doc-2")
        } returns OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0)

        val req = CreateReceiptCommand(
            kkmId = "kkm-1",
            pin = "1234",
            idempotencyKey = "key-1",
            operation = ReceiptOperationType.SELL,
            items = listOf(
                CreateReceiptCommand.ItemInput(
                    name = "Кофе",
                    price = Decimal.parse("150.55"),
                    quantity = Decimal.parse("3"),
                    discountPercent = Decimal.parse("3.5")
                )
            ),
            payments = listOf(CreateReceiptCommand.PaymentInput(type = "CASH", sum = Decimal.parse("435.84"))),
            discountPercent = null,
            discountSum = null,
            markupPercent = null,
            markupSum = null,
            taken = Decimal.parse("500")
        )

        processReceipt.execute(req)

        verify {
            storage.saveReceipt(match { request ->
                request.items[0].sum.tiyn() == 43584L &&
                request.items[0].discount?.tiyn() == 1581L &&
                request.items[0].quantity == 3000L &&
                request.total.tiyn() == 43584L &&
                request.change?.tiyn() == 6416L
            }, "doc-2", "shift-1", 1000L)
        }
    }

    @Test
    fun `сдача смешанного чека считается с наличной части`() {
        // 1000 к оплате: 600 картой, 400 наличными, покупатель дал 500.
        // Сдача — 100, а не «принято минус итог»: с итога она ушла бы в ноль,
        // и покупатель недополучил бы свои деньги.
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-1") } returns null
        every { idGenerator.nextId() } returns "doc-3"
        every { clock.now() } returns 1000L
        val shift = ShiftInfo(id = "shift-1", kkmId = "kkm-1", shiftNo = 1L, status = ShiftStatus.OPEN, openedAt = 100L)
        every { storage.findOpenShift("kkm-1") } returns shift
        every { queue.canSendDirectly("kkm-1") } returns true
        every {
            kkmCommonHelper.sendOfdCommand(kkm, OfdCommandType.TICKET, "doc-3")
        } returns OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0)

        processReceipt.execute(mixedReceipt(cash = "400.0", card = "600.0", taken = "500.0"))

        verify {
            storage.saveReceipt(match { request ->
                request.total.tiyn() == 100_000L &&
                    request.taken?.tiyn() == 50_000L &&
                    request.change?.tiyn() == 10_000L &&
                    request.payments.sumOf { it.sum.tiyn() } == 100_000L
            }, "doc-3", "shift-1", 1000L)
        }
    }

    @Test
    fun `оплаты, не сходящиеся с итогом, чек не пробивают`() {
        // 600 картой и 300 наличными при итоге 1000: ОФД принял бы документ,
        // в котором заплачено не столько, сколько пробито, а расхождение
        // нашлось бы на инкассации.
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-1") } returns null

        val refusal = assertFailsWith<ValidationException> {
            processReceipt.execute(mixedReceipt(cash = "300.0", card = "600.0", taken = null))
        }
        assertEquals("PAYMENTS_TOTAL_MISMATCH", refusal.code)
    }

    /** Чек на 1000 из одной позиции, оплаченный двумя видами. */
    private fun mixedReceipt(cash: String, card: String, taken: String?): CreateReceiptCommand =
        CreateReceiptCommand(
            kkmId = "kkm-1",
            pin = "1234",
            idempotencyKey = "key-1",
            operation = ReceiptOperationType.SELL,
            items = listOf(
                CreateReceiptCommand.ItemInput(
                    name = "Чайник",
                    price = Decimal.parse("1000.0"),
                    quantity = Decimal.parse("1.0")
                )
            ),
            payments = listOf(
                CreateReceiptCommand.PaymentInput(type = "CARD", sum = Decimal.parse(card)),
                CreateReceiptCommand.PaymentInput(type = "CASH", sum = Decimal.parse(cash))
            ),
            discountPercent = null,
            discountSum = null,
            markupPercent = null,
            markupSum = null,
            taken = taken?.let { Decimal.parse(it) }
        )

    @Test
    fun testProcessReceiptTakenNull() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-1") } returns null
        every { idGenerator.nextId() } returns "doc-2"
        every { clock.now() } returns 1000L
        val shift = ShiftInfo(id = "shift-1", kkmId = "kkm-1", shiftNo = 1L, status = ShiftStatus.OPEN, openedAt = 100L)
        every { storage.findOpenShift("kkm-1") } returns shift
        every { queue.canSendDirectly("kkm-1") } returns true
        val ofdResult = OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0)
        every { kkmCommonHelper.sendOfdCommand(kkm, OfdCommandType.TICKET, "doc-2") } returns ofdResult

        val req = CreateReceiptCommand(
            kkmId = "kkm-1",
            pin = "1234",
            idempotencyKey = "key-1",
            operation = ReceiptOperationType.SELL,
            items = listOf(
                CreateReceiptCommand.ItemInput(name = "Item 1", price = Decimal.parse("100.0"), quantity = Decimal.parse("2.0"))
            ),
            payments = listOf(
                CreateReceiptCommand.PaymentInput(type = "CASH", sum = Decimal.parse("200.0"))
            ),
            discountPercent = null,
            discountSum = null,
            markupPercent = null,
            markupSum = null,
            taken = null
        )

        processReceipt.execute(req)

        verify {
            storage.saveReceipt(match { request ->
                request.taken == Money.fromTenge(Decimal.parse("200.0")) && request.change == null
            }, "doc-2", "shift-1", 1000L)
        }
    }

    @Test
    fun testProcessReceiptThrowsOnInvalidDefaultVatGroup() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-1") } returns null

        val req = CreateReceiptCommand(
            kkmId = "kkm-1",
            pin = "1234",
            idempotencyKey = "key-1",
            operation = ReceiptOperationType.SELL,
            items = listOf(
                CreateReceiptCommand.ItemInput(name = "Item 1", price = Decimal.parse("100.0"), quantity = Decimal.parse("1.0"))
            ),
            payments = listOf(
                CreateReceiptCommand.PaymentInput(type = "CASH", sum = Decimal.parse("100.0"))
            ),
            discountPercent = null,
            discountSum = null,
            markupPercent = null,
            markupSum = null,
            taken = Decimal.parse("100.0"),
            vatGroup = "INVALID"
        )
        assertFailsWith<IllegalArgumentException> {
            processReceipt.execute(req)
        }
    }

    @Test
    fun testProcessReceiptThrowsOnInvalidPaymentType() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-1") } returns null

        val req = CreateReceiptCommand(
            kkmId = "kkm-1",
            pin = "1234",
            idempotencyKey = "key-1",
            operation = ReceiptOperationType.SELL,
            items = listOf(
                CreateReceiptCommand.ItemInput(name = "Item 1", price = Decimal.parse("100.0"), quantity = Decimal.parse("1.0"))
            ),
            payments = listOf(
                CreateReceiptCommand.PaymentInput(type = "INVALID", sum = Decimal.parse("100.0"))
            ),
            discountPercent = null,
            discountSum = null,
            markupPercent = null,
            markupSum = null,
            taken = Decimal.parse("100.0")
        )
        assertFailsWith<IllegalArgumentException> {
            processReceipt.execute(req)
        }
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
            storage.updateReceiptStatus("doc-3", ofdResult.fiscalSign, ofdResult.autonomousSign, "SENT", null, 1200L, false)
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
            storage.updateReceiptStatus("doc-3", ofdResult.fiscalSign, ofdResult.autonomousSign, "SENT", null, 1200L, false)
            updateCountersUseCase.execute("kkm-1", "shift-1", receiptReq, false)
        }
        verify(exactly = 0) {
            receiptDeliveryHelper.deliverReceipt(any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun testProcessOfdDocumentResultSuccessWithTicketAds() {
        val responseJson = kotlinx.serialization.json.Json.parseToJsonElement(
            """{
                "payload": {
                    "service": {
                        "ticketAds": [
                            {"text": "Fresh Ads Content"}
                        ]
                    }
                }
            }"""
        ) as kotlinx.serialization.json.JsonObject

        val ofdResult = OfdCommandResult(
            status = OfdCommandStatus.OK, 
            resultCode = 0, 
            receiptUrl = "http://ofd/receipt", 
            responseBin = byteArrayOf(2),
            responseJson = responseJson
        )
        val receiptReq = mockk<ReceiptRequest>()
        val shiftId = "shift-1"
        
        every { storage.findKkmForUpdate("kkm-1") } returns kkm
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
            storage.updateReceiptStatus("doc-3", ofdResult.fiscalSign, ofdResult.autonomousSign, "SENT", null, 1200L, false)
            updateCountersUseCase.execute("kkm-1", "shift-1", receiptReq, false)
            storage.updateKkm(any())
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
            storage.updateReceiptStatus("doc-3", null, "1300", "PENDING", null, null, true)
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
