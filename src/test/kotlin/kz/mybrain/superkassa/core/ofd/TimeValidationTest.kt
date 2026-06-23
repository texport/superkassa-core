package kz.mybrain.superkassa.core.ofd

import kotlin.test.*
import kz.mybrain.superkassa.core.application.error.ValidationException
import kz.mybrain.superkassa.core.application.model.KkmInitSimpleRequest
import kz.mybrain.superkassa.core.application.policy.DefaultCounterUpdater
import kz.mybrain.superkassa.core.application.service.AuthorizationService
import kz.mybrain.superkassa.core.application.service.KkmRegistrationService
import kz.mybrain.superkassa.core.application.service.ReqNumService
import kz.mybrain.superkassa.core.data.adapter.OfdConfigPortAdapter
import kz.mybrain.superkassa.core.data.adapter.Sha256PinHasherPort
import kz.mybrain.superkassa.core.data.adapter.Base64TokenCodecPort
import kz.mybrain.superkassa.core.domain.model.*
import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.IdGenerator
import kz.mybrain.superkassa.core.domain.port.OfdManagerPort
import kz.mybrain.superkassa.core.support.TestStoragePort
import kz.mybrain.superkassa.core.application.service.OfdCommandRequestBuilder

class TimeValidationTest {

    private val storage = TestStoragePort()
    private val ofdConfigPort = OfdConfigPortAdapter()
    private val tokenCodec = Base64TokenCodecPort()
    private val pinHasher = Sha256PinHasherPort()
    private val authorization = AuthorizationService(storage, pinHasher)
    private val ofdCommandRequestBuilder = OfdCommandRequestBuilder(ofdConfigPort)
    private val reqNumService = ReqNumService(storage)

    private val testClock = object : ClockPort {
        override fun now(): Long = System.currentTimeMillis()
    }

    private val testIdGenerator = object : IdGenerator {
        override fun nextId(): String = "uuid-123"
    }

    private val ofdManager = object : OfdManagerPort {
        override fun send(command: OfdCommandRequest): OfdCommandResult {
            return OfdCommandResult(
                status = OfdCommandStatus.OK,
                responseJson = kotlinx.serialization.json.buildJsonObject {},
                responseToken = 12345,
                responseReqNum = command.reqNum,
                resultCode = 0
            )
        }
    }

    @Test
    fun `initKkmSimple throws SYSTEM_TIME_INVALID when system time is invalid`() {
        val badTimeValidator = object : kz.mybrain.superkassa.core.domain.port.TimeValidatorPort {
            override fun validate(clock: ClockPort) = kz.mybrain.superkassa.core.domain.port.TimeValidationResult(false, "RANGE")
        }

        val service = KkmRegistrationService(
            storage = storage,
            ofd = ofdManager,
            ofdConfig = ofdConfigPort,
            tokenCodec = tokenCodec,
            idGenerator = testIdGenerator,
            clock = testClock,
            pinHasher = pinHasher,
            authorization = authorization,
            ofdCommandRequestBuilder = ofdCommandRequestBuilder,
            reqNumService = reqNumService,
            counters = DefaultCounterUpdater(storage),
            timeValidator = badTimeValidator
        )

        val request = KkmInitSimpleRequest(
            ofdId = "KAZAKHTELECOM",
            ofdEnvironment = "TEST",
            ofdSystemId = "203534",
            ofdToken = "12345"
        )

        val exception = assertFailsWith<ValidationException> {
            service.initKkmSimple("1234", request)
        }
        assertEquals("SYSTEM_TIME_INVALID", exception.code)
    }
}
