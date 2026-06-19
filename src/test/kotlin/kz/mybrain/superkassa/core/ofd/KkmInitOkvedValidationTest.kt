package kz.mybrain.superkassa.core.ofd

import kotlin.test.*
import kotlinx.serialization.json.*
import kz.mybrain.superkassa.core.application.error.ValidationException
import kz.mybrain.superkassa.core.application.model.KkmInitDirectRequest
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

class KkmInitOkvedValidationTest {

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
        override fun validate(clock: ClockPort) = kz.mybrain.superkassa.core.domain.port.TimeValidationResult(true)
    }

    private val service = KkmRegistrationService(
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
        timeValidator = testTimeValidator
    )

    @BeforeTest
    fun setUp() {
        storage.clearAll()
        okvedValFromOfd = "47301"
    }

    @Test
    fun `initKkm succeeds when okved is provided in serviceInfo`() {
        val request = KkmInitDirectRequest(
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
            )
        )
        val kkm = service.initKkm("0000", request)
        assertEquals("47301", kkm.ofdServiceInfo?.orgOkved)
    }

    @Test
    fun `initKkm succeeds with okved override when serviceInfo okved is 00000`() {
        val request = KkmInitDirectRequest(
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
        val kkm = service.initKkm("0000", request)
        assertEquals("47111", kkm.ofdServiceInfo?.orgOkved)
    }

    @Test
    fun `initKkm throws ValidationException when okved is missing and no manual okved supplied`() {
        okvedValFromOfd = "00000"
        val request = KkmInitDirectRequest(
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
        val ex = assertFailsWith<ValidationException> {
            service.initKkm("0000", request)
        }
        assertEquals("OKVED_REQUIRED", ex.code)
    }

    @Test
    fun `initKkmSimple succeeds when OFD returns valid okved`() {
        okvedValFromOfd = "47301"
        val request = KkmInitSimpleRequest(
            ofdId = "KAZAKHTELECOM",
            ofdEnvironment = "TEST",
            ofdSystemId = "203534",
            ofdToken = "123454"
        )
        val kkm = service.initKkmSimple("0000", request)
        assertEquals("47301", kkm.ofdServiceInfo?.orgOkved)
    }

    @Test
    fun `initKkmSimple succeeds when OFD returns 00000 but manual okved is supplied`() {
        okvedValFromOfd = "00000"
        val request = KkmInitSimpleRequest(
            ofdId = "KAZAKHTELECOM",
            ofdEnvironment = "TEST",
            ofdSystemId = "203535",
            ofdToken = "123455",
            okved = "47111" // Manual override
        )
        val kkm = service.initKkmSimple("0000", request)
        assertEquals("47111", kkm.ofdServiceInfo?.orgOkved)
    }

    @Test
    fun `initKkmSimple throws ValidationException when OFD returns missing okved and no manual okved supplied`() {
        okvedValFromOfd = null // missing okved
        val request = KkmInitSimpleRequest(
            ofdId = "KAZAKHTELECOM",
            ofdEnvironment = "TEST",
            ofdSystemId = "203536",
            ofdToken = "123456",
            okved = null
        )
        val ex = assertFailsWith<ValidationException> {
            service.initKkmSimple("0000", request)
        }
        assertEquals("OKVED_REQUIRED", ex.code)
    }
}
