package kz.mybrain.superkassa.core.domain.usecase.ofd

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kz.mybrain.superkassa.core.domain.exception.ErrorMessages
import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.helper.KkmCommonHelper
import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandResult
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandStatus
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandType
import kz.mybrain.superkassa.core.domain.model.shift.ShiftStatus
import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.IdGeneratorPort
import kz.mybrain.superkassa.core.domain.port.OfflineQueuePort
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.port.TokenCodecPort
import kz.mybrain.superkassa.core.domain.usecase.auth.AuthorizeUserUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OfdUseCasesTest {

    private val storage = mockk<StoragePort>(relaxed = true)
    private val clock = mockk<ClockPort>()
    private val idGenerator = mockk<IdGeneratorPort>()
    private val queue = mockk<OfflineQueuePort>()
    private val authorizeUserUseCase = mockk<AuthorizeUserUseCase>()
    private val tokenCodec = mockk<TokenCodecPort>()
    private val kkmCommonHelper = mockk<KkmCommonHelper>(relaxed = true)
    private val generateRequestNumber = GenerateRequestNumberUseCase(storage)

    private val getOfdAuthInfo = GetOfdAuthInfoUseCase(authorizeUserUseCase, tokenCodec, generateRequestNumber)
    private val updateOfdToken = UpdateOfdTokenUseCase(storage, clock, tokenCodec, authorizeUserUseCase)
    private val getOfdInfo = GetOfdInfoUseCase(authorizeUserUseCase, kkmCommonHelper)
    private val checkOfdConnection = CheckOfdConnectionUseCase(authorizeUserUseCase, kkmCommonHelper)
    private val sendFiscalCommand = SendFiscalCommandUseCase(authorizeUserUseCase, kkmCommonHelper)
    private val syncOfdServiceInfo = SyncOfdServiceInfoUseCase(storage, queue, clock, idGenerator, authorizeUserUseCase, kkmCommonHelper)
    private val syncOfdCounters = SyncOfdCountersUseCase(storage, queue, clock, idGenerator, authorizeUserUseCase, kkmCommonHelper)

    private val kkm = KkmInfo(
        id = "kkm-1",
        createdAt = 0,
        updatedAt = 0,
        mode = "ACTIVE",
        state = "ACTIVE",
        tokenEncryptedBase64 = "encrypted-token"
    )

    init {
        every { storage.findKkmForUpdate(any()) } answers { storage.findKkm(firstArg()) }
        every { authorizeUserUseCase.requireKkm(any(), any()) } answers { authorizeUserUseCase.requireKkm(firstArg()) }
        every { authorizeUserUseCase.requireRole(any(), any(), any(), any()) } answers { authorizeUserUseCase.requireRole(firstArg(), secondArg(), thirdArg()) }
        every { storage.inTransaction<Any?>(any()) } answers {
            val block = firstArg<() -> Any?>()
            block()
        }
    }

    @Test
    fun testGetOfdAuthInfoSuccess() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { tokenCodec.decodeToken("encrypted-token") } returns 9999L
        every { storage.loadCounters("kkm-1", any(), null) } returns mapOf("ofd.req_num" to 41L)

        val authInfo = getOfdAuthInfo.execute("kkm-1", "1234")
        assertEquals("9999", authInfo.token)
        assertEquals(42, authInfo.nextReqNum)
    }

    @Test
    fun testUpdateOfdTokenSuccess() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { clock.now() } returns 2000L
        every { tokenCodec.parseToken("new-token") } returns 9999L
        every { tokenCodec.encodeToken(9999L) } returns "new-token-base64"
        every { storage.updateKkmToken("kkm-1", "new-token-base64", 2000L) } returns true

        val res = updateOfdToken.execute("kkm-1", "1234", "new-token")
        assertEquals(true, res)
    }

    @Test
    fun testUpdateOfdTokenBlankToken() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { tokenCodec.parseToken("") } throws ValidationException(ErrorMessages.badRequest(), "TOKEN_BLANK")

        assertFailsWith<ValidationException> {
            updateOfdToken.execute("kkm-1", "1234", "")
        }
    }

    @Test
    fun testGenerateRequestNumber() {
        every { storage.loadCounters("kkm-1", any(), null) } returns mapOf("ofd.req_num" to 10L)
        val num = generateRequestNumber.execute("kkm-1")
        assertEquals(11, num)
    }

    @Test
    fun testGetOfdInfo() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        val expectedResult = OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0)
        every { kkmCommonHelper.sendOfdCommand(kkm, OfdCommandType.INFO, "") } returns expectedResult

        val res = getOfdInfo.execute("kkm-1")
        assertEquals(expectedResult, res)
    }

    @Test
    fun testCheckOfdConnection() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        val expectedResult = OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0)
        every { kkmCommonHelper.sendOfdCommand(kkm, OfdCommandType.SYSTEM, "") } returns expectedResult

        val res = checkOfdConnection.execute("kkm-1")
        assertEquals(expectedResult, res)
    }

    @Test
    fun testSendFiscalCommand() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        val expectedResult = OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0)
        every { kkmCommonHelper.sendOfdCommand(kkm, OfdCommandType.TICKET, "doc-1") } returns expectedResult

        val res = sendFiscalCommand.execute("kkm-1", OfdCommandType.TICKET, "doc-1")
        assertEquals(expectedResult, res)
    }

    @Test
    fun testSyncOfdServiceInfoSuccess() {
        every { kkmCommonHelper.requireSyncAllowed("kkm-1", "1234", false, authorizeUserUseCase, queue) } returns kkm
        every { idGenerator.nextId() } returns "req-id-1"
        val expectedResult = OfdCommandResult(
            status = OfdCommandStatus.OK,
            resultCode = 0,
            responseJson = Json.parseToJsonElement("{\"payload\":{\"report\":{\"zxReport\":{\"org\":{\"name\":\"test\"}}}}}" + "").jsonObject
        )
        every { kkmCommonHelper.sendOfdCommand(kkm, OfdCommandType.INFO, "req-id-1") } returns expectedResult
        every { clock.now() } returns 1000L
        every { storage.findKkm("kkm-1") } returns kkm

        val res = syncOfdServiceInfo.execute("kkm-1", "1234")
        assertEquals(expectedResult, res)
        verify {
            storage.updateKkm(any())
        }
    }

    @Test
    fun testSyncOfdCountersSuccess() {
        every { kkmCommonHelper.requireSyncAllowed("kkm-1", "1234", true, authorizeUserUseCase, queue) } returns kkm
        every { idGenerator.nextId() } returns "req-id-1"
        val expectedResult = OfdCommandResult(
            status = OfdCommandStatus.OK,
            resultCode = 0,
            responseJson = Json.parseToJsonElement(
                "{\"payload\":{\"report\":{\"reportType\":\"REPORT_X\",\"zxReport\":{\"shiftNumber\":5,\"operations\":[],\"ticketOperations\":[],\"cashSum\":{\"bills\":0},\"revenue\":{\"sum\":{\"bills\":0},\"isNegative\":false},\"nonNullableSums\":[],\"startShiftNonNullableSums\":[]}}}}"
            ).jsonObject
        )
        every { kkmCommonHelper.sendOfdCommand(kkm, OfdCommandType.INFO, "req-id-1") } returns expectedResult
        every { clock.now() } returns 1000L
        every { storage.findOpenShift("kkm-1") } returns null
        every { storage.findKkm("kkm-1") } returns kkm

        val res = syncOfdCounters.execute("kkm-1", "1234")
        assertEquals(expectedResult, res)
        verify {
            storage.createShift(match { it.shiftNo == 5L && it.status == ShiftStatus.OPEN })
            storage.updateKkm(any())
        }
    }

    @Test
    fun testSyncOfdServiceInfoCommandFailure() {
        every { kkmCommonHelper.requireSyncAllowed("kkm-1", "1234", false, authorizeUserUseCase, queue) } returns kkm
        every { idGenerator.nextId() } returns "req-id-1"
        val expectedResult = OfdCommandResult(status = OfdCommandStatus.FAILED, resultCode = -1, responseJson = null)
        every { kkmCommonHelper.sendOfdCommand(kkm, OfdCommandType.INFO, "req-id-1") } returns expectedResult

        val res = syncOfdServiceInfo.execute("kkm-1", "1234")
        assertEquals(expectedResult, res)
    }

    @Test
    fun testSyncOfdServiceInfoKkmNotFound() {
        every { kkmCommonHelper.requireSyncAllowed("kkm-1", "1234", false, authorizeUserUseCase, queue) } returns kkm
        every { idGenerator.nextId() } returns "req-id-1"
        val expectedResult = OfdCommandResult(
            status = OfdCommandStatus.OK,
            resultCode = 0,
            responseJson = Json.parseToJsonElement("{\"payload\":{\"report\":{\"zxReport\":{\"org\":{\"name\":\"test\"}}}}}" + "").jsonObject
        )
        every { kkmCommonHelper.sendOfdCommand(kkm, OfdCommandType.INFO, "req-id-1") } returns expectedResult
        every { clock.now() } returns 1000L
        every { storage.findKkm("kkm-1") } returns null

        val res = syncOfdServiceInfo.execute("kkm-1", "1234")
        assertEquals(expectedResult, res)
    }

    @Test
    fun testSyncOfdServiceInfoNullServiceInfoFallback() {
        val kkmWithNullServiceInfo = kkm.copy(ofdServiceInfo = null)
        every { kkmCommonHelper.requireSyncAllowed("kkm-1", "1234", false, authorizeUserUseCase, queue) } returns kkmWithNullServiceInfo
        every { idGenerator.nextId() } returns "req-id-1"
        val expectedResult = OfdCommandResult(
            status = OfdCommandStatus.OK,
            resultCode = 0,
            responseJson = Json.parseToJsonElement("{\"payload\":{\"report\":{\"zxReport\":{\"org\":{\"name\":\"test\"}}}}}" + "").jsonObject
        )
        every { kkmCommonHelper.sendOfdCommand(kkmWithNullServiceInfo, OfdCommandType.INFO, "req-id-1") } returns expectedResult
        every { clock.now() } returns 1000L
        every { storage.findKkm("kkm-1") } returns kkmWithNullServiceInfo

        val res = syncOfdServiceInfo.execute("kkm-1", "1234")
        assertEquals(expectedResult, res)
        verify {
            storage.updateKkm(any())
        }
    }

    @Test
    fun testSyncOfdCountersCommandFailure() {
        every { kkmCommonHelper.requireSyncAllowed("kkm-1", "1234", true, authorizeUserUseCase, queue) } returns kkm
        every { idGenerator.nextId() } returns "req-id-1"
        val expectedResult = OfdCommandResult(status = OfdCommandStatus.FAILED, resultCode = -1, responseJson = null)
        every { kkmCommonHelper.sendOfdCommand(kkm, OfdCommandType.INFO, "req-id-1") } returns expectedResult

        val res = syncOfdCounters.execute("kkm-1", "1234")
        assertEquals(expectedResult, res)
    }

    @Test
    fun testSyncOfdCountersIsOpenShiftAndLocalOpenShiftExists() {
        every { kkmCommonHelper.requireSyncAllowed("kkm-1", "1234", true, authorizeUserUseCase, queue) } returns kkm
        every { idGenerator.nextId() } returns "req-id-1"
        val expectedResult = OfdCommandResult(
            status = OfdCommandStatus.OK,
            resultCode = 0,
            responseJson = Json.parseToJsonElement(
                "{\"payload\":{\"report\":{\"reportType\":\"REPORT_X\",\"zxReport\":{\"shiftNumber\":5,\"operations\":[],\"ticketOperations\":[],\"cashSum\":{\"bills\":0},\"revenue\":{\"sum\":{\"bills\":0},\"isNegative\":false},\"nonNullableSums\":[],\"startShiftNonNullableSums\":[]}}}}"
            ).jsonObject
        )
        every { kkmCommonHelper.sendOfdCommand(kkm, OfdCommandType.INFO, "req-id-1") } returns expectedResult
        every { clock.now() } returns 1000L
        val localOpenShift = kz.mybrain.superkassa.core.domain.model.shift.ShiftInfo(
            id = "shift-1", kkmId = "kkm-1", shiftNo = 5L, status = ShiftStatus.OPEN, openedAt = 500L, closedAt = null
        )
        every { storage.findOpenShift("kkm-1") } returns localOpenShift
        every { storage.findKkm("kkm-1") } returns kkm

        val res = syncOfdCounters.execute("kkm-1", "1234")
        assertEquals(expectedResult, res)
        verify(exactly = 0) {
            storage.createShift(any())
        }
    }

    @Test
    fun testSyncOfdCountersIsOpenShiftTimeNullFallback() {
        every { kkmCommonHelper.requireSyncAllowed("kkm-1", "1234", true, authorizeUserUseCase, queue) } returns kkm
        every { idGenerator.nextId() } returns "req-id-1"
        val expectedResult = OfdCommandResult(
            status = OfdCommandStatus.OK,
            resultCode = 0,
            responseJson = Json.parseToJsonElement(
                "{\"payload\":{\"report\":{\"reportType\":\"REPORT_X\",\"zxReport\":{\"shiftNumber\":5,\"operations\":[],\"ticketOperations\":[],\"cashSum\":{\"bills\":0},\"revenue\":{\"sum\":{\"bills\":0},\"isNegative\":false},\"nonNullableSums\":[],\"startShiftNonNullableSums\":[]}}}}"
            ).jsonObject
        )
        every { kkmCommonHelper.sendOfdCommand(kkm, OfdCommandType.INFO, "req-id-1") } returns expectedResult
        every { clock.now() } returns 1000L
        every { storage.findOpenShift("kkm-1") } returns null
        every { storage.findKkm("kkm-1") } returns kkm

        val res = syncOfdCounters.execute("kkm-1", "1234")
        assertEquals(expectedResult, res)
        verify {
            storage.createShift(match { it.openedAt == 1000L })
        }
    }

    @Test
    fun testSyncOfdCountersIsClosedShiftLocalOpenShiftExists() {
        every { kkmCommonHelper.requireSyncAllowed("kkm-1", "1234", true, authorizeUserUseCase, queue) } returns kkm
        every { idGenerator.nextId() } returns "req-id-1"
        val expectedResult = OfdCommandResult(
            status = OfdCommandStatus.OK,
            resultCode = 0,
            responseJson = Json.parseToJsonElement(
                "{\"payload\":{\"report\":{\"reportType\":\"REPORT_Z\",\"zxReport\":{\"shiftNumber\":5,\"cashSum\":{\"bills\":0},\"revenue\":{\"sum\":{\"bills\":0},\"isNegative\":false},\"nonNullableSums\":[]}}}}"
            ).jsonObject
        )
        every { kkmCommonHelper.sendOfdCommand(kkm, OfdCommandType.INFO, "req-id-1") } returns expectedResult
        every { clock.now() } returns 1000L
        val localOpenShift = kz.mybrain.superkassa.core.domain.model.shift.ShiftInfo(
            id = "shift-1", kkmId = "kkm-1", shiftNo = 5L, status = ShiftStatus.OPEN, openedAt = 500L, closedAt = null
        )
        every { storage.findOpenShift("kkm-1") } returns localOpenShift
        every { storage.findKkm("kkm-1") } returns kkm

        val res = syncOfdCounters.execute("kkm-1", "1234")
        assertEquals(expectedResult, res)
        verify {
            storage.closeShift("shift-1", ShiftStatus.CLOSED, 1000L, null)
        }
    }

    @Test
    fun testSyncOfdCountersIsClosedShiftLocalOpenShiftNull() {
        every { kkmCommonHelper.requireSyncAllowed("kkm-1", "1234", true, authorizeUserUseCase, queue) } returns kkm
        every { idGenerator.nextId() } returns "req-id-1"
        val expectedResult = OfdCommandResult(
            status = OfdCommandStatus.OK,
            resultCode = 0,
            responseJson = Json.parseToJsonElement(
                "{\"payload\":{\"report\":{\"reportType\":\"REPORT_Z\",\"zxReport\":{\"shiftNumber\":5,\"cashSum\":{\"bills\":0},\"revenue\":{\"sum\":{\"bills\":0},\"isNegative\":false},\"nonNullableSums\":[]}}}}"
            ).jsonObject
        )
        every { kkmCommonHelper.sendOfdCommand(kkm, OfdCommandType.INFO, "req-id-1") } returns expectedResult
        every { clock.now() } returns 1000L
        every { storage.findOpenShift("kkm-1") } returns null
        every { storage.findKkm("kkm-1") } returns kkm

        val res = syncOfdCounters.execute("kkm-1", "1234")
        assertEquals(expectedResult, res)
        verify(exactly = 0) {
            storage.closeShift(any(), any(), any(), any())
        }
    }

    @Test
    fun testSyncOfdCountersKkmNotFoundAtEnd() {
        every { kkmCommonHelper.requireSyncAllowed("kkm-1", "1234", true, authorizeUserUseCase, queue) } returns kkm
        every { idGenerator.nextId() } returns "req-id-1"
        val expectedResult = OfdCommandResult(
            status = OfdCommandStatus.OK,
            resultCode = 0,
            responseJson = Json.parseToJsonElement(
                "{\"payload\":{\"report\":{\"reportType\":\"REPORT_X\",\"zxReport\":{\"shiftNumber\":5,\"operations\":[],\"ticketOperations\":[],\"cashSum\":{\"bills\":0},\"revenue\":{\"sum\":{\"bills\":0},\"isNegative\":false},\"nonNullableSums\":[],\"startShiftNonNullableSums\":[]}}}}"
            ).jsonObject
        )
        every { kkmCommonHelper.sendOfdCommand(kkm, OfdCommandType.INFO, "req-id-1") } returns expectedResult
        every { clock.now() } returns 1000L
        every { storage.findOpenShift("kkm-1") } returns null
        every { storage.findKkm("kkm-1") } returns null

        val res = syncOfdCounters.execute("kkm-1", "1234")
        assertEquals(expectedResult, res)
        verify(exactly = 0) {
            storage.updateKkm(any())
        }
    }

    @Test
    fun testSyncOfdCountersMissingShiftNumberFallback() {
        val kkmWithLastShiftNo = kkm.copy(lastShiftNo = 3)
        every { kkmCommonHelper.requireSyncAllowed("kkm-1", "1234", true, authorizeUserUseCase, queue) } returns kkmWithLastShiftNo
        every { idGenerator.nextId() } returns "req-id-1"
        val expectedResult = OfdCommandResult(
            status = OfdCommandStatus.OK,
            resultCode = 0,
            responseJson = Json.parseToJsonElement(
                "{\"payload\":{\"report\":{\"reportType\":\"REPORT_X\",\"zxReport\":{\"operations\":[],\"ticketOperations\":[],\"cashSum\":{\"bills\":0},\"revenue\":{\"sum\":{\"bills\":0},\"isNegative\":false},\"nonNullableSums\":[],\"startShiftNonNullableSums\":[]}}}}"
            ).jsonObject
        )
        every { kkmCommonHelper.sendOfdCommand(kkmWithLastShiftNo, OfdCommandType.INFO, "req-id-1") } returns expectedResult
        every { clock.now() } returns 1000L
        every { storage.findOpenShift("kkm-1") } returns null
        every { storage.findKkm("kkm-1") } returns kkmWithLastShiftNo

        val res = syncOfdCounters.execute("kkm-1", "1234")
        assertEquals(expectedResult, res)
        verify {
            storage.createShift(match { it.shiftNo == 3L })
        }
    }

    @Test
    fun testGetOfdAuthInfoDecodeNull() {
        every { authorizeUserUseCase.requireKkm("kkm-1") } returns kkm
        every { authorizeUserUseCase.requireRole("kkm-1", "1234", any()) } returns mockk()
        every { tokenCodec.decodeToken(any()) } returns null
        every { storage.loadCounters("kkm-1", any(), null) } returns mapOf("ofd.req_num" to 41L)

        val authInfo = getOfdAuthInfo.execute("kkm-1", "1234")
        assertEquals(null, authInfo.token)
        assertEquals(42, authInfo.nextReqNum)
    }

    @Test
    fun testSyncOfdCountersWithGlobalCountersAndTimes() {
        every { kkmCommonHelper.requireSyncAllowed("kkm-1", "1234", true, authorizeUserUseCase, queue) } returns kkm
        every { idGenerator.nextId() } returns "req-id-1"
        // Report with global counters, openTime, closeTime, etc.
        val expectedResult = OfdCommandResult(
            status = OfdCommandStatus.OK,
            resultCode = 0,
            responseJson = Json.parseToJsonElement(
                "{\"payload\":{\"report\":{\"reportType\":\"REPORT_X\",\"zxReport\":{\"shiftNumber\":5,\"openShiftTime\":{\"date\":{\"year\":2026,\"month\":6,\"day\":27},\"time\":{\"hour\":10,\"minute\":0,\"second\":0}},\"operations\":[],\"ticketOperations\":[],\"cashSum\":{\"bills\":100},\"revenue\":{\"sum\":{\"bills\":100},\"isNegative\":false},\"nonNullableSums\":[{\"operation\":\"SELL\",\"sum\":{\"bills\":500}}],\"startShiftNonNullableSums\":[]}}}}"
            ).jsonObject
        )
        every { kkmCommonHelper.sendOfdCommand(kkm, OfdCommandType.INFO, "req-id-1") } returns expectedResult
        every { clock.now() } returns 1000L
        every { storage.findOpenShift("kkm-1") } returns null
        every { storage.findKkm("kkm-1") } returns kkm

        val res = syncOfdCounters.execute("kkm-1", "1234")
        assertEquals(expectedResult, res)
        verify {
            storage.createShift(match { it.openedAt > 0L })
            storage.upsertCounter("kkm-1", "GLOBAL", null, any(), any())
        }
    }

    @Test
    fun testSyncOfdCountersWithCloseShiftTime() {
        every { kkmCommonHelper.requireSyncAllowed("kkm-1", "1234", true, authorizeUserUseCase, queue) } returns kkm
        every { idGenerator.nextId() } returns "req-id-1"
        val expectedResult = OfdCommandResult(
            status = OfdCommandStatus.OK,
            resultCode = 0,
            responseJson = Json.parseToJsonElement(
                "{\"payload\":{\"report\":{\"reportType\":\"REPORT_Z\",\"zxReport\":{\"shiftNumber\":5,\"closeShiftTime\":{\"date\":{\"year\":2026,\"month\":6,\"day\":27},\"time\":{\"hour\":12,\"minute\":0,\"second\":0}},\"cashSum\":{\"bills\":0},\"revenue\":{\"sum\":{\"bills\":0},\"isNegative\":false},\"nonNullableSums\":[]}}}}"
            ).jsonObject
        )
        every { kkmCommonHelper.sendOfdCommand(kkm, OfdCommandType.INFO, "req-id-1") } returns expectedResult
        every { clock.now() } returns 1000L
        val localOpenShift = kz.mybrain.superkassa.core.domain.model.shift.ShiftInfo(
            id = "shift-1", kkmId = "kkm-1", shiftNo = 5L, status = ShiftStatus.OPEN, openedAt = 500L, closedAt = null
        )
        every { storage.findOpenShift("kkm-1") } returns localOpenShift
        every { storage.findKkm("kkm-1") } returns kkm

        val res = syncOfdCounters.execute("kkm-1", "1234")
        assertEquals(expectedResult, res)
        verify {
            storage.closeShift("shift-1", ShiftStatus.CLOSED, match { it > 0L }, null)
        }
    }

    @Test
    fun testSyncOfdCountersAllShiftNoNull() {
        val kkmNullShift = kkm.copy(lastShiftNo = null)
        every { kkmCommonHelper.requireSyncAllowed("kkm-1", "1234", true, authorizeUserUseCase, queue) } returns kkmNullShift
        every { idGenerator.nextId() } returns "req-id-1"
        val expectedResult = OfdCommandResult(
            status = OfdCommandStatus.OK,
            resultCode = 0,
            responseJson = Json.parseToJsonElement(
                "{\"payload\":{\"report\":{\"reportType\":\"REPORT_X\",\"zxReport\":{\"operations\":[],\"ticketOperations\":[],\"cashSum\":{\"bills\":0},\"revenue\":{\"sum\":{\"bills\":0},\"isNegative\":false},\"nonNullableSums\":[],\"startShiftNonNullableSums\":[]}}}}"
            ).jsonObject
        )
        every { kkmCommonHelper.sendOfdCommand(kkmNullShift, OfdCommandType.INFO, "req-id-1") } returns expectedResult
        every { clock.now() } returns 1000L
        every { storage.findOpenShift("kkm-1") } returns null
        every { storage.findKkm("kkm-1") } returns kkmNullShift

        val res = syncOfdCounters.execute("kkm-1", "1234")
        assertEquals(expectedResult, res)
        verify {
            storage.createShift(match { it.shiftNo == 0L })
        }
    }
}

