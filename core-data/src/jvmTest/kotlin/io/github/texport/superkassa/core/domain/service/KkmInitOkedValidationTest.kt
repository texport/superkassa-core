package io.github.texport.superkassa.core.domain.service

import kotlin.test.*
import kotlinx.serialization.json.*
import io.github.texport.superkassa.core.data.impl.adapter.security.Base64TokenCodecAdapter
import io.github.texport.superkassa.core.data.impl.adapter.ofd.OfdConfigAdapter
import io.github.texport.superkassa.core.data.impl.adapter.security.Sha256PinHasherAdapter
import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.model.common.*
import io.github.texport.superkassa.core.domain.api.model.ofd.*
import io.github.texport.superkassa.core.domain.api.port.integration.ClockPort
import io.github.texport.superkassa.core.domain.api.port.integration.TimeValidatorPort
import io.github.texport.superkassa.core.domain.api.port.internal.IdGeneratorPort
import io.github.texport.superkassa.core.domain.api.port.internal.OfdManagerPort
import io.github.texport.superkassa.core.domain.impl.usecase.kkm.RegisterKkmUseCase
import io.github.texport.superkassa.core.support.TestStoragePort

class KkmInitOkedValidationTest {

    private val storage = TestStoragePort()
    private val ofdConfigPort = OfdConfigAdapter()
    private val tokenCodec = Base64TokenCodecAdapter()
    private val pinHasher = Sha256PinHasherAdapter()

    private val ofdCommandRequestFactory = io.github.texport.superkassa.core.domain.impl.helper.ofd.OfdCommandRequestFactory(ofdConfigPort)
    private val generateRequestNumberUseCase = io.github.texport.superkassa.core.domain.impl.usecase.ofd.GenerateRequestNumberUseCase(storage)

    private val testClock = object : ClockPort {
        override fun now(): Long = System.currentTimeMillis()
        override fun currentYear(): Int = 2026
        override fun parseDateTimeToMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Long = 0L
    }

    private val testIdGenerator = object : IdGeneratorPort {
        override fun nextId(): String = "uuid-123"
        override fun generateFactoryNumber(prefix: String): String = "${prefix}26TEST12345"
    }

    private var okedValFromOfd: String? = "47301"

    private val ofdManager = object : OfdManagerPort {
        override fun send(command: OfdCommandRequest): OfdCommandResult {
            val responseJson = buildJsonObject {
                put("header", buildJsonObject {
                    put("token", JsonPrimitive(12345))
                    put("reqNum", JsonPrimitive(command.reqNum))
                })
                put("payload", buildJsonObject {
                    put("result", buildJsonObject {
                        put("resultCode", JsonPrimitive(0))
                    })
                    if (command.commandType == OfdCommandType.INFO) {
                        put("service", buildJsonObject {
                            put("regInfo", buildJsonObject {
                                put("org", buildJsonObject {
                                    put("title", JsonPrimitive("Test Org"))
                                    put("address", JsonPrimitive("Test Address"))
                                    put("inn", JsonPrimitive("123456789012"))
                                    okedValFromOfd?.let { put("okved", JsonPrimitive(it)) }
                                })
                                put("kkm", buildJsonObject {
                                    put("fnsKkmId", JsonPrimitive("RN-1"))
                                    put("serialNumber", JsonPrimitive("FN-1"))
                                })
                            })
                        })
                        put("report", buildJsonObject {
                            put("zxReport", buildJsonObject {
                                put("shiftNumber", JsonPrimitive(1))
                            })
                        })
                    }
                })
            }
            return OfdCommandResult(
                status = OfdCommandStatus.OK,
                responseJson = responseJson,
                responseToken = 12345,
                responseReqNum = command.reqNum,
                resultCode = 0
            )
        }
    }

    private val testTimeValidator = object : TimeValidatorPort {
        override fun validate(clock: ClockPort) = TimeValidationResult(true)
    }

    private val kkmCommonHelper = io.github.texport.superkassa.core.domain.impl.helper.KkmCommonHelper(
        storage = storage,
        clock = testClock,
        timeValidator = testTimeValidator,
        tokenCodec = tokenCodec,
        generateRequestNumberUseCase = generateRequestNumberUseCase,
        ofdCommandRequestFactory = ofdCommandRequestFactory,
        ofd = ofdManager
    )

    private val initializeKkmRegistrationUseCase = io.github.texport.superkassa.core.domain.impl.usecase.kkm.InitializeKkmRegistrationUseCase(
        storage = storage,
        clock = testClock,
        idGenerator = testIdGenerator,
        tokenCodec = tokenCodec,
        pinHasher = pinHasher,
        kkmCommonHelper = kkmCommonHelper
    )

    private val useCase = RegisterKkmUseCase(
        storage = storage,
        ofdConfig = ofdConfigPort,
        tokenCodec = tokenCodec,
        idGenerator = testIdGenerator,
        clock = testClock,
        kkmCommonHelper = kkmCommonHelper,
        initializeKkmRegistrationUseCase = initializeKkmRegistrationUseCase
    )

    @BeforeTest
    fun setUp() {
        storage.clearAll()
        okedValFromOfd = "47301"
    }

    @Test
    fun `initKkm succeeds when oked is provided in serviceInfo`() {
        val kkm = useCase.initKkm(
            adminPin = "7391",
            ofdId = "KAZAKHTELECOM",
            ofdEnvironment = "TEST",
            ofdSystemId = "203531",
            ofdToken = "123451",
            kkmKgdId = "RN-1",
            factoryNumber = "FN-1",
            manufactureYear = 2024,
            serviceInfo = OfdServiceInfo(
                orgTitle = "Test Org",
                orgAddress = "Test Address",
                orgAddressKz = "Test Address KZ",
                orgIinOrBin = "123456789012",
                orgOked = "47301",
                geoLatitude = 1,
                geoLongitude = 1,
                geoSource = "TEST"
            ),
            oked = null
        )
        assertEquals("47301", kkm.ofdServiceInfo?.orgOked)
    }

    @Test
    fun `initKkm succeeds with oked override when serviceInfo oked is 00000`() {
        val kkm = useCase.initKkm(
            adminPin = "7391",
            ofdId = "KAZAKHTELECOM",
            ofdEnvironment = "TEST",
            ofdSystemId = "203532",
            ofdToken = "123452",
            kkmKgdId = "RN-2",
            factoryNumber = "FN-2",
            manufactureYear = 2024,
            oked = "47111", // Manual override
            serviceInfo = OfdServiceInfo(
                orgTitle = "Test Org",
                orgAddress = "Test Address",
                orgAddressKz = "Test Address KZ",
                orgIinOrBin = "123456789012",
                orgOked = "00000", // Placeholder returned by OFD
                geoLatitude = 1,
                geoLongitude = 1,
                geoSource = "TEST"
            )
        )
        assertEquals("47111", kkm.ofdServiceInfo?.orgOked)
    }

    @Test
    fun `initKkm throws ValidationException when oked is missing and no manual oked supplied`() {
        okedValFromOfd = "00000"
        val ex = assertFailsWith<ValidationException> {
            useCase.initKkm(
                adminPin = "7391",
                ofdId = "KAZAKHTELECOM",
                ofdEnvironment = "TEST",
                ofdSystemId = "203533",
                ofdToken = "123453",
                kkmKgdId = "RN-3",
                factoryNumber = "FN-3",
                manufactureYear = 2024,
                oked = null,
                serviceInfo = OfdServiceInfo(
                    orgTitle = "Test Org",
                    orgAddress = "Test Address",
                    orgAddressKz = "Test Address KZ",
                    orgIinOrBin = "123456789012",
                    orgOked = "00000", // placeholder
                    geoLatitude = 1,
                    geoLongitude = 1,
                    geoSource = "TEST"
                )
            )
        }
        assertEquals("OKED_REQUIRED", ex.code)
    }

    @Test
    fun `initKkmSimple succeeds when OFD returns valid oked`() {
        okedValFromOfd = "47301"
        val kkm = useCase.initKkmSimple(
            adminPin = "7391",
            ofdId = "KAZAKHTELECOM",
            ofdEnvironment = "TEST",
            ofdSystemId = "203534",
            ofdToken = "123454",
            defaultVatGroup = VatGroup.VAT_10,
            oked = null
        )
        assertEquals("47301", kkm.ofdServiceInfo?.orgOked)
    }

    @Test
    fun `initKkmSimple succeeds when OFD returns 00000 but manual oked is supplied`() {
        okedValFromOfd = "00000"
        val kkm = useCase.initKkmSimple(
            adminPin = "7391",
            ofdId = "KAZAKHTELECOM",
            ofdEnvironment = "TEST",
            ofdSystemId = "203535",
            ofdToken = "123455",
            defaultVatGroup = VatGroup.VAT_10,
            oked = "47111" // Manual override
        )
        assertEquals("47111", kkm.ofdServiceInfo?.orgOked)
    }

    @Test
    fun `initKkmSimple throws ValidationException when OFD returns missing oked and no manual oked supplied`() {
        okedValFromOfd = null // missing oked
        val ex = assertFailsWith<ValidationException> {
            useCase.initKkmSimple(
                adminPin = "7391",
                ofdId = "KAZAKHTELECOM",
                ofdEnvironment = "TEST",
                ofdSystemId = "203536",
                ofdToken = "123456",
                defaultVatGroup = VatGroup.VAT_10,
                oked = null
            )
        }
        assertEquals("OKED_REQUIRED", ex.code)
    }
}
