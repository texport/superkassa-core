package kz.mybrain.superkassa.core.presentation.facade

import io.mockk.every
import io.mockk.mockk
import kz.mybrain.superkassa.core.domain.exception.NotFoundException
import kz.mybrain.superkassa.core.domain.exception.ValidationException
import kz.mybrain.superkassa.core.domain.exception.ConflictException
import kz.mybrain.superkassa.core.domain.model.delivery.DeliveryStatus
import kz.mybrain.superkassa.core.domain.model.common.CounterSnapshot
import kz.mybrain.superkassa.core.domain.model.common.TaxRegime
import kz.mybrain.superkassa.core.domain.model.common.VatGroup
import kz.mybrain.superkassa.core.domain.model.common.TimeValidationResult
import kz.mybrain.superkassa.core.domain.model.kkm.CashOperationRequest
import kz.mybrain.superkassa.core.domain.model.kkm.FiscalDocumentSnapshot
import kz.mybrain.superkassa.core.domain.model.kkm.KkmInfo
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandResult
import kz.mybrain.superkassa.core.domain.model.ofd.OfdCommandStatus
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptBranding
import kz.mybrain.superkassa.core.domain.model.receipt.ReceiptRequest
import kz.mybrain.superkassa.core.domain.model.report.PrintDocumentType
import kz.mybrain.superkassa.core.domain.model.settings.CoreSettings
import kz.mybrain.superkassa.core.domain.model.shift.ShiftInfo
import kz.mybrain.superkassa.core.domain.model.shift.ShiftStatus
import kz.mybrain.superkassa.core.domain.model.auth.UserRole
import kz.mybrain.superkassa.core.domain.model.auth.KkmUser
import kz.mybrain.superkassa.core.domain.port.*
import kz.mybrain.superkassa.core.presentation.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SuperkassaApiImplTest {

    private val storage = mockk<StoragePort>(relaxed = true)
    private val queue = mockk<OfflineQueuePort>(relaxed = true)
    private val ofd = mockk<OfdManagerPort>(relaxed = true)
    private val ofdConfig = mockk<OfdConfigPort>(relaxed = true)
    private val delivery = mockk<DeliveryPort>(relaxed = true)
    private val tokenCodec = mockk<TokenCodecPort>(relaxed = true)
    private val idGenerator = mockk<IdGeneratorPort>(relaxed = true)
    private val clock = mockk<ClockPort>(relaxed = true)
    private val pinHasher = mockk<PinHasherPort>(relaxed = true)
    private val coreSettings = mockk<CoreSettings>(relaxed = true)
    private val receiptRender = mockk<ReceiptRenderPort>(relaxed = true)
    private val docConvert = mockk<DocumentConvertPort>(relaxed = true)
    private val timeValidator = mockk<TimeValidatorPort>(relaxed = true)

    private val api = SuperkassaApiImpl(
        storage = storage,
        queue = queue,
        ofd = ofd,
        ofdConfig = ofdConfig,
        delivery = delivery,
        tokenCodec = tokenCodec,
        idGenerator = idGenerator,
        clock = clock,
        pinHasher = pinHasher,
        coreSettings = coreSettings,
        receiptRenderPort = receiptRender,
        documentConvertPort = docConvert,
        timeValidator = timeValidator
    )

    init {
        every { storage.inTransaction<Any?>(any()) } answers {
            val block = firstArg<() -> Any?>()
            block()
        }
        every { ofdConfig.parseTag(any()) } answers {
            val tag = firstArg<String>()
            val parts = tag.split("_")
            if (parts.size >= 2) {
                parts[0] to parts[1]
            } else {
                "telecom" to "prod"
            }
        }
        every { ofdConfig.validateAndFormatTag(any(), any()) } answers {
            val p = firstArg<String>()
            val e = secondArg<String>()
            "${p}_${e}"
        }
        every { storage.findOpenShift(any()) } returns null
        every { storage.findKkmForUpdate(any()) } answers { storage.findKkm(firstArg()) }
        every { storage.findIdempotencyResponse(any(), any()) } returns null
        every { queue.canSendDirectly(any()) } returns true
        every { storage.findKkmByRegistrationNumber(any()) } returns null
        every { storage.findKkmBySystemId(any()) } returns null
        every { ofd.send(any()) } returns OfdCommandResult(status = OfdCommandStatus.OK)
    }

    private val testKkmInfo = KkmInfo(
        id = "kkm-1",
        createdAt = 1000L,
        updatedAt = 2000L,
        mode = "ACTIVE",
        state = "ACTIVE",
        registrationNumber = "kgd-1",
        factoryNumber = "SWK-0001",
        ofdProvider = "telecom_prod",
        systemId = "12345"
    )

    private val testUser = KkmUser(
        id = "user-1",
        name = "Cashier 1",
        role = UserRole.CASHIER,
        pin = "hash-1",
        createdAt = 1000L
    )

    private val adminUser = KkmUser(
        id = "admin-1",
        name = "Admin 1",
        role = UserRole.ADMIN,
        pin = "hash-admin",
        createdAt = 1000L
    )

    @Test
    fun `listVatRates returns all vat rates`() {
        val rates = api.listVatRates()
        assertEquals(VatGroup.entries.size, rates.size)
        assertEquals("NO_VAT", rates.first().code)
    }

    @Test
    fun `generateFactoryInfo calls generators correctly`() {
        every { idGenerator.generateFactoryNumber() } returns "SWK-2026-0001"
        every { clock.currentYear() } returns 2026

        val response = api.generateFactoryInfo()
        assertEquals("SWK-2026-0001", response.factoryNumber)
        assertEquals(2026, response.manufactureYear)
    }

    @Test
    fun `getKkm throws NotFoundException when KKM does not exist`() {
        every { storage.findKkm("non-existent") } returns null
        assertFailsWith<NotFoundException> {
            api.getKkm("non-existent")
        }
    }

    @Test
    fun `getKkm returns KKM when exists`() {
        every { storage.findKkm("kkm-1") } returns testKkmInfo
        val result = api.getKkm("kkm-1")
        assertEquals(testKkmInfo, result)
    }

    @Test
    fun `listKkms calls storage list and count`() {
        every { storage.listKkms(any(), any(), any(), any(), any(), any()) } returns listOf(testKkmInfo)
        every { storage.countKkms(any(), any()) } returns 1

        val params = KkmListParams()
        val result = api.listKkms(params)
        assertEquals(1, result.total)
        assertEquals(1, result.items.size)
        assertEquals(testKkmInfo, result.items.first())
    }

    @Test
    fun `deleteKkm requires ADMIN role and calls decommission`() {
        val programmingKkm = testKkmInfo.copy(state = "PROGRAMMING", mode = "PROGRAMMING")
        every { storage.findKkm("kkm-1") } returns programmingKkm
        every { pinHasher.hash("1234") } returns "hash-admin"
        every { storage.findUserByPin("kkm-1", "hash-admin") } returns adminUser
        every { storage.findOpenShift("kkm-1") } returns null
        every { queue.canSendDirectly("kkm-1") } returns true
        every { storage.deleteKkmCompletely("kkm-1") } returns true

        val deleted = api.deleteKkm("kkm-1", "1234")
        assertTrue(deleted)
    }

    @Test
    fun `listCounters requires ADMIN and returns counters`() {
        every { storage.findKkm("kkm-1") } returns testKkmInfo
        every { pinHasher.hash("1234") } returns "hash-admin"
        every { storage.findUserByPin("kkm-1", "hash-admin") } returns adminUser
        val snapshotList = listOf(CounterSnapshot(scope = "SHIFT", shiftId = "shift-1", key = "cash", value = 100L, updatedAt = 1000L))
        every { storage.listCounters("kkm-1") } returns snapshotList
        val res = api.listCounters("kkm-1", "1234")
        assertEquals(1, res.size)
    }

    @Test
    fun `updateKkmSettings requires ADMIN and calls storage update`() {
        val programmingKkm = testKkmInfo.copy(state = "PROGRAMMING", mode = "PROGRAMMING")
        every { storage.findKkm("kkm-1") } returns programmingKkm
        every { pinHasher.hash("1234") } returns "hash-admin"
        every { storage.findUserByPin("kkm-1", "hash-admin") } returns adminUser
        every { timeValidator.validate(any()) } returns TimeValidationResult(true, null, null)
        every { storage.updateKkm(any()) } returns true

        val updated = api.updateKkmSettings("kkm-1", "1234", true)
        assertTrue(updated.autoCloseShift)
    }

    @Test
    fun `listUsers requires ADMIN and returns DTO list`() {
        every { storage.findKkm("kkm-1") } returns testKkmInfo
        every { pinHasher.hash("1234") } returns "hash-admin"
        every { storage.findUserByPin("kkm-1", "hash-admin") } returns adminUser
        every { storage.listUsers("kkm-1") } returns listOf(testUser)

        val users = api.listUsers("kkm-1", "1234")
        assertEquals(1, users.size)
        assertEquals("Cashier 1", users.first().name)
    }

    @Test
    fun `createUser successfully creates and saves user`() {
        every { storage.findKkm("kkm-1") } returns testKkmInfo
        every { pinHasher.hash("1234") } returns "hash-admin"
        every { storage.findUserByPin("kkm-1", "hash-admin") } returns adminUser
        every { idGenerator.nextId() } returns "user-new"
        every { pinHasher.hash("4321") } returns "hash-4321"
        every { storage.createUser(any(), any(), any(), any(), any(), any(), any()) } returns true

        val request = UserCreateRequest(name = "New User", role = UserRole.CASHIER, userPin = "4321")
        val created = api.createUser("kkm-1", "1234", request)
        assertEquals("user-new", created.userId)
        assertEquals("New User", created.name)
        assertEquals(UserRole.CASHIER, created.role)
    }

    @Test
    fun `updateUser updates user details successfully`() {
        every { storage.findKkm("kkm-1") } returns testKkmInfo
        every { pinHasher.hash("1234") } returns "hash-admin"
        every { storage.findUserByPin("kkm-1", "hash-admin") } returns adminUser
        every { storage.listUsers("kkm-1") } returns listOf(testUser)
        every { storage.updateUser(any(), any(), any(), any(), any(), any()) } returns true

        val request = UserUpdateRequest(name = "Updated User")
        val updated = api.updateUser("kkm-1", "user-1", "1234", request)
        assertEquals("Updated User", updated.name)
    }

    @Test
    fun `deleteUser removes user successfully`() {
        every { storage.findKkm("kkm-1") } returns testKkmInfo
        every { pinHasher.hash("1234") } returns "hash-admin"
        every { storage.findUserByPin("kkm-1", "hash-admin") } returns adminUser
        
        // Setup two admin users, one is not the user being deleted (user-1)
        val admin1 = adminUser.copy(id = "admin-1")
        val admin2 = adminUser.copy(id = "admin-2")
        // We delete user-1 (which is testUser). We must return testUser, admin1, and admin2.
        // Wait, testUser is a CASHIER, not an ADMIN. The validation rule says:
        // "on the cash register, there must be at least one user with this role".
        // If we delete testUser (role = CASHIER), we must have another CASHIER remaining!
        val otherCashier = testUser.copy(id = "user-2")
        every { storage.listUsers("kkm-1") } returns listOf(testUser, otherCashier, admin1, admin2)
        every { storage.deleteUser("kkm-1", "user-1") } returns true

        val deleted = api.deleteUser("kkm-1", "user-1", "1234")
        assertTrue(deleted)
    }

    @Test
    fun `getOfdAuthInfo decrypts token and returns credentials`() {
        every { storage.findKkm("kkm-1") } returns testKkmInfo.copy(tokenEncryptedBase64 = "encrypted-token")
        every { pinHasher.hash("1234") } returns "hash-admin"
        every { storage.findUserByPin("kkm-1", "hash-admin") } returns adminUser
        every { tokenCodec.decodeToken("encrypted-token") } returns 9999L
        every { storage.loadCounters("kkm-1", any(), null) } returns mapOf("ofd.req_num" to 5L)

        val auth = api.getOfdAuthInfo("kkm-1", "1234")
        assertEquals("9999", auth.token)
        assertEquals(6, auth.nextReqNum)
    }

    @Test
    fun `updateOfdToken encrypts and updates database`() {
        every { storage.findKkm("kkm-1") } returns testKkmInfo
        every { pinHasher.hash("1234") } returns "hash-admin"
        every { storage.findUserByPin("kkm-1", "hash-admin") } returns adminUser
        every { tokenCodec.parseToken("new-token") } returns 8888L
        every { tokenCodec.encodeToken(8888L) } returns "new-encrypted"
        every { storage.updateKkmToken(any(), any(), any()) } returns true

        val updated = api.updateOfdToken("kkm-1", "1234", "new-token")
        assertTrue(updated)
    }

    @Test
    fun `checkOfdConnection executes check connection command`() {
        every { storage.findKkm("kkm-1") } returns testKkmInfo
        every { timeValidator.validate(any()) } returns TimeValidationResult(true, null, null)
        val mockResult = OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0)
        every { ofdConfig.parseTag("telecom_prod") } returns ("telecom" to "prod")
        every { ofdConfig.validateAndFormatTag("telecom", "prod") } returns "telecom_prod"
        every { ofd.send(any()) } returns mockResult

        val result = api.checkOfdConnection("kkm-1")
        assertEquals(OfdCommandStatus.OK, result.status)
    }

    @Test
    fun `getOfdInfo executes get ofd info command`() {
        every { storage.findKkm("kkm-1") } returns testKkmInfo
        every { timeValidator.validate(any()) } returns TimeValidationResult(true, null, null)
        val mockResult = OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0)
        every { ofdConfig.parseTag("telecom_prod") } returns ("telecom" to "prod")
        every { ofdConfig.validateAndFormatTag("telecom", "prod") } returns "telecom_prod"
        every { ofd.send(any()) } returns mockResult

        val result = api.getOfdInfo("kkm-1")
        assertEquals(OfdCommandStatus.OK, result.status)
    }

    @Test
    fun `getReceiptHtml returns html output`() {
        every { storage.findKkm("kkm-1") } returns testKkmInfo
        every { pinHasher.hash("1234") } returns "hash-1"
        every { storage.findUserByPin("kkm-1", "hash-1") } returns testUser
        val mockSnapshot = FiscalDocumentSnapshot(
            id = "doc-1",
            cashboxId = "kkm-1",
            shiftId = "shift-1",
            docType = "CHECK",
            docNo = 1L,
            shiftNo = 1L,
            createdAt = 1000L,
            totalAmount = 100L,
            currency = "KZT",
            fiscalSign = "fs",
            autonomousSign = "as",
            isAutonomous = false,
            ofdStatus = "DELIVERED",
            deliveredAt = 1000L
        )
        every { storage.findFiscalDocumentWithReceiptPayload("doc-1") } returns (mockSnapshot to mockk<ReceiptRequest>())
        every { receiptRender.renderHtml(any(), any(), any(), any()) } returns "<html>Receipt</html>"

        val html = api.getReceiptHtml("kkm-1", "doc-1", "1234")
        assertEquals("<html>Receipt</html>", html)
    }

    @Test
    fun `getPrintHtml returns html output for print`() {
        every { storage.findKkm("kkm-1") } returns testKkmInfo
        every { pinHasher.hash("1234") } returns "hash-1"
        every { storage.findUserByPin("kkm-1", "hash-1") } returns testUser
        val mockSnapshot = FiscalDocumentSnapshot(
            id = "doc-1",
            cashboxId = "kkm-1",
            shiftId = "shift-1",
            docType = "CHECK",
            docNo = 1L,
            shiftNo = 1L,
            createdAt = 1000L,
            totalAmount = 100L,
            currency = "KZT",
            fiscalSign = "fs",
            autonomousSign = "as",
            isAutonomous = false,
            ofdStatus = "DELIVERED",
            deliveredAt = 1000L
        )
        every { storage.findFiscalDocumentById("doc-1") } returns mockSnapshot
        every { storage.findFiscalDocumentWithReceiptPayload("doc-1") } returns (mockSnapshot to mockk<ReceiptRequest>())
        every { receiptRender.renderHtml(any(), any(), any(), any()) } returns "<html>Receipt</html>"

        val html = api.getPrintHtml("kkm-1", PrintDocumentType.DOCUMENT, "doc-1", null, "1234")
        assertEquals("<html>Receipt</html>", html)
    }

    @Test
    fun `getPrintPdf returns byte array pdf`() {
        every { storage.findKkm("kkm-1") } returns testKkmInfo
        every { pinHasher.hash("1234") } returns "hash-1"
        every { storage.findUserByPin("kkm-1", "hash-1") } returns testUser
        val mockSnapshot = FiscalDocumentSnapshot(
            id = "doc-1",
            cashboxId = "kkm-1",
            shiftId = "shift-1",
            docType = "CHECK",
            docNo = 1L,
            shiftNo = 1L,
            createdAt = 1000L,
            totalAmount = 100L,
            currency = "KZT",
            fiscalSign = "fs",
            autonomousSign = "as",
            isAutonomous = false,
            ofdStatus = "DELIVERED",
            deliveredAt = 1000L
        )
        every { storage.findFiscalDocumentById("doc-1") } returns mockSnapshot
        every { storage.findFiscalDocumentWithReceiptPayload("doc-1") } returns (mockSnapshot to mockk<ReceiptRequest>())
        every { receiptRender.renderHtml(any(), any(), any(), any()) } returns "<html>Receipt</html>"
        every { docConvert.htmlToPdf("<html>Receipt</html>") } returns byteArrayOf(1, 2, 3)

        val pdf = api.getPrintPdf("kkm-1", PrintDocumentType.DOCUMENT, "doc-1", null, "1234")
        assertEquals(3, pdf.size)
    }

    @Test
    fun `cashIn executes cash flow operation`() {
        every { storage.findKkm("kkm-1") } returns testKkmInfo
        every { timeValidator.validate(any()) } returns TimeValidationResult(true, null, null)
        every { pinHasher.hash("1234") } returns "hash-1"
        every { storage.findUserByPin("kkm-1", "hash-1") } returns testUser
        every { idGenerator.nextId() } returns "cashop-1"
        every { queue.canSendDirectly("kkm-1") } returns true
        every { ofdConfig.parseTag("telecom_prod") } returns ("telecom" to "prod")
        every { ofdConfig.validateAndFormatTag("telecom", "prod") } returns "telecom_prod"

        val request = CashOperationRequest(
            pin = "1234",
            idempotencyKey = "key-cash-in",
            amount = 500.0
        )
        every { storage.findOpenShift("kkm-1") } returns ShiftInfo("shift-1", "kkm-1", 1L, ShiftStatus.OPEN, 1000L)
        val ofdCommandResult = OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0)
        every { ofd.send(any()) } returns ofdCommandResult
        every { storage.findFiscalDocumentById(any()) } returns FiscalDocumentSnapshot(
            id = "cashop-1",
            cashboxId = "kkm-1",
            shiftId = "shift-1",
            docType = "CASH_IN",
            docNo = 2L,
            shiftNo = 1L,
            createdAt = 1000L,
            totalAmount = 50000L, // 500.0 * 100
            currency = "KZT",
            fiscalSign = "fs",
            autonomousSign = "as",
            isAutonomous = false,
            ofdStatus = "DELIVERED",
            deliveredAt = 1000L
        )

        // Mock transaction block to execute the lambda synchronously
        every { storage.inTransaction<Any?>(any()) } answers {
            val block = firstArg<() -> Any?>()
            block()
        }

        val result = api.cashIn("kkm-1", "1234", request)
        assertNotNull(result.documentId)
    }

    @Test
    fun `initKkm executes KKM initialization UseCases`() {
        every { timeValidator.validate(any()) } returns TimeValidationResult(true, null, null)
        every { idGenerator.nextId() } returns "kkm-new"
        every { storage.inTransaction<Any?>(any()) } answers {
            val block = firstArg<() -> Any?>()
            block()
        }
        every { storage.createKkm(any()) } returns true
        every { pinHasher.hash("1234") } returns "hash-1234"
        every { storage.createUser(any(), any(), any(), any(), any(), any(), any()) } returns true

        val request = KkmInitDirectRequest(
            ofdId = "kazakhtelecom",
            ofdEnvironment = "test",
            ofdSystemId = "12345",
            ofdToken = "token-abc-123",
            kkmKgdId = "123456789012",
            factoryNumber = "SWK-0001",
            manufactureYear = 2024,
            okved = "47110"
        )
        val kkm = api.initKkm("0000", request)
        assertEquals("kkm-new", kkm.id)
    }

    @Test
    fun `initKkmSimple initializes KKM with defaults`() {
        every { timeValidator.validate(any()) } returns TimeValidationResult(true, null, null)
        every { idGenerator.nextId() } returns "kkm-simple"
        every { storage.inTransaction<Any?>(any()) } answers {
            val block = firstArg<() -> Any?>()
            block()
        }
        every { storage.createKkm(any()) } returns true
        every { pinHasher.hash("1234") } returns "hash-1234"
        every { storage.createUser(any(), any(), any(), any(), any(), any(), any()) } returns true

        val request = KkmInitSimpleRequest(
            ofdId = "kazakhtelecom",
            ofdEnvironment = "test",
            ofdSystemId = "200367",
            ofdToken = "32876190",
            okved = "47110"
        )
        val kkm = api.initKkmSimple("0000", request)
        assertEquals("kkm-simple", kkm.id)
    }

    @Test
    fun `updateTaxSettings updates regime and VAT`() {
        val programmingKkm = testKkmInfo.copy(state = "PROGRAMMING", mode = "PROGRAMMING")
        every { storage.findKkm("kkm-1") } returns programmingKkm
        every { pinHasher.hash("1234") } returns "hash-admin"
        every { storage.findUserByPin("kkm-1", "hash-admin") } returns adminUser
        every { timeValidator.validate(any()) } returns TimeValidationResult(true, null, null)
        every { storage.updateKkm(any()) } returns true

        val result = api.updateTaxSettings("kkm-1", "1234", TaxRegime.MIXED, VatGroup.VAT_16)
        assertEquals(TaxRegime.MIXED, result.taxRegime)
        assertEquals(VatGroup.VAT_16, result.defaultVatGroup)
    }

    @Test
    fun `updateBrandingSettings updates branding`() {
        val programmingKkm = testKkmInfo.copy(state = "PROGRAMMING", mode = "PROGRAMMING")
        every { storage.findKkm("kkm-1") } returns programmingKkm
        every { pinHasher.hash("1234") } returns "hash-admin"
        every { storage.findUserByPin("kkm-1", "hash-admin") } returns adminUser
        every { timeValidator.validate(any()) } returns TimeValidationResult(true, null, null)
        every { storage.updateKkm(any()) } returns true

        val branding = ReceiptBranding(headerMsg = "Super Title")
        val result = api.updateBrandingSettings("kkm-1", "1234", branding)
        assertEquals(branding, result.branding)
    }

    @Test
    fun `enterProgramming updates state to PROGRAMMING`() {
        every { storage.findKkm("kkm-1") } returns testKkmInfo
        every { pinHasher.hash("1234") } returns "hash-admin"
        every { storage.findUserByPin("kkm-1", "hash-admin") } returns adminUser
        every { timeValidator.validate(any()) } returns TimeValidationResult(true, null, null)
        every { storage.updateKkm(any()) } returns true
        every { storage.findOpenShift("kkm-1") } returns null

        val result = api.enterProgramming("kkm-1", "1234")
        assertEquals("PROGRAMMING", result.state)
    }

    @Test
    fun `exitProgramming updates state to ACTIVE`() {
        val programmingKkm = testKkmInfo.copy(state = "PROGRAMMING")
        every { storage.findKkm("kkm-1") } returns programmingKkm
        every { pinHasher.hash("1234") } returns "hash-admin"
        every { storage.findUserByPin("kkm-1", "hash-admin") } returns adminUser
        every { timeValidator.validate(any()) } returns TimeValidationResult(true, null, null)
        every { storage.updateKkm(any()) } returns true

        val result = api.exitProgramming("kkm-1", "1234")
        assertEquals("ACTIVE", result.state)
    }

    @Test
    fun `syncOfdServiceInfo calls OFD manager and updates settings`() {
        every { storage.findKkm("kkm-1") } returns testKkmInfo
        every { pinHasher.hash("1234") } returns "hash-admin"
        every { storage.findUserByPin("kkm-1", "hash-admin") } returns adminUser
        every { timeValidator.validate(any()) } returns TimeValidationResult(true, null, null)
        every { queue.canSendDirectly("kkm-1") } returns true
        every { storage.findOpenShift("kkm-1") } returns null
        every { tokenCodec.decodeToken(any()) } returns 1234L
        every { storage.inTransaction<Any?>(any()) } answers {
            val block = firstArg<() -> Any?>()
            block()
        }
        val ofdCommandResult = OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0)
        every { ofd.send(any()) } returns ofdCommandResult

        val result = api.syncOfdServiceInfo("kkm-1", "1234")
        assertEquals(OfdCommandStatus.OK, result.status)
    }

    @Test
    fun `syncOfdCounters calls OFD manager and updates counters`() {
        every { storage.findKkm("kkm-1") } returns testKkmInfo
        every { pinHasher.hash("1234") } returns "hash-admin"
        every { storage.findUserByPin("kkm-1", "hash-admin") } returns adminUser
        every { timeValidator.validate(any()) } returns TimeValidationResult(true, null, null)
        every { queue.canSendDirectly("kkm-1") } returns true
        every { storage.findOpenShift("kkm-1") } returns null
        every { tokenCodec.decodeToken(any()) } returns 1234L
        every { storage.inTransaction<Any?>(any()) } answers {
            val block = firstArg<() -> Any?>()
            block()
        }
        val ofdCommandResult = OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0)
        every { ofd.send(any()) } returns ofdCommandResult

        val result = api.syncOfdCounters("kkm-1", "1234")
        assertEquals(OfdCommandStatus.OK, result.status)
    }

    @Test
    fun `createReceipt methods map to underlying use case`() {
        every { storage.findKkm("kkm-1") } returns testKkmInfo
        every { timeValidator.validate(any()) } returns TimeValidationResult(true, null, null)
        every { pinHasher.hash("1234") } returns "hash-1"
        every { storage.findUserByPin("kkm-1", "hash-1") } returns testUser
        every { storage.findOpenShift("kkm-1") } returns ShiftInfo("shift-1", "kkm-1", 1L, ShiftStatus.OPEN, 1000L)
        every { idGenerator.nextId() } returns "doc-new"
        every { queue.canSendDirectly("kkm-1") } returns true
        every { storage.inTransaction<Any?>(any()) } answers {
            val block = firstArg<() -> Any?>()
            block()
        }
        val ofdCommandResult = OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0)
        every { ofd.send(any()) } returns ofdCommandResult
        val mockSnapshot = FiscalDocumentSnapshot(
            id = "doc-new",
            cashboxId = "kkm-1",
            shiftId = "shift-1",
            docType = "CHECK",
            docNo = 1L,
            shiftNo = 1L,
            createdAt = 1000L,
            totalAmount = 100L,
            currency = "KZT",
            fiscalSign = "fs",
            autonomousSign = "as",
            isAutonomous = false,
            ofdStatus = "DELIVERED",
            deliveredAt = 1000L
        )
        every { storage.findFiscalDocumentById("doc-new") } returns mockSnapshot

        val sellReq = ReceiptSellRequest(
            items = listOf(ReceiptItemDto(name = "Item 1", price = 10.0, quantity = 1L, vatGroup = "VAT_16", measureUnitCode = "796")),
            payments = listOf(ReceiptPaymentDto("CASH", 10.0)),
            idempotencyKey = "sell-key"
        )
        val resSell = api.createSellReceipt("kkm-1", "1234", sellReq)
        assertEquals("doc-new", resSell.documentId)

        val parentTicket = ParentTicketDto(
            parentTicketNumber = 123L,
            parentTicketDateTime = "2026-06-27T16:00:00Z",
            kgdKkmId = "kgd-1",
            parentTicketTotal = 10.0,
            parentTicketIsOffline = false
        )

        val sellRetReq = ReceiptSellReturnRequest(
            items = listOf(ReceiptItemDto(name = "Item 1", price = 10.0, quantity = 1L, vatGroup = "VAT_16", measureUnitCode = "796")),
            payments = listOf(ReceiptPaymentDto("CASH", 10.0)),
            idempotencyKey = "sell-ret-key",
            parentTicket = parentTicket
        )
        val resSellRet = api.createSellReturnReceipt("kkm-1", "1234", sellRetReq)
        assertEquals("doc-new", resSellRet.documentId)

        val buyReq = ReceiptBuyRequest(
            items = listOf(ReceiptItemDto(name = "Item 1", price = 10.0, quantity = 1L, vatGroup = "VAT_16", measureUnitCode = "796")),
            payments = listOf(ReceiptPaymentDto("CASH", 10.0)),
            idempotencyKey = "buy-key"
        )
        val resBuy = api.createBuyReceipt("kkm-1", "1234", buyReq)
        assertEquals("doc-new", resBuy.documentId)

        val buyRetReq = ReceiptBuyReturnRequest(
            items = listOf(ReceiptItemDto(name = "Item 1", price = 10.0, quantity = 1L, vatGroup = "VAT_16", measureUnitCode = "796")),
            payments = listOf(ReceiptPaymentDto("CASH", 10.0)),
            idempotencyKey = "buy-ret-key",
            parentTicket = parentTicket
        )
        val resBuyRet = api.createBuyReturnReceipt("kkm-1", "1234", buyRetReq)
        assertEquals("doc-new", resBuyRet.documentId)
    }

    @Test
    fun `cashOut executes cash out operation`() {
        every { storage.findKkm("kkm-1") } returns testKkmInfo
        every { timeValidator.validate(any()) } returns TimeValidationResult(true, null, null)
        every { pinHasher.hash("1234") } returns "hash-1"
        every { storage.findUserByPin("kkm-1", "hash-1") } returns testUser
        every { idGenerator.nextId() } returns "cashop-2"
        every { queue.canSendDirectly("kkm-1") } returns true
        every { ofdConfig.parseTag("telecom_prod") } returns ("telecom" to "prod")
        every { ofdConfig.validateAndFormatTag("telecom", "prod") } returns "telecom_prod"

        val request = CashOperationRequest(
            pin = "1234",
            idempotencyKey = "key-cash-out",
            amount = 100.0
        )
        every { storage.findOpenShift("kkm-1") } returns ShiftInfo("shift-1", "kkm-1", 1L, ShiftStatus.OPEN, 1000L)
        val ofdCommandResult = OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0)
        every { ofd.send(any()) } returns ofdCommandResult
        every { storage.findFiscalDocumentById(any()) } returns FiscalDocumentSnapshot(
            id = "cashop-2",
            cashboxId = "kkm-1",
            shiftId = "shift-1",
            docType = "CASH_OUT",
            docNo = 3L,
            shiftNo = 1L,
            createdAt = 1000L,
            totalAmount = 10000L,
            currency = "KZT",
            fiscalSign = "fs",
            autonomousSign = "as",
            isAutonomous = false,
            ofdStatus = "DELIVERED",
            deliveredAt = 1000L
        )

        every { storage.inTransaction<Any?>(any()) } answers {
            val block = firstArg<() -> Any?>()
            block()
        }

        val result = api.cashOut("kkm-1", "1234", request)
        assertNotNull(result.documentId)
    }

    @Test
    fun `retryReceiptDelivery executes delivery retry`() {
        every { storage.findKkm("kkm-1") } returns testKkmInfo
        every { pinHasher.hash("1234") } returns "hash-1"
        every { storage.findUserByPin("kkm-1", "hash-1") } returns testUser
        val mockSnapshot = FiscalDocumentSnapshot(
            id = "doc-1",
            cashboxId = "kkm-1",
            shiftId = "shift-1",
            docType = "CHECK",
            docNo = 1L,
            shiftNo = 1L,
            createdAt = 1000L,
            totalAmount = 100L,
            currency = "KZT",
            fiscalSign = "fs",
            autonomousSign = "as",
            isAutonomous = false,
            ofdStatus = "DELIVERED",
            deliveredAt = 1000L
        )
        every { storage.findFiscalDocumentWithReceiptPayload("doc-1") } returns (mockSnapshot to mockk<ReceiptRequest>())
        every { delivery.deliver(any()) } returns true

        val results = api.retryReceiptDelivery("kkm-1", "doc-1", "1234")
        assertTrue(results.isEmpty()) // Since coreSettings.delivery is mock-relaxed (null)
    }

    @Test
    fun `shift operations open close get list shifts and list docs`() {
        every { storage.findKkm("kkm-1") } returns testKkmInfo
        every { timeValidator.validate(any()) } returns TimeValidationResult(true, null, null)
        every { pinHasher.hash("1234") } returns "hash-admin"
        every { storage.findUserByPin("kkm-1", "hash-admin") } returns adminUser
        every { idGenerator.nextId() } returns "shift-new"
        every { queue.canSendDirectly("kkm-1") } returns true
        every { storage.inTransaction<Any?>(any()) } answers {
            val block = firstArg<() -> Any?>()
            block()
        }
        val ofdCommandResult = OfdCommandResult(status = OfdCommandStatus.OK, resultCode = 0)
        every { ofd.send(any()) } returns ofdCommandResult

        every { storage.findOpenShift("kkm-1") } returns null
        val openShift = api.openShift("kkm-1", "1234")
        assertEquals("shift-new", openShift.id)

        // Close shift
        every { storage.findOpenShift("kkm-1") } returns ShiftInfo("shift-new", "kkm-1", 1L, ShiftStatus.OPEN, 1000L)
        every { idGenerator.nextId() } returns "report-new"
        val closeShift = api.closeShift("kkm-1", "1234")
        assertEquals("report-new", closeShift.documentId)

        // Get open shift
        val getOpen = api.getOpenShift("kkm-1", "1234")
        assertEquals("shift-new", getOpen.id)

        every { storage.findOpenShift("kkm-1") } returns null
        assertFailsWith<ConflictException> {
            api.getOpenShift("kkm-1", "1234")
        }

        // List shifts
        every { storage.listShifts("kkm-1", 10, 0) } returns listOf(openShift)
        val listShifts = api.listShifts("kkm-1", 10, 0, "1234")
        assertEquals(1, listShifts.size)

        // List shift docs
        val mockSnapshot = FiscalDocumentSnapshot(
            id = "doc-1",
            cashboxId = "kkm-1",
            shiftId = "shift-new",
            docType = "CHECK",
            docNo = 1L,
            shiftNo = 1L,
            createdAt = 1000L,
            totalAmount = 100L,
            currency = "KZT",
            fiscalSign = "fs",
            autonomousSign = "as",
            isAutonomous = false,
            ofdStatus = "DELIVERED",
            deliveredAt = 1000L
        )
        every { storage.listFiscalDocumentsByShift("kkm-1", "shift-new", 10, 0) } returns listOf(mockSnapshot)
        val docs = api.listShiftDocuments("kkm-1", "shift-new", 10, 0, "1234")
        assertEquals(1, docs.size)

        // List by period
        every { storage.listFiscalDocumentsByPeriod("kkm-1", 1000L, 2000L, 10, 0) } returns listOf(mockSnapshot)
        val periodDocs = api.listFiscalDocumentsByPeriod("kkm-1", 1000L, 2000L, 10, 0, "1234")
        assertEquals(1, periodDocs.size)

        // Create report
        val report = api.createReport("kkm-1", "1234")
        assertEquals("report-new", report.documentId)
    }

    @Test
    fun `requireOperational checks blocked and programming state`() {
        // Blocked state check
        val blockedKkm = testKkmInfo.copy(state = "BLOCKED")
        every { storage.findKkm("kkm-1") } returns blockedKkm
        every { timeValidator.validate(any()) } returns TimeValidationResult(true, null, null)
        every { pinHasher.hash("1234") } returns "hash-admin"
        every { storage.findUserByPin("kkm-1", "hash-admin") } returns adminUser
        assertFailsWith<ValidationException> {
            api.createReport("kkm-1", "1234")
        }

        // Programming state check
        val programmingKkm = testKkmInfo.copy(state = "PROGRAMMING")
        every { storage.findKkm("kkm-1") } returns programmingKkm
        assertFailsWith<ValidationException> {
            api.createReport("kkm-1", "1234")
        }
    }

    @Test
    fun `createReceipt and createReport in offline mode`() {
        every { storage.findKkm("kkm-1") } returns testKkmInfo
        every { timeValidator.validate(any()) } returns TimeValidationResult(true, null, null)
        every { pinHasher.hash("1234") } returns "hash-1"
        every { storage.findUserByPin("kkm-1", "hash-1") } returns testUser
        every { storage.findOpenShift("kkm-1") } returns ShiftInfo("shift-1", "kkm-1", 1L, ShiftStatus.OPEN, 1000L)
        every { idGenerator.nextId() } returns "doc-new"
        
        // Mock KKM to be offline
        every { queue.canSendDirectly("kkm-1") } returns false

        val sellReq = ReceiptSellRequest(
            items = listOf(ReceiptItemDto(name = "Item 1", price = 10.0, quantity = 1L, vatGroup = "VAT_16", measureUnitCode = "796")),
            payments = listOf(ReceiptPaymentDto("CASH", 10.0)),
            idempotencyKey = "sell-key-offline"
        )
        val resSell = api.createSellReceipt("kkm-1", "1234", sellReq)
        assertEquals(DeliveryStatus.ONLINE_OK, resSell.deliveryStatus)

        // Close report offline
        every { pinHasher.hash("1234") } returns "hash-admin"
        every { storage.findUserByPin("kkm-1", "hash-admin") } returns adminUser
        every { idGenerator.nextId() } returns "report-offline"
        val createReport = api.createReport("kkm-1", "1234")
        assertEquals(DeliveryStatus.OFFLINE_QUEUED, createReport.deliveryStatus)
    }

    @Test
    fun `createReport and createReceipt with closed shift and OFD errors`() {
        every { storage.findKkm("kkm-1") } returns testKkmInfo
        every { timeValidator.validate(any()) } returns TimeValidationResult(true, null, null)
        every { pinHasher.hash("1234") } returns "hash-1"
        every { storage.findUserByPin("kkm-1", "hash-1") } returns testUser
        every { idGenerator.nextId() } returns "doc-new"
        every { queue.canSendDirectly("kkm-1") } returns true

        // 1. Closed shift check
        every { storage.findOpenShift("kkm-1") } returns null
        val sellReq = ReceiptSellRequest(
            items = listOf(ReceiptItemDto(name = "Item 1", price = 10.0, quantity = 1L, vatGroup = "VAT_16", measureUnitCode = "796")),
            payments = listOf(ReceiptPaymentDto("CASH", 10.0)),
            idempotencyKey = "sell-key-closed"
        )
        assertFailsWith<ConflictException> {
            api.createSellReceipt("kkm-1", "1234", sellReq)
        }

        // 2. OFD TIMEOUT and FAILED checks for createReport
        every { pinHasher.hash("1234") } returns "hash-admin"
        every { storage.findUserByPin("kkm-1", "hash-admin") } returns adminUser
        
        // Timeout check
        every { ofd.send(any()) } returns OfdCommandResult(status = OfdCommandStatus.TIMEOUT, errorMessage = "Timeout")
        val resTimeout = api.createReport("kkm-1", "1234")
        assertEquals(DeliveryStatus.OFFLINE_QUEUED, resTimeout.deliveryStatus)
        assertEquals("Timeout", resTimeout.deliveryError)

        // Failed check
        every { ofd.send(any()) } returns OfdCommandResult(status = OfdCommandStatus.FAILED, errorMessage = "Failed")
        val resFailed = api.createReport("kkm-1", "1234")
        assertEquals(DeliveryStatus.ONLINE_ERROR, resFailed.deliveryStatus)
        assertEquals("Failed", resFailed.deliveryError)
    }
}
