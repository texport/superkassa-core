package kz.mybrain.superkassa.core.domain.service

import kotlin.test.*
import kotlinx.serialization.json.*
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

class KkmInitOkvedValidationTest {

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

    private var okvedValFromOfd: String? = "47301"

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
                                    okvedValFromOfd?.let { put("okved", JsonPrimitive(it)) }
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

    private val testTimeValidator = object : kz.mybrain.superkassa.core.domain.port.TimeValidatorPort {
        override fun validate(clock: ClockPort) = TimeValidationResult(true)
    }

    private val kkmCommonHelper = kz.mybrain.superkassa.core.domain.helper.KkmCommonHelper(
        storage = storage,
        clock = testClock,
        timeValidator = testTimeValidator,
        tokenCodec = tokenCodec,
        generateRequestNumberUseCase = generateRequestNumberUseCase,
        ofdCommandRequestFactory = ofdCommandRequestFactory,
        ofd = ofdManager
    )

    private val initializeKkmRegistrationUseCase = kz.mybrain.superkassa.core.domain.usecase.kkm.InitializeKkmRegistrationUseCase(
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
        okvedValFromOfd = "47301"
    }

    @Test
    fun `initKkm succeeds when okved is provided in serviceInfo`() {
        val kkm = useCase.initKkm(
            pin = "0000",
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
                orgInn = "123456789012",
                orgOkved = "47301",
                geoLatitude = 1,
                geoLongitude = 1,
                geoSource = "TEST"
            ),
            okved = null
        )
        assertEquals("47301", kkm.ofdServiceInfo?.orgOkved)
    }

    @Test
    fun `initKkm succeeds with okved override when serviceInfo okved is 00000`() {
        val kkm = useCase.initKkm(
            pin = "0000",
            ofdId = "KAZAKHTELECOM",
            ofdEnvironment = "TEST",
            ofdSystemId = "203532",
            ofdToken = "123452",
            kkmKgdId = "RN-2",
            factoryNumber = "FN-2",
            manufactureYear = 2024,
            okved = "47111", // Manual override
            serviceInfo = OfdServiceInfo(
                orgTitle = "Test Org",
                orgAddress = "Test Address",
                orgAddressKz = "Test Address KZ",
                orgInn = "123456789012",
                orgOkved = "00000", // Placeholder returned by OFD
                geoLatitude = 1,
                geoLongitude = 1,
                geoSource = "TEST"
            )
        )
        assertEquals("47111", kkm.ofdServiceInfo?.orgOkved)
    }

    @Test
    fun `initKkm throws ValidationException when okved is missing and no manual okved supplied`() {
        okvedValFromOfd = "00000"
        val ex = assertFailsWith<ValidationException> {
            useCase.initKkm(
                pin = "0000",
                ofdId = "KAZAKHTELECOM",
                ofdEnvironment = "TEST",
                ofdSystemId = "203533",
                ofdToken = "123453",
                kkmKgdId = "RN-3",
                factoryNumber = "FN-3",
                manufactureYear = 2024,
                okved = null,
                serviceInfo = OfdServiceInfo(
                    orgTitle = "Test Org",
                    orgAddress = "Test Address",
                    orgAddressKz = "Test Address KZ",
                    orgInn = "123456789012",
                    orgOkved = "00000", // placeholder
                    geoLatitude = 1,
                    geoLongitude = 1,
                    geoSource = "TEST"
                )
            )
        }
        assertEquals("OKVED_REQUIRED", ex.code)
    }

    @Test
    fun `initKkmSimple succeeds when OFD returns valid okved`() {
        okvedValFromOfd = "47301"
        val kkm = useCase.initKkmSimple(
            pin = "0000",
            ofdId = "KAZAKHTELECOM",
            ofdEnvironment = "TEST",
            ofdSystemId = "203534",
            ofdToken = "123454",
            defaultVatGroup = VatGroup.VAT_10,
            okved = null
        )
        assertEquals("47301", kkm.ofdServiceInfo?.orgOkved)
    }

    @Test
    fun `initKkmSimple succeeds when OFD returns 00000 but manual okved is supplied`() {
        okvedValFromOfd = "00000"
        val kkm = useCase.initKkmSimple(
            pin = "0000",
            ofdId = "KAZAKHTELECOM",
            ofdEnvironment = "TEST",
            ofdSystemId = "203535",
            ofdToken = "123455",
            defaultVatGroup = VatGroup.VAT_10,
            okved = "47111" // Manual override
        )
        assertEquals("47111", kkm.ofdServiceInfo?.orgOkved)
    }

    @Test
    fun `initKkmSimple throws ValidationException when OFD returns missing okved and no manual okved supplied`() {
        okvedValFromOfd = null // missing okved
        val ex = assertFailsWith<ValidationException> {
            useCase.initKkmSimple(
                pin = "0000",
                ofdId = "KAZAKHTELECOM",
                ofdEnvironment = "TEST",
                ofdSystemId = "203536",
                ofdToken = "123456",
                defaultVatGroup = VatGroup.VAT_10,
                okved = null
            )
        }
        assertEquals("OKVED_REQUIRED", ex.code)
    }
}
