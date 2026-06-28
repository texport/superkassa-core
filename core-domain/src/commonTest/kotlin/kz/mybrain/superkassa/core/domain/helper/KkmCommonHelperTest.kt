package kz.mybrain.superkassa.core.domain.helper

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.slot
import kz.mybrain.superkassa.core.domain.exception.ConflictException
import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.model.auth.UserRole
import kz.mybrain.superkassa.core.domain.model.common.TimeValidationResult
import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kz.mybrain.superkassa.core.domain.model.ofd.*
import kz.mybrain.superkassa.core.domain.model.shift.ShiftInfo
import kz.mybrain.superkassa.core.domain.port.*
import kz.mybrain.superkassa.core.domain.usecase.auth.AuthorizeUserUseCase
import kz.mybrain.superkassa.core.domain.usecase.ofd.GenerateRequestNumberUseCase
import kz.mybrain.superkassa.core.domain.helper.ofd.OfdCommandRequestFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Тесты для [KkmCommonHelper] на русском языке без предупреждений статического анализа.
 */
class KkmCommonHelperTest {

    private val storage = mockk<StoragePort>()
    private val clock = mockk<ClockPort>()
    private val timeValidator = mockk<TimeValidatorPort>()
    private val tokenCodec = mockk<TokenCodecPort>()
    private val generateRequestNumberUseCase = mockk<GenerateRequestNumberUseCase>()
    private val ofdCommandRequestFactory = mockk<OfdCommandRequestFactory>()
    private val ofd = mockk<OfdManagerPort>()

    private val helper = KkmCommonHelper(
        storage,
        clock,
        timeValidator,
        tokenCodec,
        generateRequestNumberUseCase,
        ofdCommandRequestFactory,
        ofd
    )

    @Test
    fun testEnsureSystemTimeValidSuccess() {
        every { timeValidator.validate(clock) } returns TimeValidationResult(ok = true)
        helper.ensureSystemTimeValid()
        verify(exactly = 1) { timeValidator.validate(clock) }
    }

    @Test
    fun testEnsureSystemTimeValidFailure() {
        every { timeValidator.validate(clock) } returns TimeValidationResult(ok = false)
        assertFailsWith<ValidationException> {
            helper.ensureSystemTimeValid()
        }
    }

    @Test
    fun testDefaultServiceInfoReturnsTemplate() {
        val info = helper.defaultServiceInfo()
        assertEquals("UNKNOWN", info.orgTitle)
        assertEquals("000000000000", info.orgInn)
    }

    @Test
    fun testSendOfdCommandSuccessAndUpdatesToken() {
        val kkm = KkmInfo(id = "kkm-1", createdAt = 0L, updatedAt = 0L, mode = "ACTIVE", state = "ACTIVE", tokenEncryptedBase64 = "encrypted")
        val resultToken = 9999L
        val encodedToken = "encoded-next"
        val request = mockk<OfdCommandRequest>()
        val result = OfdCommandResult(
            status = OfdCommandStatus.OK,
            responseToken = resultToken
        )

        every { tokenCodec.decodeToken("encrypted") } returns 1234L
        every { generateRequestNumberUseCase.execute("kkm-1") } returns 77
        every { clock.now() } returns 1000L
        every {
            ofdCommandRequestFactory.build(
                kkm = kkm,
                commandType = OfdCommandType.TICKET,
                payloadRef = "payload",
                token = 1234L,
                reqNum = 77,
                now = 1000L,
                serviceInfoOverride = null,
                registrationNumberOverride = null,
                factoryNumberOverride = null,
                ofdProviderOverride = null,
                defaultServiceInfo = any()
            )
        } returns request

        every { ofd.send(request) } returns result
        every { tokenCodec.encodeToken(resultToken) } returns encodedToken
        every { storage.updateKkmToken("kkm-1", encodedToken, 1000L) } returns true

        val res = helper.sendOfdCommand(kkm, OfdCommandType.TICKET, "payload")
        assertEquals(result, res)

        verify(exactly = 1) { storage.updateKkmToken("kkm-1", encodedToken, 1000L) }
    }

    @Test
    fun testSendOfdCommandSuccessNoTokenUpdate() {
        val kkm = KkmInfo(id = "kkm-1", createdAt = 0L, updatedAt = 0L, mode = "ACTIVE", state = "ACTIVE", tokenEncryptedBase64 = "encrypted")
        val request = mockk<OfdCommandRequest>()
        val result = OfdCommandResult(
            status = OfdCommandStatus.OK,
            responseToken = 9999L
        )

        every { tokenCodec.decodeToken("encrypted") } returns 1234L
        every { generateRequestNumberUseCase.execute("kkm-1") } returns 77
        every { clock.now() } returns 1000L
        every {
            ofdCommandRequestFactory.build(
                kkm = kkm,
                commandType = OfdCommandType.TICKET,
                payloadRef = "payload",
                token = 1234L,
                reqNum = 77,
                now = 1000L,
                serviceInfoOverride = null,
                registrationNumberOverride = null,
                factoryNumberOverride = null,
                ofdProviderOverride = null,
                defaultServiceInfo = any()
            )
        } returns request

        every { ofd.send(request) } returns result

        val res = helper.sendOfdCommand(kkm, OfdCommandType.TICKET, "payload", updateToken = false)
        assertEquals(result, res)

        verify(exactly = 0) { storage.updateKkmToken(any(), any(), any()) }
    }

    @Test
    fun testSendOfdCommandTokenRequiredException() {
        val kkm = KkmInfo(id = "kkm-1", createdAt = 0L, updatedAt = 0L, mode = "ACTIVE", state = "ACTIVE", tokenEncryptedBase64 = "encrypted")
        every { tokenCodec.decodeToken("encrypted") } returns null

        assertFailsWith<ValidationException> {
            helper.sendOfdCommand(kkm, OfdCommandType.TICKET, "payload")
        }
    }

    @Test
    fun testRequireSyncAllowedSuccess() {
        val kkm = KkmInfo(id = "kkm-1", createdAt = 0L, updatedAt = 0L, mode = "ACTIVE", state = "ACTIVE")
        val authorization = mockk<AuthorizeUserUseCase>()
        val queue = mockk<OfflineQueuePort>()

        every { timeValidator.validate(clock) } returns TimeValidationResult(ok = true)
        every { authorization.requireRole("kkm-1", "1234", setOf(UserRole.ADMIN)) } returns Unit
        
        val transactionSlot = slot<() -> KkmInfo>()
        every { storage.inTransaction(capture(transactionSlot)) } answers {
            transactionSlot.captured.invoke()
        }
        every { authorization.requireKkm("kkm-1") } returns kkm
        every { storage.findOpenShift("kkm-1") } returns null
        every { queue.canSendDirectly("kkm-1") } returns true

        val res = helper.requireSyncAllowed("kkm-1", "1234", false, authorization, queue)
        assertEquals(kkm, res)
    }

    @Test
    fun testRequireSyncAllowedShiftOpenException() {
        val kkm = KkmInfo(id = "kkm-1", createdAt = 0L, updatedAt = 0L, mode = "ACTIVE", state = "ACTIVE")
        val authorization = mockk<AuthorizeUserUseCase>()
        val queue = mockk<OfflineQueuePort>()

        every { timeValidator.validate(clock) } returns TimeValidationResult(ok = true)
        every { authorization.requireRole("kkm-1", "1234", setOf(UserRole.ADMIN)) } returns Unit
        
        val transactionSlot = slot<() -> KkmInfo>()
        every { storage.inTransaction(capture(transactionSlot)) } answers {
            transactionSlot.captured.invoke()
        }
        every { authorization.requireKkm("kkm-1") } returns kkm
        every { storage.findOpenShift("kkm-1") } returns mockk<ShiftInfo>()

        assertFailsWith<ConflictException> {
            helper.requireSyncAllowed("kkm-1", "1234", false, authorization, queue)
        }
    }

    @Test
    fun testRequireSyncAllowedQueueNotEmptyException() {
        val kkm = KkmInfo(id = "kkm-1", createdAt = 0L, updatedAt = 0L, mode = "ACTIVE", state = "ACTIVE")
        val authorization = mockk<AuthorizeUserUseCase>()
        val queue = mockk<OfflineQueuePort>()

        every { timeValidator.validate(clock) } returns TimeValidationResult(ok = true)
        every { authorization.requireRole("kkm-1", "1234", setOf(UserRole.ADMIN)) } returns Unit
        
        val transactionSlot = slot<() -> KkmInfo>()
        every { storage.inTransaction(capture(transactionSlot)) } answers {
            transactionSlot.captured.invoke()
        }
        every { authorization.requireKkm("kkm-1") } returns kkm
        every { storage.findOpenShift("kkm-1") } returns null
        every { queue.canSendDirectly("kkm-1") } returns false

        assertFailsWith<ConflictException> {
            helper.requireSyncAllowed("kkm-1", "1234", false, authorization, queue)
        }
    }
}
