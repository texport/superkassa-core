package io.github.texport.superkassa.core.domain.impl.helper.common

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.delivery.DeliveryStatus
import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftStatus
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.internal.IdGeneratorPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.api.port.integration.inTransaction
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.domain.impl.usecase.auth.AuthorizeUserUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.kkm.RequireOperationalUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IdempotentOperationExecutorTest {

    private val storage = mockk<StoragePort>(relaxed = true)
    private val idGenerator = mockk<IdGeneratorPort>()
    private val clock = mockk<ClockPort>()
    private val authorizeUserUseCase = mockk<AuthorizeUserUseCase>()
    private val requireOperationalUseCase = mockk<RequireOperationalUseCase>(relaxed = true)
    private val executor = IdempotentOperationExecutor(storage, idGenerator, clock, authorizeUserUseCase, requireOperationalUseCase)

    private val shift = ShiftInfo(
        id = "shift-1",
        kkmId = "kkm-1",
        shiftNo = 2,
        status = ShiftStatus.OPEN,
        openedAt = NOW - DAY
    )

    init {
        // Часы спрашиваются на каждой операции: касса сверяет
        // продолжительность смены прежде, чем что-либо оформить.
        every { clock.now() } returns NOW
        // По умолчанию платёжных документов в смене нет: отсчёт суток
        // ещё не начался, и проверка продолжительности пропускает.
        every { storage.firstPaymentTimeInShift(any()) } returns null
    }

    @Test
    fun testExecuteNewOperationSuccess() {
        val kkm = KkmInfo(id = "kkm-1", createdAt = 0L, updatedAt = 0L, mode = "ACTIVE", state = KkmState.ACTIVE.name)
        every { authorizeUserUseCase.requireKkm("kkm-1", any()) } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-1") } returns null
        every { idGenerator.nextId() } returns "doc-1"
        every { clock.now() } returns 1000L

        val checkShift = { "shift-1" }
        val saveOperation = mockk<(String, Long, String) -> Unit>(relaxed = true)
        val sendOfdCommand = mockk<(KkmInfo, String) -> OfdCommandResult>()
        val ofdResult = OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0)
        every { sendOfdCommand(kkm, "doc-1") } returns ofdResult
        val processResult = mockk<(KkmInfo, String, String, OfdCommandResult, Any, Long, Any?) -> Unit>(relaxed = true)
        val buildResult = { docId: String, res: OfdCommandResult, status: DeliveryStatus ->
            "result-$docId-$status"
        }

        val res = executor.executeIdempotentFiscalOperation(
            kkmId = "kkm-1",
            pin = "1234",
            idempotencyKey = "key-1",
            operationType = "CREATE_RECEIPT",
            checkShift = checkShift,
            saveOperation = saveOperation,
            sendOfdCommand = sendOfdCommand,
            processResult = { k, doc, kId, r, type, now, ctx -> processResult(k, doc, kId, r, type, now, ctx) },
            buildResult = buildResult
        )

        assertEquals("result-doc-1-ONLINE_OK", res)
        verify { storage.insertIdempotency("kkm-1", "key-1", "CREATE_RECEIPT") }
        verify { saveOperation("doc-1", 1000L, "shift-1") }
        verify { storage.updateIdempotencyResponse("kkm-1", "key-1", "doc-1") }
    }

    @Test
    fun testExecuteIdempotencyHit() {
        val kkm = KkmInfo(id = "kkm-1", createdAt = 0L, updatedAt = 0L, mode = "ACTIVE", state = KkmState.ACTIVE.name)
        every { authorizeUserUseCase.requireKkm("kkm-1", any()) } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-1") } returns "doc-existing"
        every { storage.findFiscalDocumentById("doc-existing") } returns existing("SENT")

        val buildResult = { docId: String, res: OfdCommandResult, status: DeliveryStatus ->
            "result-$docId-$status"
        }

        val res = executor.executeIdempotentFiscalOperation(
            kkmId = "kkm-1",
            pin = "1234",
            idempotencyKey = "key-1",
            operationType = "CREATE_RECEIPT",
            checkShift = { "shift-1" },
            saveOperation = { _, _, _ -> },
            sendOfdCommand = { _, _ -> mockk() },
            processResult = { _, _, _, _, _, _, _ -> },
            buildResult = buildResult
        )

        assertEquals("result-doc-existing-ONLINE_OK", res)
    }

    @Test
    fun `смена длиннее суток операций не допускает`() {
        // Требование к ККМ (пункты 14, 52 и 93): сверх суток касса обязана
        // перестать оформлять кассовые операции, пока смену не закроют.
        // Отсчёт идёт с первого платёжного документа смены.
        val kkm = KkmInfo(id = "kkm-1", createdAt = 0L, updatedAt = 0L, mode = "ACTIVE", state = KkmState.ACTIVE.name)
        every { authorizeUserUseCase.requireKkm("kkm-1", any()) } returns kkm
        every { storage.findOpenShift("kkm-1") } returns shift
        every { storage.firstPaymentTimeInShift("shift-1") } returns NOW - DAY - 1

        val failure = assertFailsWith<ValidationException> { punch() }

        assertEquals("SHIFT_LONGER_THAN_DAY", failure.code)
    }

    @Test
    fun `смена ровно суток операции ещё допускает`() {
        val kkm = KkmInfo(id = "kkm-1", createdAt = 0L, updatedAt = 0L, mode = "ACTIVE", state = KkmState.ACTIVE.name)
        every { authorizeUserUseCase.requireKkm("kkm-1", any()) } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findOpenShift("kkm-1") } returns shift
        every { storage.firstPaymentTimeInShift("shift-1") } returns NOW - DAY
        every { storage.findIdempotencyResponse("kkm-1", "key-1") } returns "doc-existing"
        every { storage.findFiscalDocumentById("doc-existing") } returns existing("SENT")

        assertEquals("result-doc-existing-ONLINE_OK", punch())
    }

    @Test
    fun `повтор документа, ушедшего в очередь, отвечает «в очереди», а не «доставлен»`() {
        val kkm = KkmInfo(id = "kkm-1", createdAt = 0L, updatedAt = 0L, mode = "ACTIVE", state = KkmState.ACTIVE.name)
        every { authorizeUserUseCase.requireKkm("kkm-1", any()) } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-1") } returns "doc-existing"
        every { storage.findFiscalDocumentById("doc-existing") } returns existing("PENDING")

        assertEquals("result-doc-existing-OFFLINE_QUEUED", punch())
    }

    private fun existing(ofdStatus: String) = FiscalDocumentSnapshot(
        id = "doc-existing", cashboxId = "kkm-1", shiftId = "shift-1", docType = "SALE", docNo = null, shiftNo = 1L,
        createdAt = NOW, totalAmount = 100L, currency = "KZT", fiscalSign = null, autonomousSign = null,
        isAutonomous = ofdStatus == "PENDING", ofdStatus = ofdStatus, deliveredAt = null
    )

    private fun punch(): String = executor.executeIdempotentFiscalOperation(
        kkmId = "kkm-1",
        pin = "1234",
        idempotencyKey = "key-1",
        operationType = "CREATE_RECEIPT",
        checkShift = { "shift-1" },
        saveOperation = { _, _, _ -> },
        sendOfdCommand = { _, _ -> mockk() },
        processResult = { _, _, _, _, _, _, _ -> },
        buildResult = { docId: String, _: OfdCommandResult, status: DeliveryStatus -> "result-$docId-$status" }
    )

    @Test
    fun testExecuteKkmProgrammingState() {
        val kkm = KkmInfo(id = "kkm-1", createdAt = 0L, updatedAt = 0L, mode = "ACTIVE", state = KkmState.PROGRAMMING.name)
        every { authorizeUserUseCase.requireKkm("kkm-1", any()) } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { requireOperationalUseCase.execute(kkm) } throws ValidationException(
            trilingualMessage = CoreStrings.kkmInProgramming(),
            code = "KKM_IN_PROGRAMMING"
        )

        assertFailsWith<ValidationException> {
            executor.executeIdempotentFiscalOperation(
                kkmId = "kkm-1",
                pin = "1234",
                idempotencyKey = "key-1",
                operationType = "CREATE_RECEIPT",
                checkShift = { "shift-1" },
                saveOperation = { _, _, _ -> },
                sendOfdCommand = { _, _ -> mockk() },
                processResult = { _, _, _, _, _, _, _ -> },
                buildResult = { _, _, _ -> "res" }
            )
        }
    }
}

/** Время, которое показывают часы в проверках. */
private const val NOW: Long = 1_700_000_000_000L

/** Сутки в миллисекундах. */
private const val DAY: Long = 24L * 60 * 60 * 1000
