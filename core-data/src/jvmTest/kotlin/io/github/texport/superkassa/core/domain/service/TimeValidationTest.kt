package io.github.texport.superkassa.core.domain.service

import kotlin.test.*
import io.github.texport.superkassa.core.data.adapter.security.Base64TokenCodecAdapter
import io.github.texport.superkassa.core.data.adapter.ofd.OfdConfigAdapter
import io.github.texport.superkassa.core.data.adapter.security.Sha256PinHasherAdapter
import io.github.texport.superkassa.core.domain.exception.ValidationException
import io.github.texport.superkassa.core.domain.model.common.*
import io.github.texport.superkassa.core.domain.model.ofd.*
import io.github.texport.superkassa.core.domain.port.ClockPort
import io.github.texport.superkassa.core.domain.port.IdGeneratorPort
import io.github.texport.superkassa.core.domain.port.OfdManagerPort
import io.github.texport.superkassa.core.domain.usecase.kkm.RegisterKkmUseCase
import io.github.texport.superkassa.core.support.TestStoragePort

class TimeValidationTest {

    private val storage = TestStoragePort()
    private val ofdConfigPort = OfdConfigAdapter()
    private val tokenCodec = Base64TokenCodecAdapter()
    private val pinHasher = Sha256PinHasherAdapter()

    private val ofdCommandRequestFactory = io.github.texport.superkassa.core.domain.helper.ofd.OfdCommandRequestFactory(ofdConfigPort)
    private val generateRequestNumberUseCase = io.github.texport.superkassa.core.domain.usecase.ofd.GenerateRequestNumberUseCase(storage)

    private val testClock = object : ClockPort {
        override fun now(): Long = System.currentTimeMillis()
        override fun currentYear(): Int = 2026
        override fun parseDateTimeToMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Long = 0L
    }

    private val testIdGenerator = object : IdGeneratorPort {
        override fun nextId(): String = "uuid-123"
        override fun generateFactoryNumber(prefix: String): String = "${prefix}26TEST12345"
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
        val badTimeValidator = object : io.github.texport.superkassa.core.domain.port.TimeValidatorPort {
            override fun validate(clock: ClockPort) = TimeValidationResult(false, "RANGE")
        }

        val kkmCommonHelper = io.github.texport.superkassa.core.domain.helper.KkmCommonHelper(
            storage = storage,
            clock = testClock,
            timeValidator = badTimeValidator,
            tokenCodec = tokenCodec,
            generateRequestNumberUseCase = generateRequestNumberUseCase,
            ofdCommandRequestFactory = ofdCommandRequestFactory,
            ofd = ofdManager
        )

        val initializeKkmRegistrationUseCase = io.github.texport.superkassa.core.domain.usecase.kkm.InitializeKkmRegistrationUseCase(
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
