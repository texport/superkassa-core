package kz.mybrain.superkassa.core.domain.helper.common

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.model.delivery.DeliveryStatus
import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kz.mybrain.superkassa.core.domain.model.kkm.KkmState
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandResult
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandStatus
import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.IdGeneratorPort
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.usecase.auth.AuthorizeUserUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IdempotentOperationExecutorTest {

    private val storage = mockk<StoragePort>(relaxed = true)
    private val idGenerator = mockk<IdGeneratorPort>()
    private val clock = mockk<ClockPort>()
    private val authorizeUserUseCase = mockk<AuthorizeUserUseCase>()
    private val executor = IdempotentOperationExecutor(storage, idGenerator, clock, authorizeUserUseCase)

    init {
        every { storage.inTransaction<Any>(any()) } answers {
            val block = firstArg<() -> Any>()
            block()
        }
    }

    @Test
    fun testExecuteNewOperationSuccess() {
        val kkm = KkmInfo(id = "kkm-1", createdAt = 0L, updatedAt = 0L, mode = "ACTIVE", state = KkmState.ACTIVE.name)
        every { authorizeUserUseCase.requireKkm("kkm-1", any()) } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any(), any()) } returns mockk()
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
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any(), any()) } returns mockk()
        every { storage.findIdempotencyResponse("kkm-1", "key-1") } returns "doc-existing"

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
    fun testExecuteKkmProgrammingState() {
        val kkm = KkmInfo(id = "kkm-1", createdAt = 0L, updatedAt = 0L, mode = "ACTIVE", state = KkmState.PROGRAMMING.name)
        every { authorizeUserUseCase.requireKkm("kkm-1", any()) } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any(), any()) } returns mockk()

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
