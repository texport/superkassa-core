package kz.mybrain.superkassa.core.domain.service

import kotlin.test.*
import kz.mybrain.superkassa.core.data.adapter.Base64TokenCodecAdapter
import kz.mybrain.superkassa.core.data.adapter.OfdConfigAdapter
import kz.mybrain.superkassa.core.data.adapter.Sha256PinHasherAdapter
import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.model.common.*
import kz.mybrain.superkassa.core.domain.model.ofd.*
import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.IdGeneratorPort
import kz.mybrain.superkassa.core.domain.port.OfdManagerPort
import kz.mybrain.superkassa.core.domain.usecase.kkm.RegisterKkmUseCase
import kz.mybrain.superkassa.core.support.TestStoragePort

class TimeValidationTest {

    private val storage = TestStoragePort()
    private val ofdConfigPort = OfdConfigAdapter()
    private val tokenCodec = Base64TokenCodecAdapter()
    private val pinHasher = Sha256PinHasherAdapter()

    private val ofdCommandRequestFactory = kz.mybrain.superkassa.core.domain.helper.ofd.OfdCommandRequestFactory(ofdConfigPort)
    private val generateRequestNumberUseCase = kz.mybrain.superkassa.core.domain.usecase.ofd.GenerateRequestNumberUseCase(storage)

    private val testClock = object : ClockPort {
        override fun now(): Long = System.currentTimeMillis()
        override fun currentYear(): Int = 2026
        override fun parseDateTimeToMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Long = 0L
    }

    private val testIdGenerator = object : IdGeneratorPort {
        override fun nextId(): String = "uuid-123"
        override fun generateFactoryNumber(): String = "KZT26TEST12345"
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
            override fun validate(clock: ClockPort) = TimeValidationResult(false, "RANGE")
        }

        val kkmCommonHelper = kz.mybrain.superkassa.core.domain.helper.KkmCommonHelper(
            storage = storage,
            clock = testClock,
            timeValidator = badTimeValidator,
            tokenCodec = tokenCodec,
            generateRequestNumberUseCase = generateRequestNumberUseCase,
            ofdCommandRequestFactory = ofdCommandRequestFactory,
            ofd = ofdManager
        )

        val initializeKkmRegistrationUseCase = kz.mybrain.superkassa.core.domain.usecase.kkm.InitializeKkmRegistrationUseCase(
            storage = storage,
            clock = testClock,
            idGenerator = testIdGenerator,
            tokenCodec = tokenCodec,
            pinHasher = pinHasher,
            kkmCommonHelper = kkmCommonHelper
        )

        val useCase = RegisterKkmUseCase(
            storage = storage,
            ofdConfig = ofdConfigPort,
            tokenCodec = tokenCodec,
            idGenerator = testIdGenerator,
            clock = testClock,
            kkmCommonHelper = kkmCommonHelper,
            initializeKkmRegistrationUseCase = initializeKkmRegistrationUseCase
        )

        val exception = assertFailsWith<ValidationException> {
            useCase.initKkmSimple(
                pin = "0000", // Default bootstrap admin pin
                ofdId = "KAZAKHTELECOM",
                ofdEnvironment = "TEST",
                ofdSystemId = "203534",
                ofdToken = "12345",
                defaultVatGroup = VatGroup.VAT_10,
                okved = null
            )
        }
        assertEquals("SYSTEM_TIME_INVALID", exception.code)
    }
}
