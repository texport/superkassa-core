package kz.mybrain.superkassa.core.domain.usecase.kkm

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kz.mybrain.superkassa.core.domain.exception.ConflictException
import kz.mybrain.superkassa.core.domain.exception.ForbiddenException
import kz.mybrain.superkassa.core.domain.exception.NotFoundException
import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.helper.KkmCommonHelper
import kz.mybrain.superkassa.core.domain.model.auth.UserRole
import kz.mybrain.superkassa.core.domain.model.common.TaxRegime
import kz.mybrain.superkassa.core.domain.model.common.CounterKeyFormats
import kz.mybrain.superkassa.core.domain.model.common.VatGroup
import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kz.mybrain.superkassa.core.domain.model.kkm.KkmMode
import kz.mybrain.superkassa.core.domain.model.kkm.KkmState
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandResult
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandStatus
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandType
import kz.mybrain.superkassa.core.domain.model.ofd.OfdServiceInfo
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptBranding
import kz.mybrain.superkassa.core.domain.port.ClockPort
import kz.mybrain.superkassa.core.domain.port.IdGeneratorPort
import kz.mybrain.superkassa.core.domain.port.OfdConfigPort
import kz.mybrain.superkassa.core.domain.port.StoragePort
import kz.mybrain.superkassa.core.domain.port.TokenCodecPort
import kz.mybrain.superkassa.core.domain.port.OfflineQueuePort
import kz.mybrain.superkassa.core.domain.port.PinHasherPort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class KkmUseCasesTest {

    private val storage = mockk<StoragePort>(relaxed = true)
    private val ofdConfig = mockk<OfdConfigPort>(relaxed = true)
    private val tokenCodec = mockk<TokenCodecPort>(relaxed = true)
    private val idGenerator = mockk<IdGeneratorPort>(relaxed = true)
    private val clock = mockk<ClockPort>(relaxed = true)
    private val queue = mockk<OfflineQueuePort>(relaxed = true)
    private val pinHasher = mockk<PinHasherPort>(relaxed = true)
    private val kkmCommonHelper = mockk<KkmCommonHelper>(relaxed = true)

    private val initializeKkmRegistration = InitializeKkmRegistrationUseCase(
        storage, clock, idGenerator, tokenCodec, pinHasher, kkmCommonHelper
    )
    private val registerKkm = RegisterKkmUseCase(
        storage, ofdConfig, tokenCodec, idGenerator, clock, kkmCommonHelper, initializeKkmRegistration
    )
    private val decommissionKkm = DecommissionKkmUseCase(storage, queue)
    private val enterProgramming = EnterProgrammingUseCase(storage, clock)
    private val exitProgramming = ExitProgrammingUseCase(storage, clock)
    private val updateKkmSettings = UpdateKkmSettingsUseCase(storage, queue, clock)
    private val enforceAutonomousLimits = EnforceAutonomousLimitsUseCase(storage, queue, clock)

    private val kkm = KkmInfo(
        id = "kkm-1",
        createdAt = 0,
        updatedAt = 0,
        mode = KkmMode.PROGRAMMING.name,
        state = KkmState.PROGRAMMING.name,
        registrationNumber = "reg-1"
    )

    init {
        every { storage.inTransaction<Any?>(any()) } answers {
            val block = firstArg<() -> Any?>()
            block()
        }
        every { storage.findKkmByRegistrationNumber(any()) } returns null
        every { storage.findKkmBySystemId(any()) } returns null
    }

    // ==========================================
    // RegisterKkmUseCase & InitializeKkmRegistrationUseCase Tests
    // ==========================================

    @Test
    fun testInitKkmForbiddenPin() {
        assertFailsWith<ForbiddenException> {
            registerKkm.initKkm(
                pin = "1111",
                ofdId = "ofd",
                ofdEnvironment = "prod",
                ofdSystemId = "sys-1",
                ofdToken = "token",
                kkmKgdId = "kgd-1",
                factoryNumber = "fact-1",
                manufactureYear = 2026,
                serviceInfo = null,
                okved = "12345"
            )
        }
    }

    @Test
    fun testInitKkmBlankSystemId() {
        every { kkmCommonHelper.ensureSystemTimeValid() } returns Unit
        assertFailsWith<ValidationException> {
            registerKkm.initKkm(
                pin = "0000",
                ofdId = "ofd",
                ofdEnvironment = "prod",
                ofdSystemId = "",
                ofdToken = "token",
                kkmKgdId = "kgd-1",
                factoryNumber = "fact-1",
                manufactureYear = 2026,
                serviceInfo = null,
                okved = "12345"
            )
        }
    }

    @Test
    fun testInitKkmExistingRegNum() {
        every { kkmCommonHelper.ensureSystemTimeValid() } returns Unit
        every { ofdConfig.validateAndFormatTag("ofd", "prod") } returns "ofd-tag"
        every { clock.now() } returns 1000L
        every { storage.findKkmByRegistrationNumber("kgd-1") } returns kkm

        assertFailsWith<ConflictException> {
            registerKkm.initKkm(
                pin = "0000",
                ofdId = "ofd",
                ofdEnvironment = "prod",
                ofdSystemId = "sys-1",
                ofdToken = "token",
                kkmKgdId = "kgd-1",
                factoryNumber = "fact-1",
                manufactureYear = 2026,
                serviceInfo = null,
                okved = "12345"
            )
        }
    }

    @Test
    fun testInitKkmExistingSystemId() {
        every { kkmCommonHelper.ensureSystemTimeValid() } returns Unit
        every { ofdConfig.validateAndFormatTag("ofd", "prod") } returns "ofd-tag"
        every { clock.now() } returns 1000L
        every { storage.findKkmByRegistrationNumber("kgd-1") } returns null
        every { storage.findKkmBySystemId("sys-1") } returns kkm

        assertFailsWith<ConflictException> {
            registerKkm.initKkm(
                pin = "0000",
                ofdId = "ofd",
                ofdEnvironment = "prod",
                ofdSystemId = "sys-1",
                ofdToken = "token",
                kkmKgdId = "kgd-1",
                factoryNumber = "fact-1",
                manufactureYear = 2026,
                serviceInfo = null,
                okved = "12345"
            )
        }
    }

    @Test
    fun testInitKkmInvalidOkved() {
        every { kkmCommonHelper.ensureSystemTimeValid() } returns Unit
        every { ofdConfig.validateAndFormatTag("ofd", "prod") } returns "ofd-tag"
        every { clock.now() } returns 1000L
        every { storage.findKkmByRegistrationNumber("kgd-1") } returns null
        every { storage.findKkmBySystemId("sys-1") } returns null

        assertFailsWith<ValidationException> {
            registerKkm.initKkm(
                pin = "0000",
                ofdId = "ofd",
                ofdEnvironment = "prod",
                ofdSystemId = "sys-1",
                ofdToken = "token",
                kkmKgdId = "kgd-1",
                factoryNumber = "fact-1",
                manufactureYear = 2026,
                serviceInfo = null,
                okved = ""
            )
        }

        assertFailsWith<ValidationException> {
            registerKkm.initKkm(
                pin = "0000",
                ofdId = "ofd",
                ofdEnvironment = "prod",
                ofdSystemId = "sys-1",
                ofdToken = "token",
                kkmKgdId = "kgd-1",
                factoryNumber = "fact-1",
                manufactureYear = 2026,
                serviceInfo = null,
                okved = "00000"
            )
        }
    }

    @Test
    fun testInitKkmSuccessDefaultServiceInfo() {
        every { kkmCommonHelper.ensureSystemTimeValid() } returns Unit
        every { ofdConfig.validateAndFormatTag("ofd", "prod") } returns "ofd-tag"
        every { clock.now() } returns 1000L
        every { storage.findKkmByRegistrationNumber("kgd-1") } returns null
        every { storage.findKkmBySystemId("sys-1") } returns null
        every { idGenerator.nextId() } returns "kkm-id-123"
        every { tokenCodec.parseToken("token") } returns 555L
        every { tokenCodec.encodeToken(555L) } returns "enc-555"

        val defaultInfo = OfdServiceInfo(
            orgTitle = "UNKNOWN",
            orgAddress = "UNKNOWN",
            orgAddressKz = "UNKNOWN",
            orgInn = "000000000000",
            orgOkved = "12345",
            geoLatitude = 0,
            geoLongitude = 0,
            geoSource = "UNKNOWN"
        )
        every { kkmCommonHelper.defaultServiceInfo() } returns defaultInfo

        val systemResult = OfdCommandResult(status = OfdCommandStatus.OK, responseToken = 555L)
        val infoResult = OfdCommandResult(status = OfdCommandStatus.OK, responseToken = 555L)
        every { kkmCommonHelper.sendOfdCommand(any(), OfdCommandType.SYSTEM, any(), 555L, defaultInfo, "kgd-1", "fact-1", "ofd-tag") } returns systemResult
        every { kkmCommonHelper.sendOfdCommand(any(), OfdCommandType.INFO, any(), 555L, defaultInfo, "kgd-1", "fact-1", "ofd-tag") } returns infoResult

        every { storage.createKkm(any()) } returns true
        every { storage.listUsers("kkm-id-123") } returns emptyList()

        val res = registerKkm.initKkm(
            pin = "0000",
            ofdId = "ofd",
            ofdEnvironment = "prod",
            ofdSystemId = "sys-1",
            ofdToken = "token",
            kkmKgdId = "kgd-1",
            factoryNumber = "fact-1",
            manufactureYear = 2026,
            serviceInfo = null,
            okved = "12345"
        )

        assertEquals("kkm-id-123", res.id)
        assertEquals("ofd-tag", res.ofdProvider)
        assertEquals("kgd-1", res.registrationNumber)
        verify { storage.createKkm(match { it.id == "kkm-id-123" }) }
    }

    @Test
    fun testInitKkmStorageCreateFail() {
        every { kkmCommonHelper.ensureSystemTimeValid() } returns Unit
        every { ofdConfig.validateAndFormatTag("ofd", "prod") } returns "ofd-tag"
        every { clock.now() } returns 1000L
        every { storage.findKkmByRegistrationNumber("kgd-1") } returns null
        every { storage.findKkmBySystemId("sys-1") } returns null
        every { idGenerator.nextId() } returns "kkm-id-123"
        every { tokenCodec.parseToken("token") } returns 555L
        every { tokenCodec.encodeToken(555L) } returns "enc-555"

        val defaultInfo = OfdServiceInfo(
            orgTitle = "UNKNOWN",
            orgAddress = "UNKNOWN",
            orgAddressKz = "UNKNOWN",
            orgInn = "000000000000",
            orgOkved = "12345",
            geoLatitude = 0,
            geoLongitude = 0,
            geoSource = "UNKNOWN"
        )
        every { kkmCommonHelper.defaultServiceInfo() } returns defaultInfo

        val systemResult = OfdCommandResult(status = OfdCommandStatus.OK, responseToken = 555L)
        val infoResult = OfdCommandResult(status = OfdCommandStatus.OK, responseToken = 555L)
        every { kkmCommonHelper.sendOfdCommand(any(), OfdCommandType.SYSTEM, any(), 555L, defaultInfo, "kgd-1", "fact-1", "ofd-tag") } returns systemResult
        every { kkmCommonHelper.sendOfdCommand(any(), OfdCommandType.INFO, any(), 555L, defaultInfo, "kgd-1", "fact-1", "ofd-tag") } returns infoResult

        every { storage.createKkm(any()) } returns false

        assertFailsWith<ConflictException> {
            registerKkm.initKkm(
                pin = "0000",
                ofdId = "ofd",
                ofdEnvironment = "prod",
                ofdSystemId = "sys-1",
                ofdToken = "token",
                kkmKgdId = "kgd-1",
                factoryNumber = "fact-1",
                manufactureYear = 2026,
                serviceInfo = null,
                okved = "12345"
            )
        }
    }

    @Test
    fun testInitKkmSimpleForbiddenPin() {
        assertFailsWith<ForbiddenException> {
            registerKkm.initKkmSimple(
                pin = "1111",
                ofdId = "ofd",
                ofdEnvironment = "prod",
                ofdSystemId = "sys-1",
                ofdToken = "token",
                defaultVatGroup = VatGroup.VAT_10,
                okved = "12345"
            )
        }
    }

    @Test
    fun testInitKkmSimpleBlankSystemId() {
        every { kkmCommonHelper.ensureSystemTimeValid() } returns Unit
        assertFailsWith<ValidationException> {
            registerKkm.initKkmSimple(
                pin = "0000",
                ofdId = "ofd",
                ofdEnvironment = "prod",
                ofdSystemId = "",
                ofdToken = "token",
                defaultVatGroup = VatGroup.VAT_10,
                okved = "12345"
            )
        }
    }

    @Test
    fun testInitKkmSimpleExistingSystemId() {
        every { kkmCommonHelper.ensureSystemTimeValid() } returns Unit
        every { ofdConfig.validateAndFormatTag("ofd", "prod") } returns "ofd-tag"
        every { clock.now() } returns 1000L
        every { storage.findKkmBySystemId("sys-1") } returns kkm

        assertFailsWith<ConflictException> {
            registerKkm.initKkmSimple(
                pin = "0000",
                ofdId = "ofd",
                ofdEnvironment = "prod",
                ofdSystemId = "sys-1",
                ofdToken = "token",
                defaultVatGroup = VatGroup.VAT_10,
                okved = "12345"
            )
        }
    }

    @Test
    fun testInitKkmSimpleSystemCommandFail() {
        every { kkmCommonHelper.ensureSystemTimeValid() } returns Unit
        every { ofdConfig.validateAndFormatTag("ofd", "prod") } returns "ofd-tag"
        every { clock.now() } returns 1000L
        every { storage.findKkmBySystemId("sys-1") } returns null
        every { idGenerator.nextId() } returns "kkm-id-123"
        every { tokenCodec.parseToken("token") } returns 555L

        val defaultInfo = OfdServiceInfo(
            orgTitle = "UNKNOWN",
            orgAddress = "UNKNOWN",
            orgAddressKz = "UNKNOWN",
            orgInn = "000000000000",
            orgOkved = "12345",
            geoLatitude = 0,
            geoLongitude = 0,
            geoSource = "UNKNOWN"
        )
        every { kkmCommonHelper.defaultServiceInfo() } returns defaultInfo

        val systemResult = OfdCommandResult(status = OfdCommandStatus.FAILED, errorMessage = "System connection error")
        every { kkmCommonHelper.sendOfdCommand(any(), OfdCommandType.SYSTEM, any(), 555L, defaultInfo, "TEMP_REG_kkm-id-1", "TEMP_FACTORY_kkm-id-1", "ofd-tag") } returns systemResult

        assertFailsWith<ValidationException> {
            registerKkm.initKkmSimple(
                pin = "0000",
                ofdId = "ofd",
                ofdEnvironment = "prod",
                ofdSystemId = "sys-1",
                ofdToken = "token",
                defaultVatGroup = VatGroup.VAT_10,
                okved = "12345"
            )
        }
    }

    @Test
    fun testInitKkmSimpleInfoCommandFail() {
        every { kkmCommonHelper.ensureSystemTimeValid() } returns Unit
        every { ofdConfig.validateAndFormatTag("ofd", "prod") } returns "ofd-tag"
        every { clock.now() } returns 1000L
        every { storage.findKkmBySystemId("sys-1") } returns null
        every { idGenerator.nextId() } returns "kkm-id-123"
        every { tokenCodec.parseToken("token") } returns 555L

        val defaultInfo = OfdServiceInfo(
            orgTitle = "UNKNOWN",
            orgAddress = "UNKNOWN",
            orgAddressKz = "UNKNOWN",
            orgInn = "000000000000",
            orgOkved = "12345",
            geoLatitude = 0,
            geoLongitude = 0,
            geoSource = "UNKNOWN"
        )
        every { kkmCommonHelper.defaultServiceInfo() } returns defaultInfo

        val systemResult = OfdCommandResult(status = OfdCommandStatus.OK, responseToken = 777L)
        val infoResult = OfdCommandResult(status = OfdCommandStatus.FAILED, errorMessage = "Info parse error")

        every { kkmCommonHelper.sendOfdCommand(any(), OfdCommandType.SYSTEM, any(), 555L, defaultInfo, "TEMP_REG_kkm-id-1", "TEMP_FACTORY_kkm-id-1", "ofd-tag") } returns systemResult
        every { kkmCommonHelper.sendOfdCommand(any(), OfdCommandType.INFO, any(), 777L, defaultInfo, "TEMP_REG_kkm-id-1", "TEMP_FACTORY_kkm-id-1", "ofd-tag") } returns infoResult

        assertFailsWith<ValidationException> {
            registerKkm.initKkmSimple(
                pin = "0000",
                ofdId = "ofd",
                ofdEnvironment = "prod",
                ofdSystemId = "sys-1",
                ofdToken = "token",
                defaultVatGroup = VatGroup.VAT_10,
                okved = "12345"
            )
        }
    }

    @Test
    fun testInitKkmSimpleConflictRegNum() {
        every { kkmCommonHelper.ensureSystemTimeValid() } returns Unit
        every { ofdConfig.validateAndFormatTag("ofd", "prod") } returns "ofd-tag"
        every { clock.now() } returns 1000L
        every { storage.findKkmBySystemId("sys-1") } returns null
        every { idGenerator.nextId() } returns "kkm-id-123"
        every { tokenCodec.parseToken("token") } returns 555L

        val defaultInfo = OfdServiceInfo(
            orgTitle = "UNKNOWN",
            orgAddress = "UNKNOWN",
            orgAddressKz = "UNKNOWN",
            orgInn = "000000000000",
            orgOkved = "12345",
            geoLatitude = 0,
            geoLongitude = 0,
            geoSource = "UNKNOWN"
        )
        every { kkmCommonHelper.defaultServiceInfo() } returns defaultInfo

        val systemResult = OfdCommandResult(status = OfdCommandStatus.OK, responseToken = 777L)
        val responseJson = buildJsonObject {
            putJsonObject("kkmInfo") {
                put("registrationNumber", "existing-reg-num")
            }
        }
        val infoResult = OfdCommandResult(status = OfdCommandStatus.OK, responseToken = 777L, responseJson = responseJson)

        every { kkmCommonHelper.sendOfdCommand(any(), OfdCommandType.SYSTEM, any(), 555L, defaultInfo, "TEMP_REG_kkm-id-1", "TEMP_FACTORY_kkm-id-1", "ofd-tag") } returns systemResult
        every { kkmCommonHelper.sendOfdCommand(any(), OfdCommandType.INFO, any(), 777L, defaultInfo, "TEMP_REG_kkm-id-1", "TEMP_FACTORY_kkm-id-1", "ofd-tag") } returns infoResult

        // Another KKM exists with registration number "existing-reg-num"
        val existingKkm = kkm.copy(id = "other-kkm-id")
        every { storage.findKkmByRegistrationNumber("existing-reg-num") } returns existingKkm

        assertFailsWith<ConflictException> {
            registerKkm.initKkmSimple(
                pin = "0000",
                ofdId = "ofd",
                ofdEnvironment = "prod",
                ofdSystemId = "sys-1",
                ofdToken = "token",
                defaultVatGroup = VatGroup.VAT_10,
                okved = "12345"
            )
        }
    }

    @Test
    fun testInitKkmSimpleSuccess() {
        every { kkmCommonHelper.ensureSystemTimeValid() } returns Unit
        every { ofdConfig.validateAndFormatTag("ofd", "prod") } returns "ofd-tag"
        every { clock.now() } returns 1000L
        every { clock.currentYear() } returns 2026
        every { storage.findKkmBySystemId("sys-1") } returns null
        every { idGenerator.nextId() } returns "kkm-id-123"
        every { tokenCodec.parseToken("token") } returns 555L
        every { tokenCodec.encodeToken(777L) } returns "enc-777"

        val defaultInfo = OfdServiceInfo(
            orgTitle = "UNKNOWN",
            orgAddress = "UNKNOWN",
            orgAddressKz = "UNKNOWN",
            orgInn = "000000000000",
            orgOkved = "12345",
            geoLatitude = 0,
            geoLongitude = 0,
            geoSource = "UNKNOWN"
        )
        every { kkmCommonHelper.defaultServiceInfo() } returns defaultInfo

        val systemResult = OfdCommandResult(status = OfdCommandStatus.OK, responseToken = 777L)
        val responseJson = buildJsonObject {
            putJsonObject("payload") {
                putJsonObject("service") {
                    putJsonObject("regInfo") {
                        putJsonObject("kkm") {
                            put("fnsKkmId", "reg-num-simple")
                            put("serialNumber", "fact-num-simple")
                        }
                        putJsonObject("org") {
                            put("title", "Org Title Simple")
                            put("address", "Org Address Simple")
                            put("bin", "Org Bin Simple")
                            put("okved", "12345")
                        }
                    }
                }
            }
        }
        val infoResult = OfdCommandResult(status = OfdCommandStatus.OK, responseToken = 777L, responseJson = responseJson)

        every { kkmCommonHelper.sendOfdCommand(any(), OfdCommandType.SYSTEM, any(), 555L, defaultInfo, "TEMP_REG_kkm-id-1", "TEMP_FACTORY_kkm-id-1", "ofd-tag") } returns systemResult
        every { kkmCommonHelper.sendOfdCommand(any(), OfdCommandType.INFO, any(), 777L, defaultInfo, "TEMP_REG_kkm-id-1", "TEMP_FACTORY_kkm-id-1", "ofd-tag") } returns infoResult

        every { storage.findKkmByRegistrationNumber("reg-num-simple") } returns null
        every { storage.createKkm(any()) } returns true
        every { storage.listUsers("kkm-id-123") } returns emptyList()

        val res = registerKkm.initKkmSimple(
            pin = "0000",
            ofdId = "ofd",
            ofdEnvironment = "prod",
            ofdSystemId = "sys-1",
            ofdToken = "token",
            defaultVatGroup = VatGroup.VAT_10,
            okved = "12345"
        )

        assertEquals("kkm-id-123", res.id)
        assertEquals("reg-num-simple", res.registrationNumber)
        assertEquals("fact-num-simple", res.factoryNumber)
        assertEquals(TaxRegime.VAT_PAYER, res.taxRegime)
        assertEquals(VatGroup.VAT_10, res.defaultVatGroup)
        verify { storage.createKkm(match { it.id == "kkm-id-123" }) }
    }

    @Test
    fun testInitKkmSimpleStorageCreateFail() {
        every { kkmCommonHelper.ensureSystemTimeValid() } returns Unit
        every { ofdConfig.validateAndFormatTag("ofd", "prod") } returns "ofd-tag"
        every { clock.now() } returns 1000L
        every { clock.currentYear() } returns 2026
        every { storage.findKkmBySystemId("sys-1") } returns null
        every { idGenerator.nextId() } returns "kkm-id-123"
        every { tokenCodec.parseToken("token") } returns 555L
        every { tokenCodec.encodeToken(777L) } returns "enc-777"

        val defaultInfo = OfdServiceInfo(
            orgTitle = "UNKNOWN",
            orgAddress = "UNKNOWN",
            orgAddressKz = "UNKNOWN",
            orgInn = "000000000000",
            orgOkved = "12345",
            geoLatitude = 0,
            geoLongitude = 0,
            geoSource = "UNKNOWN"
        )
        every { kkmCommonHelper.defaultServiceInfo() } returns defaultInfo

        val systemResult = OfdCommandResult(status = OfdCommandStatus.OK, responseToken = 777L)
        val responseJson = buildJsonObject {
            putJsonObject("kkmInfo") {
                put("registrationNumber", "reg-num-simple")
                put("factoryNumber", "fact-num-simple")
                put("lastShiftNo", 42L)
            }
            putJsonObject("orgInfo") {
                put("title", "Org Title Simple")
                put("address", "Org Address Simple")
                put("bin", "Org Bin Simple")
                put("okved", "12345")
            }
        }
        val infoResult = OfdCommandResult(status = OfdCommandStatus.OK, responseToken = 777L, responseJson = responseJson)

        every { kkmCommonHelper.sendOfdCommand(any(), OfdCommandType.SYSTEM, any(), 555L, defaultInfo, "TEMP_REG_kkm-id-1", "TEMP_FACTORY_kkm-id-1", "ofd-tag") } returns systemResult
        every { kkmCommonHelper.sendOfdCommand(any(), OfdCommandType.INFO, any(), 777L, defaultInfo, "TEMP_REG_kkm-id-1", "TEMP_FACTORY_kkm-id-1", "ofd-tag") } returns infoResult

        every { storage.findKkmByRegistrationNumber("reg-num-simple") } returns null
        every { storage.createKkm(any()) } returns false

        assertFailsWith<ConflictException> {
            registerKkm.initKkmSimple(
                pin = "0000",
                ofdId = "ofd",
                ofdEnvironment = "prod",
                ofdSystemId = "sys-1",
                ofdToken = "token",
                defaultVatGroup = VatGroup.NO_VAT,
                okved = "12345"
            )
        }
    }

    // ==========================================
    // InitializeKkmRegistrationUseCase Tests
    // ==========================================

    @Test
    fun testInitializeKkmRegistrationFactoryRequired() {
        val baseKkmWithoutFactory = kkm.copy(factoryNumber = null)
        val params = InitializeKkmRegistrationUseCase.KkmInitializationParams(
            baseInfo = baseKkmWithoutFactory,
            ofdToken = "token",
            registrationNumber = "reg-1",
            factoryNumber = null,
            ofdTag = "ofd-tag",
            okvedOverride = "12345",
            updateKkm = {}
        )

        assertFailsWith<ValidationException> {
            initializeKkmRegistration.execute(params)
        }
    }

    @Test
    fun testInitializeKkmRegistrationInfoResultNull() {
        val params = InitializeKkmRegistrationUseCase.KkmInitializationParams(
            baseInfo = kkm.copy(factoryNumber = "fact-1"),
            ofdToken = "token",
            registrationNumber = "reg-1",
            factoryNumber = "fact-1",
            ofdTag = "ofd-tag",
            okvedOverride = "12345",
            updateKkm = {}
        )
        every { tokenCodec.parseToken("token") } returns 555L
        val systemResult = OfdCommandResult(status = OfdCommandStatus.FAILED, errorMessage = "System error")
        every { kkmCommonHelper.sendOfdCommand(any(), OfdCommandType.SYSTEM, any(), any(), any(), any(), any(), any()) } returns systemResult

        val result = initializeKkmRegistration.execute(params)
        assertEquals(params.baseInfo, result)
    }

    @Test
    fun testInitializeKkmRegistrationSuccess() {
        val params = InitializeKkmRegistrationUseCase.KkmInitializationParams(
            baseInfo = kkm.copy(factoryNumber = "fact-1"),
            ofdToken = "token",
            registrationNumber = "reg-1",
            factoryNumber = "fact-1",
            ofdTag = "ofd-tag",
            okvedOverride = "12345",
            updateKkm = mockk(relaxed = true)
        )
        every { tokenCodec.parseToken("token") } returns 555L
        every { tokenCodec.encodeToken(777L) } returns "enc-777"
        every { clock.now() } returns 1000L

        val defaultInfo = OfdServiceInfo(
            orgTitle = "UNKNOWN",
            orgAddress = "UNKNOWN",
            orgAddressKz = "UNKNOWN",
            orgInn = "000000000000",
            orgOkved = "00000",
            geoLatitude = 0,
            geoLongitude = 0,
            geoSource = "UNKNOWN"
        )
        every { kkmCommonHelper.defaultServiceInfo() } returns defaultInfo

        val systemResult = OfdCommandResult(status = OfdCommandStatus.OK, responseToken = 777L)
        val responseJson = buildJsonObject {
            putJsonObject("payload") {
                putJsonObject("service") {
                    putJsonObject("regInfo") {
                        putJsonObject("kkm") {
                            put("fnsKkmId", "reg-1")
                            put("serialNumber", "fact-1")
                        }
                        putJsonObject("org") {
                            put("title", "Org Title")
                            put("address", "Org Address")
                            put("bin", "Org Bin")
                            put("okved", "")
                        }
                    }
                    putJsonObject("zxReport") {
                        putJsonArray("nonNullableSums") {
                            add(buildJsonObject {
                                put("operation", "SELL")
                                putJsonObject("sum") {
                                    put("bills", 9999L)
                                }
                            })
                        }
                    }
                }
            }
        }
        val infoResult = OfdCommandResult(status = OfdCommandStatus.OK, responseToken = 777L, responseJson = responseJson)

        every { kkmCommonHelper.sendOfdCommand(any(), OfdCommandType.SYSTEM, any(), any(), any(), any(), any(), any()) } returns systemResult
        every { kkmCommonHelper.sendOfdCommand(any(), OfdCommandType.INFO, any(), any(), any(), any(), any(), any()) } returns infoResult
        every { storage.listUsers(any()) } returns emptyList()
        every { idGenerator.nextId() } returns "user-id"
        every { pinHasher.hash(any()) } returns "hash"

        val result = initializeKkmRegistration.execute(params)
        assertEquals("reg-1", result.registrationNumber)
        assertEquals("enc-777", result.tokenEncryptedBase64)
        verify { storage.upsertCounter(result.id, "GLOBAL", null, CounterKeyFormats.NON_NULLABLE_SUM.format("SELL"), 9999L) }
        verify { storage.createUser(result.id, any(), "Администратор", UserRole.ADMIN, "0000", "hash", 1000L) }
    }

    @Test
    fun testInitializeKkmRegistrationDefaultOkvedFallback() {
        val params = InitializeKkmRegistrationUseCase.KkmInitializationParams(
            baseInfo = kkm.copy(factoryNumber = "fact-1"),
            ofdToken = "token",
            registrationNumber = "reg-1",
            factoryNumber = "fact-1",
            ofdTag = "ofd-tag",
            okvedOverride = null,
            updateKkm = mockk(relaxed = true)
        )
        every { tokenCodec.parseToken("token") } returns 555L
        every { tokenCodec.encodeToken(777L) } returns "enc-777"
        every { clock.now() } returns 1000L

        // Default info has a valid OKVED
        val defaultInfo = OfdServiceInfo(
            orgTitle = "UNKNOWN",
            orgAddress = "UNKNOWN",
            orgAddressKz = "UNKNOWN",
            orgInn = "000000000000",
            orgOkved = "54321",
            geoLatitude = 0,
            geoLongitude = 0,
            geoSource = "UNKNOWN"
        )
        every { kkmCommonHelper.defaultServiceInfo() } returns defaultInfo

        val systemResult = OfdCommandResult(status = OfdCommandStatus.OK, responseToken = 777L)
        // Response JSON returns an empty OKVED
        val responseJson = buildJsonObject {
            putJsonObject("kkmInfo") {
                put("registrationNumber", "reg-1")
            }
            putJsonObject("orgInfo") {
                put("okved", "00000")
            }
        }
        val infoResult = OfdCommandResult(status = OfdCommandStatus.OK, responseToken = 777L, responseJson = responseJson)

        every { kkmCommonHelper.sendOfdCommand(any(), OfdCommandType.SYSTEM, any(), any(), any(), any(), any(), any()) } returns systemResult
        every { kkmCommonHelper.sendOfdCommand(any(), OfdCommandType.INFO, any(), any(), any(), any(), any(), any()) } returns infoResult

        val result = initializeKkmRegistration.execute(params)
        assertEquals("54321", result.ofdServiceInfo?.orgOkved)
    }

    @Test
    fun testInitializeKkmRegistrationPerformOfdInfoFailWithToken() {
        every { tokenCodec.parseToken("token") } returns 555L
        every { clock.now() } returns 1000L
        every { tokenCodec.encodeToken(777L) } returns "enc-777"

        val defaultInfo = OfdServiceInfo(
            orgTitle = "UNKNOWN",
            orgAddress = "UNKNOWN",
            orgAddressKz = "UNKNOWN",
            orgInn = "000000000000",
            orgOkved = "12345",
            geoLatitude = 0,
            geoLongitude = 0,
            geoSource = "UNKNOWN"
        )

        val systemResult = OfdCommandResult(status = OfdCommandStatus.OK, responseToken = 777L)
        val infoResult = OfdCommandResult(status = OfdCommandStatus.FAILED, responseToken = 777L, errorMessage = "Failed info command")

        every { kkmCommonHelper.sendOfdCommand(any(), OfdCommandType.SYSTEM, any(), any(), any(), any(), any(), any()) } returns systemResult
        every { kkmCommonHelper.sendOfdCommand(any(), OfdCommandType.INFO, any(), any(), any(), any(), any(), any()) } returns infoResult

        val result = initializeKkmRegistration.performOfdSystemAndInfo(
            baseInfo = kkm,
            initialToken = 555L,
            serviceInfo = defaultInfo,
            registrationNumber = "reg-1",
            factoryNumber = "fact-1",
            ofdTag = "ofd-tag"
        )
        assertNull(result)
        verify { storage.updateKkmToken(kkm.id, "enc-777", 1000L) }
    }

    @Test
    fun testInitializeKkmRegistrationPerformOfdInfoFailNoToken() {
        every { tokenCodec.parseToken("token") } returns 555L
        every { clock.now() } returns 1000L

        val defaultInfo = OfdServiceInfo(
            orgTitle = "UNKNOWN",
            orgAddress = "UNKNOWN",
            orgAddressKz = "UNKNOWN",
            orgInn = "000000000000",
            orgOkved = "12345",
            geoLatitude = 0,
            geoLongitude = 0,
            geoSource = "UNKNOWN"
        )

        val systemResult = OfdCommandResult(status = OfdCommandStatus.OK, responseToken = 777L)
        val infoResult = OfdCommandResult(status = OfdCommandStatus.FAILED, responseToken = null, errorMessage = "Failed info command")

        every { kkmCommonHelper.sendOfdCommand(any(), OfdCommandType.SYSTEM, any(), any(), any(), any(), any(), any()) } returns systemResult
        every { kkmCommonHelper.sendOfdCommand(any(), OfdCommandType.INFO, any(), any(), any(), any(), any(), any()) } returns infoResult

        val result = initializeKkmRegistration.performOfdSystemAndInfo(
            baseInfo = kkm,
            initialToken = 555L,
            serviceInfo = defaultInfo,
            registrationNumber = "reg-1",
            factoryNumber = "fact-1",
            ofdTag = "ofd-tag"
        )
        assertNull(result)
    }

    @Test
    fun testInitializeKkmRegistrationEnsureDefaultUsersExist() {
        every { storage.listUsers("kkm-1") } returns listOf(mockk())
        initializeKkmRegistration.ensureDefaultUsers("kkm-1", 1000L)
        verify(exactly = 0) { storage.createUser(any(), any(), any(), any(), any(), any(), any()) }
    }

    // ==========================================
    // DecommissionKkmUseCase Tests
    // ==========================================

    @Test
    fun testDecommissionKkmRequiresProgramming() {
        val activeKkm = kkm.copy(state = KkmState.ACTIVE.name)
        assertFailsWith<ValidationException> {
            decommissionKkm.execute(activeKkm)
        }
    }

    @Test
    fun testDecommissionKkmShiftOpen() {
        every { storage.findOpenShift("kkm-1") } returns mockk()
        assertFailsWith<ConflictException> {
            decommissionKkm.execute(kkm)
        }
    }

    @Test
    fun testDecommissionKkmQueueNotEmpty() {
        every { storage.findOpenShift("kkm-1") } returns null
        every { queue.canSendDirectly("kkm-1") } returns false
        assertFailsWith<ConflictException> {
            decommissionKkm.execute(kkm)
        }
    }

    @Test
    fun testDecommissionKkmNotFound() {
        every { storage.findOpenShift("kkm-1") } returns null
        every { queue.canSendDirectly("kkm-1") } returns true
        every { storage.deleteKkmCompletely("kkm-1") } returns false

        assertFailsWith<NotFoundException> {
            decommissionKkm.execute(kkm)
        }
    }

    @Test
    fun testDecommissionKkmSuccess() {
        every { queue.canSendDirectly("kkm-1") } returns true
        every { storage.findOpenShift("kkm-1") } returns null
        every { storage.deleteKkmCompletely("kkm-1") } returns true

        val res = decommissionKkm.execute(kkm)
        assertEquals(true, res)
        verify {
            storage.deleteKkmCompletely("kkm-1")
            queue.deleteQueuedCommands("kkm-1")
        }
    }

    // ==========================================
    // EnterProgrammingUseCase Tests
    // ==========================================

    @Test
    fun testEnterProgrammingSuccess() {
        val registeredKkm = kkm.copy(mode = KkmMode.REGISTRATION.name, state = KkmState.ACTIVE.name)
        every { clock.now() } returns 1000L

        val updated = enterProgramming.execute(registeredKkm)
        assertEquals(KkmMode.PROGRAMMING.name, updated.mode)
        assertEquals(KkmState.PROGRAMMING.name, updated.state)
        verify {
            storage.updateKkm(match { it.mode == KkmMode.PROGRAMMING.name && it.state == KkmState.PROGRAMMING.name })
        }
    }

    // ==========================================
    // ExitProgrammingUseCase Tests
    // ==========================================

    @Test
    fun testExitProgrammingSuccessDraft() {
        every { clock.now() } returns 1000L
        val draftKkm = kkm.copy(registrationNumber = "")

        val updated = exitProgramming.execute(draftKkm)
        assertEquals(KkmMode.REGISTRATION.name, updated.mode)
        assertEquals(KkmState.IDLE.name, updated.state)
        verify {
            storage.updateKkm(match { it.mode == KkmMode.REGISTRATION.name && it.state == KkmState.IDLE.name })
        }
    }

    @Test
    fun testExitProgrammingSuccessRegistered() {
        every { clock.now() } returns 1000L

        val updated = exitProgramming.execute(kkm)
        assertEquals(KkmMode.REGISTRATION.name, updated.mode)
        assertEquals(KkmState.ACTIVE.name, updated.state)
        verify {
            storage.updateKkm(match { it.mode == KkmMode.REGISTRATION.name && it.state == KkmState.ACTIVE.name })
        }
    }

    // ==========================================
    // UpdateKkmSettingsUseCase Tests
    // ==========================================

    @Test
    fun testUpdateGeneralSettingsRequiresProgramming() {
        val activeKkm = kkm.copy(state = KkmState.ACTIVE.name)
        assertFailsWith<ValidationException> {
            updateKkmSettings.updateGeneralSettings(activeKkm, true)
        }
    }

    @Test
    fun testUpdateGeneralSettingsSuccess() {
        every { clock.now() } returns 1000L
        val res = updateKkmSettings.updateGeneralSettings(kkm, true)
        assertEquals(true, res.autoCloseShift)
        verify { storage.updateKkm(match { it.autoCloseShift }) }
    }

    @Test
    fun testUpdateTaxSettingsRequiresProgrammingState() {
        val invalidKkm = kkm.copy(state = KkmState.ACTIVE.name)
        assertFailsWith<ValidationException> {
            updateKkmSettings.updateTaxSettings(invalidKkm, TaxRegime.VAT_PAYER, VatGroup.VAT_10)
        }
    }

    @Test
    fun testUpdateTaxSettingsRequiresProgrammingMode() {
        val invalidKkm = kkm.copy(mode = KkmMode.REGISTRATION.name)
        assertFailsWith<ValidationException> {
            updateKkmSettings.updateTaxSettings(invalidKkm, TaxRegime.VAT_PAYER, VatGroup.VAT_10)
        }
    }

    @Test
    fun testUpdateTaxSettingsShiftOpen() {
        every { storage.findOpenShift("kkm-1") } returns mockk()
        assertFailsWith<ConflictException> {
            updateKkmSettings.updateTaxSettings(kkm, TaxRegime.VAT_PAYER, VatGroup.VAT_10)
        }
    }

    @Test
    fun testUpdateTaxSettingsQueueNotEmpty() {
        every { storage.findOpenShift("kkm-1") } returns null
        every { queue.canSendDirectly("kkm-1") } returns false
        assertFailsWith<ConflictException> {
            updateKkmSettings.updateTaxSettings(kkm, TaxRegime.VAT_PAYER, VatGroup.VAT_10)
        }
    }

    @Test
    fun testUpdateTaxSettingsSuccess() {
        every { clock.now() } returns 1000L
        every { storage.findOpenShift("kkm-1") } returns null
        every { queue.canSendDirectly("kkm-1") } returns true

        val res = updateKkmSettings.updateTaxSettings(kkm, TaxRegime.VAT_PAYER, VatGroup.VAT_10)
        assertEquals(TaxRegime.VAT_PAYER, res.taxRegime)
        assertEquals(VatGroup.VAT_10, res.defaultVatGroup)
        verify { storage.updateKkm(match { it.taxRegime == TaxRegime.VAT_PAYER && it.defaultVatGroup == VatGroup.VAT_10 }) }
    }

    @Test
    fun testUpdateBrandingRequiresProgramming() {
        val activeKkm = kkm.copy(state = KkmState.ACTIVE.name)
        assertFailsWith<ValidationException> {
            updateKkmSettings.updateBranding(activeKkm, ReceiptBranding())
        }
    }

    @Test
    fun testUpdateBrandingSuccess() {
        every { clock.now() } returns 1000L
        val branding = ReceiptBranding(themeColor = "red")
        val res = updateKkmSettings.updateBranding(kkm, branding)
        assertEquals(branding, res.branding)
        verify { storage.updateKkm(match { it.branding == branding }) }
    }

    // ==========================================
    // EnforceAutonomousLimitsUseCase Tests
    // ==========================================

    @Test
    fun testEnforceAutonomousLimitsNoQueue() {
        every { clock.now() } returns 1000L
        every { queue.canSendDirectly("kkm-1") } returns true

        enforceAutonomousLimits.execute(kkm.copy(autonomousSince = 500L, state = KkmState.ACTIVE.name))
        verify {
            storage.updateKkm(match { it.autonomousSince == null })
        }
    }

    @Test
    fun testEnforceAutonomousLimitsStartAutonomous() {
        every { clock.now() } returns 1000L
        every { queue.canSendDirectly("kkm-1") } returns false

        enforceAutonomousLimits.execute(kkm.copy(autonomousSince = null, state = KkmState.ACTIVE.name))
        verify {
            storage.updateKkm(match { it.autonomousSince == 1000L })
        }
    }

    @Test
    fun testEnforceAutonomousLimitsResetAutonomous() {
        every { clock.now() } returns 1000L
        every { queue.canSendDirectly("kkm-1") } returns true

        enforceAutonomousLimits.execute(kkm.copy(autonomousSince = 500L, state = KkmState.ACTIVE.name))
        verify {
            storage.updateKkm(match { it.autonomousSince == null })
        }
    }

    @Test
    fun testEnforceAutonomousLimitsTimeoutBlocked() {
        every { clock.now() } returns 100000000000L
        every { queue.canSendDirectly("kkm-1") } returns false

        assertFailsWith<ConflictException> {
            enforceAutonomousLimits.execute(kkm.copy(autonomousSince = 500L, state = KkmState.ACTIVE.name))
        }
        verify {
            storage.updateKkm(match { it.state == KkmState.BLOCKED.name })
        }
    }

    @Test
    fun testEnforceAutonomousLimitsTimeoutAlreadyBlocked() {
        every { clock.now() } returns 100000000000L
        every { queue.canSendDirectly("kkm-1") } returns false

        assertFailsWith<ConflictException> {
            enforceAutonomousLimits.execute(kkm.copy(autonomousSince = 500L, state = KkmState.BLOCKED.name))
        }
        // Verify we don't update state again if already blocked
        verify(exactly = 0) {
            storage.updateKkm(any())
        }
    }

    @Test
    fun testEnforceAutonomousLimitsBlockedHasQueue() {
        every { clock.now() } returns 1000L
        every { queue.canSendDirectly("kkm-1") } returns false

        assertFailsWith<ConflictException> {
            enforceAutonomousLimits.execute(kkm.copy(autonomousSince = 500L, state = KkmState.BLOCKED.name))
        }
    }

    @Test
    fun testEnforceAutonomousLimitsBlockedNoQueue() {
        every { clock.now() } returns 1000L
        every { queue.canSendDirectly("kkm-1") } returns true

        enforceAutonomousLimits.execute(kkm.copy(autonomousSince = 500L, state = KkmState.BLOCKED.name))
        verify {
            storage.updateKkm(match { it.state == KkmState.ACTIVE.name && it.autonomousSince == null })
        }
    }

    @Test
    fun testEnforceAutonomousLimitsDefaultLimit() {
        val enforceWithDefault = EnforceAutonomousLimitsUseCase(storage, queue, clock)
        // DEFAULT_MAX_AUTONOMOUS_DURATION_MS is 72 hours
        val limit = 72L * 60L * 60L * 1000L

        every { queue.canSendDirectly("kkm-1") } returns false

        // Within limit (71 hours) -> does not block or throw
        every { clock.now() } returns limit - 1000L
        enforceWithDefault.execute(kkm.copy(autonomousSince = 0L, state = KkmState.ACTIVE.name))

        // Exceeds limit (73 hours) -> blocks and throws
        every { clock.now() } returns limit + 1000L
        assertFailsWith<ConflictException> {
            enforceWithDefault.execute(kkm.copy(autonomousSince = 0L, state = KkmState.ACTIVE.name))
        }
    }
}
