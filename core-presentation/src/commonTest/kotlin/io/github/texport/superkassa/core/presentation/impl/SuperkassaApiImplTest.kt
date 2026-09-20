package io.github.texport.superkassa.core.presentation.impl

import io.github.texport.superkassa.core.domain.api.model.auth.KkmUser as DomainKkmUser
import io.github.texport.superkassa.core.domain.api.model.common.VatGroup as DomainVatGroup
import io.github.texport.superkassa.core.domain.api.model.common.TaxRegime as DomainTaxRegime
import io.github.texport.superkassa.core.domain.api.model.common.Money as DomainMoney
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType as DomainOperationType
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest as DomainReceiptRequest
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptItem as DomainReceiptItem
import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.mockk.every
import io.mockk.mockk
import io.github.texport.superkassa.core.domain.api.exception.NotFoundException
import io.github.texport.superkassa.core.domain.api.exception.ValidationException
import io.github.texport.superkassa.core.domain.api.exception.ConflictException
import io.github.texport.superkassa.core.domain.api.model.common.CounterSnapshot
import io.github.texport.superkassa.core.domain.api.model.common.TimeValidationResult
import io.github.texport.superkassa.core.domain.api.model.kkm.CashOperationRequest as DomainCashOperationRequest
import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandResult
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandStatus as DomainOfdCommandStatus
import io.github.texport.superkassa.core.domain.api.model.ofd.OfdCommandRequest
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptBranding
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftStatus
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole as DomainUserRole
import io.github.texport.superkassa.core.domain.api.model.auth.KkmUser
import io.github.texport.superkassa.core.domain.api.port.integration.*
import io.github.texport.superkassa.core.domain.api.port.internal.*
import io.github.texport.superkassa.core.presentation.api.SuperkassaApi
import io.github.texport.superkassa.core.presentation.api.PrintApi
import io.github.texport.superkassa.core.presentation.api.OfflineQueueApi
import io.github.texport.superkassa.core.domain.impl.usecase.queue.GetQueueStatusUseCase
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType
import io.github.texport.superkassa.core.presentation.api.model.*
import io.github.texport.superkassa.core.presentation.api.model.auth.*
import io.github.texport.superkassa.core.presentation.api.model.common.*
import io.github.texport.superkassa.core.presentation.api.model.kkm.*
import io.github.texport.superkassa.core.presentation.api.model.ofd.*
import io.github.texport.superkassa.core.presentation.api.model.queue.*
import io.github.texport.superkassa.core.presentation.api.model.receipt.*
import io.github.texport.superkassa.core.presentation.api.model.shift.*
import io.github.texport.superkassa.core.presentation.api.model.user.*
import io.github.texport.superkassa.core.presentation.api.model.ofd.*
import io.github.texport.superkassa.core.presentation.api.model.queue.*
import io.github.texport.superkassa.core.domain.impl.usecase.queue.ListQueueItemsUseCase
import io.github.texport.superkassa.core.domain.impl.usecase.queue.RetryFailedQueueItemsUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
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
    private val printApi = mockk<PrintApi>(relaxed = true)

    private val api = SuperkassaApiImpl(
        storage = storage,
        queuePort = queue,
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
        timeValidator = timeValidator,
        printApi = printApi
    )

    init {
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
        every { ofd.send(any()) } returns OfdCommandResult(status = DomainOfdCommandStatus.OK)
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
        systemId = "12345",
        // Чеки этой кассы в тестах несут VAT_16, и режим обязан это
        // допускать: неплательщик НДС такой чек не оформляет.
        taxRegime = DomainTaxRegime.VAT_PAYER,
        defaultVatGroup = DomainVatGroup.VAT_16
    )

    private val testUser = KkmUser(
        id = "user-1",
        name = "Cashier 1",
        role = DomainUserRole.CASHIER,
        createdAt = 1000L
    )

    private val adminUser = KkmUser(
        id = "admin-1",
        name = "Admin 1",
        role = DomainUserRole.ADMIN,
        createdAt = 1000L
    )

    @Test
    fun `listVatRates returns all vat rates`() {
        val rates = api.listVatRates()
        assertEquals(io.github.texport.superkassa.core.domain.api.model.common.VatGroup.entries.size, rates.size)
        assertEquals("NO_VAT", rates.first().code)
        assertNotNull(rates.first().name)
        assertEquals("Без НДС", rates.first().name.ru)
    }

    @Test
    fun `generateFactoryInfo calls generators correctly`() {
        every { idGenerator.generateFactoryNumber(any()) } returns "SWK-2026-0001"
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
        assertEquals(io.github.texport.superkassa.core.presentation.impl.mapper.KkmMapper.toResponse(testKkmInfo), result)
    }

    @Test
    fun `getKkm answers even when the autonomous limit is exceeded`() {
        // Чтение кассы отказом не отвечает: превышенный лимит записывается
        // в состояние, а сказать о нём должно само действие, а не список.
        val tooLong = testKkmInfo.copy(autonomousSince = 1L)
        every { storage.findKkm("kkm-1") } returns tooLong
        every { queue.canSendDirectly("kkm-1") } returns false
        every { storage.updateKkm(any()) } returns true
        every { clock.now() } returns AUTONOMOUS_LIMIT_PASSED

        val result = api.getKkm("kkm-1")

        assertEquals("kkm-1", result.kkmId)
    }

    @Test
    fun `getKkm clears the autonomous mark once the queue has drained`() {
        // Очередь расходится в фоне: без сверки при чтении касса числилась
        // бы автономной до следующего чека, а вместе с ней и время
        // автономной работы, которое уходит в ОФД.
        val autonomous = testKkmInfo.copy(autonomousSince = 1L)
        every { storage.findKkm("kkm-1") } returns autonomous
        every { queue.canSendDirectly("kkm-1") } returns true
        every { storage.updateKkm(any()) } returns true
        every { clock.now() } returns 1_000L

        val result = api.getKkm("kkm-1")

        assertNull(result.autonomousSince)
    }

    @Test
    fun `listKkms calls storage list and count`() {
        every { storage.listKkms(any(), any(), any(), any(), any(), any()) } returns listOf(testKkmInfo)
        every { storage.countKkms(any(), any()) } returns 1
        every { storage.findOpenShift("kkm-1") } returns ShiftInfo("shift-1", "kkm-1", 1L, ShiftStatus.OPEN, 1000L)
        
        // Let's just restore the basic assertion, but to cover branches, the storage mock will execute the `lastError` fetching.
        every { storage.listQueueTasksByCashbox("kkm-1", "OFFLINE", 20) } returns listOf(
            io.github.texport.superkassa.core.domain.api.model.queue.QueueTask(
                id = "task-1", cashboxId = "kkm-1", lane = "OFFLINE", type = "REPORT", payloadRef = "ref-1",
                status = "FAILED", lastError = "Network timeout", attempt = 1, nextAttemptAt = null, createdAt = 1000L
            )
        )

        val params = KkmListParams()
        val result = api.listKkms(params)
        assertEquals(1, result.total)
        assertEquals(1, result.items.size)
        
        val expected = io.github.texport.superkassa.core.presentation.impl.mapper.KkmMapper.toResponse(testKkmInfo).copy(
            isShiftOpen = true,
            shiftOpenedAt = 1000L,
            offlineQueueCount = 0, // QueueStatus will throw since queue is not mocked properly here, or it returns relaxed 0
            lastSyncError = "Network timeout"
        )
        assertEquals(expected, result.items.first())
    }

    @Test
    fun `listKkms handles exceptions in shift and queue status fetching`() {
        every { storage.listKkms(any(), any(), any(), any(), any(), any()) } returns listOf(testKkmInfo)
        every { storage.countKkms(any(), any()) } returns 1
        every { storage.findOpenShift("kkm-1") } returns null
        every { storage.listQueueTasksByCashbox(any(), any(), any(), any()) } throws RuntimeException("DB error")

        val params = KkmListParams()
        val result = api.listKkms(params)
        assertEquals(1, result.total)
        
        val expected = io.github.texport.superkassa.core.presentation.impl.mapper.KkmMapper.toResponse(testKkmInfo).copy(
            isShiftOpen = false,
            shiftOpenedAt = null,
            offlineQueueCount = 0,
            lastSyncError = null
        )
        assertEquals(expected, result.items.first())
    }
    
    @Test
    fun `getKkm handles exceptions in shift and queue status fetching`() {
        every { storage.findKkm("kkm-1") } returns testKkmInfo
        every { storage.findOpenShift("kkm-1") } returns null
        every { storage.listQueueTasksByCashbox(any(), any(), any(), any()) } throws RuntimeException("DB error")
        
        val result = api.getKkm("kkm-1")
        val expected = io.github.texport.superkassa.core.presentation.impl.mapper.KkmMapper.toResponse(testKkmInfo).copy(
            isShiftOpen = false,
            shiftOpenedAt = null,
            offlineQueueCount = 0,
            lastSyncError = null
        )
        assertEquals(expected, result)
    }

    @Test
    fun `getLocalOpenShift returns shift when open`() {
        every { storage.findKkm("kkm-1") } returns testKkmInfo
        val testUser = KkmUser("user-1", "Name", DomainUserRole.ADMIN, 1000L)
        every { storage.findUserByPin("kkm-1", any()) } returns testUser
        val testShift = ShiftInfo("shift-1", "kkm-1", 1L, ShiftStatus.OPEN, 1000L)
        every { storage.findOpenShift("kkm-1") } returns testShift
        
        val result = api.getLocalOpenShift("kkm-1", "1234")
        assertEquals(testShift.id, result?.id)
        assertEquals(testShift.shiftNo, result?.shiftNo)
    }
    
    @Test
    fun `getLocalOpenShift returns null when no open shift`() {
        every { storage.findKkm("kkm-1") } returns testKkmInfo
        val testUser = KkmUser("user-1", "Name", DomainUserRole.ADMIN, 1000L)
        every { storage.findUserByPin("kkm-1", any()) } returns testUser
        every { storage.findOpenShift("kkm-1") } returns null
        
        val result = api.getLocalOpenShift("kkm-1", "1234")
        assertEquals(null, result)
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

        val updated = api.updateKkmSettings("kkm-1", "1234", true, false)
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
        every { storage.createUser(any(), any(), any(), any(), any(), any()) } returns true

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
        every { storage.updateUser(any(), any(), any(), any(), any()) } returns true

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

        val auth = api.getOfdAuthInfo("1234", OfdAuthInfoRequest("kkm-1"))
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
        val mockResult = OfdCommandResult(status = DomainOfdCommandStatus.OK, resultCode = 0)
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
        val mockResult = OfdCommandResult(status = DomainOfdCommandStatus.OK, resultCode = 0)
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
        every { printApi.getReceiptHtml(any(), any(), any(), any()) } returns "<html>Receipt</html>"

        val html = api.getReceiptHtml("kkm-1", "doc-1", "1234")
        assertEquals("<html>Receipt</html>", html)
    }

    @Test
    fun `отвергнутый документ отдаёт код отказа`() {
        // Код отказа лежал в снимке документа, но представление его не
        // переносило: приложение получало «FAILED» без причины.
        every { storage.findKkm("kkm-1") } returns testKkmInfo
        every { pinHasher.hash("1234") } returns "hash-1"
        every { storage.findUserByPin("kkm-1", "hash-1") } returns testUser
        every {
            storage.listFiscalDocumentsByPeriod("kkm-1", any(), any(), any(), any())
        } returns listOf(
            FiscalDocumentSnapshot(
                id = "doc-refused",
                cashboxId = "kkm-1",
                shiftId = "shift-1",
                docType = "SALE",
                docNo = null,
                shiftNo = 1L,
                createdAt = 1000L,
                totalAmount = 100L,
                currency = "KZT",
                fiscalSign = null,
                autonomousSign = null,
                isAutonomous = false,
                ofdStatus = "FAILED",
                ofdErrorCode = 13,
                deliveredAt = null
            )
        )

        val docs = api.listFiscalDocumentsByPeriod("kkm-1", 0L, 9_999_999L, 10, 0, "1234")

        assertEquals("FAILED", docs.first().ofdStatus)
        assertEquals(13, docs.first().ofdErrorCode)
    }

    @Test
    fun `getPrintHtml returns html output for print`() {
        every { printApi.getPrintHtml(any(), any(), any(), any(), any(), any()) } returns "<html>Receipt</html>"

        val html = api.getPrintHtml("kkm-1", PrintDocumentType.DOCUMENT, "doc-1", null, "1234")
        assertEquals("<html>Receipt</html>", html)
    }

    @Test
    fun `getPrintPdf returns byte array pdf`() {
        every { printApi.getPrintPdf(any(), any(), any(), any(), any(), any()) } returns byteArrayOf(1, 2, 3)

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
            idempotencyKey = "key-cash-in",
            amount = Decimal.parse("500.0")
        )
        every { storage.findOpenShift("kkm-1") } returns ShiftInfo("shift-1", "kkm-1", 1L, ShiftStatus.OPEN, 1000L)
        val ofdCommandResult = OfdCommandResult(status = DomainOfdCommandStatus.OK, resultCode = 0)
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


        val result = api.cashIn("kkm-1", "1234", request)
        assertNotNull(result.documentId)
    }

    @Test
    fun `initKkm executes KKM initialization UseCases`() {
        every { timeValidator.validate(any()) } returns TimeValidationResult(true, null, null)
        every { idGenerator.nextId() } returns "kkm-new"
        every { storage.createKkm(any()) } returns true
        every { pinHasher.hash("1234") } returns "hash-1234"
        every { storage.createUser(any(), any(), any(), any(), any(), any()) } returns true

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
        assertEquals("kkm-new", kkm.kkmId)
    }

    @Test
    fun `initKkmSimple initializes KKM with defaults`() {
        every { timeValidator.validate(any()) } returns TimeValidationResult(true, null, null)
        every { idGenerator.nextId() } returns "kkm-simple"
        every { storage.createKkm(any()) } returns true
        every { pinHasher.hash("1234") } returns "hash-1234"
        every { storage.createUser(any(), any(), any(), any(), any(), any()) } returns true

        val request = KkmInitSimpleRequest(
            ofdId = "kazakhtelecom",
            ofdEnvironment = "test",
            ofdSystemId = "200367",
            ofdToken = "32876190",
            okved = "47110"
        )
        val kkm = api.initKkmSimple("0000", request)
        assertEquals("kkm-simple", kkm.kkmId)
    }

    @Test
    fun `initKkmSimple handles exceptions`() {
        every { timeValidator.validate(any()) } returns TimeValidationResult(true, null, null)
        every { idGenerator.nextId() } returns "kkm-simple-err"
        every { storage.findKkmBySystemId("err-sys") } throws RuntimeException("Storage failure")

        val request = KkmInitSimpleRequest(
            ofdId = "kazakhtelecom",
            ofdEnvironment = "test",
            ofdSystemId = "err-sys",
            ofdToken = "32876190"
        )
        kotlin.test.assertFailsWith<RuntimeException> {
            api.initKkmSimple("0000", request)
        }
    }

    @Test
    fun `initKkm handles exceptions`() {
        every { timeValidator.validate(any()) } returns TimeValidationResult(true, null, null)
        every { storage.findKkmByRegistrationNumber(any()) } throws RuntimeException("Storage failure")

        val request = KkmInitDirectRequest(
            ofdId = "kazakhtelecom",
            ofdEnvironment = "test",
            ofdSystemId = "200367",
            ofdToken = "32876190",
            kkmKgdId = "123",
            factoryNumber = "F123",
            manufactureYear = 2026
        )
        kotlin.test.assertFailsWith<RuntimeException> {
            api.initKkm("0000", request)
        }
    }

    @Test
    fun `getKkm and listKkms handle inner exceptions gracefully`() {
        val kkm = testKkmInfo
        every { storage.findKkm("kkm-1") } returns kkm
        every { storage.findOpenShift("kkm-1") } returns null
        every { storage.listQueueTasksByCashbox(any(), any(), any(), any()) } throws RuntimeException("Tasks error")

        val resp = api.getKkm("kkm-1")
        assertEquals("kkm-1", resp.kkmId)
        assertEquals(0, resp.offlineQueueCount)

        every { storage.listKkms(any(), any(), any(), any(), any(), any()) } returns listOf(kkm)
        val listResp = api.listKkms(KkmListParams(limit = 10, offset = 0))
        assertEquals(1, listResp.items.size)
    }

    @Test
    fun `setLogLevel and setLogListener update LoggerConfig`() {
        api.setLogLevel("debug")
        api.setLogLevel("invalid_level")
        api.setLogListener(object : io.github.texport.superkassa.core.domain.impl.logging.LogListener {
            override fun onLog(levelName: String, tag: String, message: String) {}
        })
    }

    @Test
    fun `validateCanDeleteKkm delegates to validateCanDeleteKkmImpl`() {
        every { timeValidator.validate(any()) } returns TimeValidationResult(true, null, null)
        every { storage.findKkm("kkm-del") } returns testKkmInfo.copy(id = "kkm-del", state = "PROGRAMMING")
        every { pinHasher.hash("1234") } returns "hash-pin"
        every { storage.findUserByPin("kkm-del", "hash-pin") } returns adminUser
        every { storage.findOpenShift("kkm-del") } returns null
        every { queue.canSendDirectly("kkm-del") } returns true
        every { storage.countOfflineQueue() } returns 0

        val canDel = api.validateCanDeleteKkm("kkm-del", "1234")
        assertTrue(canDel)
    }

    @Test
    fun `openShift and closeShift handle exceptions`() {
        every { timeValidator.validate(any()) } returns TimeValidationResult(true, null, null)
        every { storage.findKkmForUpdate("kkm-err") } throws RuntimeException("Storage failure")

        kotlin.test.assertFailsWith<RuntimeException> {
            api.openShift("kkm-err", "0000")
        }
        kotlin.test.assertFailsWith<RuntimeException> {
            api.closeShift("kkm-err", "0000")
        }
        kotlin.test.assertFailsWith<RuntimeException> {
            api.getOpenShift("kkm-err", "0000")
        }
        kotlin.test.assertFailsWith<RuntimeException> {
            api.createReport("kkm-err", "0000")
        }
    }

    @Test
    fun `receipt creation methods handle exceptions`() {
        every { timeValidator.validate(any()) } returns TimeValidationResult(true, null, null)
        every { storage.findKkmForUpdate("kkm-err") } throws RuntimeException("Storage failure")

        val sellReq = ReceiptSellRequest(idempotencyKey = "key", items = emptyList(), payments = emptyList())
        kotlin.test.assertFailsWith<RuntimeException> {
            api.createSellReceipt("kkm-err", "0000", sellReq)
        }
        val sellReturnReq = ReceiptSellReturnRequest(idempotencyKey = "key", items = emptyList(), payments = emptyList())
        kotlin.test.assertFailsWith<RuntimeException> {
            api.createSellReturnReceipt("kkm-err", "0000", sellReturnReq)
        }
        val buyReq = ReceiptBuyRequest(idempotencyKey = "key", items = emptyList(), payments = emptyList())
        kotlin.test.assertFailsWith<RuntimeException> {
            api.createBuyReceipt("kkm-err", "0000", buyReq)
        }
        val buyReturnReq = ReceiptBuyReturnRequest(idempotencyKey = "key", items = emptyList(), payments = emptyList())
        kotlin.test.assertFailsWith<RuntimeException> {
            api.createBuyReturnReceipt("kkm-err", "0000", buyReturnReq)
        }
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
        assertEquals("MIXED", result.taxRegime)
        assertEquals("VAT_16", result.defaultVatGroup)
    }

    @Test
    fun `updateBrandingSettings updates branding`() {
        val programmingKkm = testKkmInfo.copy(state = "PROGRAMMING", mode = "PROGRAMMING")
        every { storage.findKkm("kkm-1") } returns programmingKkm
        every { pinHasher.hash("1234") } returns "hash-admin"
        every { storage.findUserByPin("kkm-1", "hash-admin") } returns adminUser
        every { timeValidator.validate(any()) } returns TimeValidationResult(true, null, null)
        every { storage.updateKkm(any()) } returns true

        val branding = ReceiptBrandingRequest(headerMsg = "Super Title")
        val result = api.updateBrandingSettings("kkm-1", "1234", branding)
        assertEquals(branding.headerMsg, result.branding?.headerMsg)
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
        val ofdCommandResult = OfdCommandResult(status = DomainOfdCommandStatus.OK, resultCode = 0)
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
        val ofdCommandResult = OfdCommandResult(status = DomainOfdCommandStatus.OK, resultCode = 0)
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
        val ofdCommandResult = OfdCommandResult(status = DomainOfdCommandStatus.OK, resultCode = 0)
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
        // Покупка платит из ящика, и касса проверяет остаток пересчётом
        // сменных счётчиков. Здесь проверяется проброс вызова, а не деньги,
        // поэтому в ящике заведомо достаточно, а документов смены нет.
        every { storage.loadCounters("kkm-1", any(), any()) } returns mapOf("start_shift_cash.sum" to 1_000_00L)
        every { storage.listFiscalDocumentsByShift("kkm-1", "shift-1", any(), any()) } returns emptyList()
        every { storage.upsertCounter("kkm-1", any(), any(), any(), any()) } returns true

        val sellReq = ReceiptSellRequest(
            items = listOf(ReceiptItemRequest(name = "Item 1", price = Decimal.parse("10.0"), quantity = Decimal.parse("1.0"), vatGroup = "VAT_16", measureUnitCode = "796")),
            payments = listOf(ReceiptPaymentRequest("CASH", Decimal.parse("10.0"))),
            idempotencyKey = "sell-key"
        )
        val resSell = api.createSellReceipt("kkm-1", "1234", sellReq)
        assertEquals("doc-new", resSell.documentId)

        val testCommand = CreateReceiptCommand(
            kkmId = "kkm-1",
            pin = "1234",
            operation = "SELL",
            idempotencyKey = "direct-key",
            items = listOf(ReceiptItemRequest(name = "Item 1", price = Decimal.parse("10.0"), quantity = Decimal.parse("1.0"))),
            payments = listOf(ReceiptPaymentRequest("CASH", Decimal.parse("10.0"))),
            discountPercent = null,
            discountSum = null,
            markupPercent = null,
            markupSum = null,
            taken = Decimal.parse("10.0")
        )
        val resDirect = api.createReceipt(testCommand)
        assertEquals("doc-new", resDirect.documentId)

        val parentTicket = ParentTicketRequest(
            parentTicketNumber = 123L,
            parentTicketDateTime = "2026-06-27T16:00:00Z",
            kgdKkmId = "kgd-1",
            parentTicketTotal = Decimal.parse("10.0"),
            parentTicketIsOffline = false
        )

        val sellRetReq = ReceiptSellReturnRequest(
            items = listOf(ReceiptItemRequest(name = "Item 1", price = Decimal.parse("10.0"), quantity = Decimal.parse("1.0"), vatGroup = "VAT_16", measureUnitCode = "796")),
            payments = listOf(ReceiptPaymentRequest("CASH", Decimal.parse("10.0"))),
            idempotencyKey = "sell-ret-key",
            parentTicket = parentTicket
        )
        val resSellRet = api.createSellReturnReceipt("kkm-1", "1234", sellRetReq)
        assertEquals("doc-new", resSellRet.documentId)

        val buyReq = ReceiptBuyRequest(
            items = listOf(ReceiptItemRequest(name = "Item 1", price = Decimal.parse("10.0"), quantity = Decimal.parse("1.0"), vatGroup = "VAT_16", measureUnitCode = "796")),
            payments = listOf(ReceiptPaymentRequest("CASH", Decimal.parse("10.0"))),
            idempotencyKey = "buy-key"
        )
        val resBuy = api.createBuyReceipt("kkm-1", "1234", buyReq)
        assertEquals("doc-new", resBuy.documentId)

        val buyRetReq = ReceiptBuyReturnRequest(
            items = listOf(ReceiptItemRequest(name = "Item 1", price = Decimal.parse("10.0"), quantity = Decimal.parse("1.0"), vatGroup = "VAT_16", measureUnitCode = "796")),
            payments = listOf(ReceiptPaymentRequest("CASH", Decimal.parse("10.0"))),
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
            idempotencyKey = "key-cash-out",
            amount = Decimal.parse("100.0")
        )
        every { storage.findOpenShift("kkm-1") } returns ShiftInfo("shift-1", "kkm-1", 1L, ShiftStatus.OPEN, 1000L)
        // Остаток наличных берётся тем же расчётом, что и в отчётах: он начинает
        // с остатка на начало смены и прибавляет документы. Поэтому фикстура
        // задаёт start_shift_cash.sum, а не готовый CASH_SUM.
        every { storage.loadCounters("kkm-1", "SHIFT", "shift-1") } returns
            mapOf("cash.sum" to 15000L, "start_shift_cash.sum" to 15000L)
        every { storage.listFiscalDocumentsByShift("kkm-1", "shift-1", any(), any()) } returns emptyList()
        every { storage.upsertCounter("kkm-1", any(), any(), any(), any()) } returns true
        val ofdCommandResult = OfdCommandResult(status = DomainOfdCommandStatus.OK, resultCode = 0)
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


        val result = api.cashOut("kkm-1", "1234", request)
        assertNotNull(result.documentId)
    }

    @Test
    fun `shift operations open close get list shifts and list docs`() {
        every { storage.findKkm("kkm-1") } returns testKkmInfo
        every { timeValidator.validate(any()) } returns TimeValidationResult(true, null, null)
        every { pinHasher.hash("1234") } returns "hash-admin"
        every { storage.findUserByPin("kkm-1", "hash-admin") } returns adminUser
        every { idGenerator.nextId() } returns "shift-new"
        every { queue.canSendDirectly("kkm-1") } returns true
        val ofdCommandResult = OfdCommandResult(status = DomainOfdCommandStatus.OK, resultCode = 0)
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
        val domainShift = ShiftInfo("shift-new", "kkm-1", 1L, io.github.texport.superkassa.core.domain.api.model.shift.ShiftStatus.OPEN, 1000L)
        every { storage.listShifts("kkm-1", 10, 0) } returns listOf(domainShift)
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

        // Документ с составом: возврату нужны позиции, разбору отказа — кассир
        val soldItem = DomainReceiptItem(
            name = "Кофе",
            sectionCode = "001",
            quantity = 3_000L,
            price = DomainMoney.fromTiyn(15055L),
            sum = DomainMoney.fromTiyn(45165L),
            vatGroup = DomainVatGroup.VAT_16
        )
        val soldReceipt = DomainReceiptRequest(
            kkmId = "kkm-1",
            pin = "1234",
            operation = DomainOperationType.SELL,
            items = listOf(soldItem),
            payments = emptyList(),
            total = DomainMoney.fromTiyn(45165L),
            idempotencyKey = "key-details",
            operatorName = "Айгүл"
        )
        every { storage.findFiscalDocumentWithReceiptPayload("doc-1") } returns (mockSnapshot to soldReceipt)
        val details = api.getDocumentDetails("kkm-1", "doc-1", "1234")
        assertEquals(1, details.items.size)
        assertEquals(3_000L, details.items.first().quantityThousandths)
        assertEquals("Айгүл", details.operatorName)

        // Документа нет — отказ, а не пустой ответ
        every { storage.findFiscalDocumentWithReceiptPayload("doc-missing") } returns null
        every { storage.findFiscalDocumentById("doc-missing") } returns null
        assertFailsWith<NotFoundException> { api.getDocumentDetails("kkm-1", "doc-missing", "1234") }

        // Отчёт: состава нет, и это не ошибка
        every { storage.findFiscalDocumentWithReceiptPayload("doc-report") } returns null
        every { storage.findFiscalDocumentById("doc-report") } returns mockSnapshot
        assertEquals(0, api.getDocumentDetails("kkm-1", "doc-report", "1234").items.size)

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
            items = listOf(ReceiptItemRequest(name = "Item 1", price = Decimal.parse("10.0"), quantity = Decimal.parse("1.0"), vatGroup = "VAT_16", measureUnitCode = "796")),
            payments = listOf(ReceiptPaymentRequest("CASH", Decimal.parse("10.0"))),
            idempotencyKey = "sell-key-offline"
        )
        val resSell = api.createSellReceipt("kkm-1", "1234", sellReq)
        // В автономном режиме чек попадает в очередь, а не доставляется:
        // сообщать об успешной доставке здесь значит вводить кассира в заблуждение.
        assertEquals(DeliveryStatus.OFFLINE_QUEUED, resSell.deliveryStatus)

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
            items = listOf(ReceiptItemRequest(name = "Item 1", price = Decimal.parse("10.0"), quantity = Decimal.parse("1.0"), vatGroup = "VAT_16", measureUnitCode = "796")),
            payments = listOf(ReceiptPaymentRequest("CASH", Decimal.parse("10.0"))),
            idempotencyKey = "sell-key-closed"
        )
        assertFailsWith<ConflictException> {
            api.createSellReceipt("kkm-1", "1234", sellReq)
        }

        // 2. OFD TIMEOUT and FAILED checks for createReport
        every { pinHasher.hash("1234") } returns "hash-admin"
        every { storage.findUserByPin("kkm-1", "hash-admin") } returns adminUser
        
        // Timeout check
        every { ofd.send(any()) } returns OfdCommandResult(status = DomainOfdCommandStatus.TIMEOUT, errorMessage = "Timeout")
        val resTimeout = api.createReport("kkm-1", "1234")
        assertEquals(DeliveryStatus.OFFLINE_QUEUED, resTimeout.deliveryStatus)
        assertEquals("Timeout", resTimeout.deliveryError)

        // Failed check
        every { ofd.send(any()) } returns OfdCommandResult(status = DomainOfdCommandStatus.FAILED, errorMessage = "Failed")
        val resFailed = api.createReport("kkm-1", "1234")
        assertEquals(DeliveryStatus.ONLINE_ERROR, resFailed.deliveryStatus)
        assertEquals("Failed", resFailed.deliveryError)
    }

    @Test
    fun `lookupNomenclature executes nomenclature lookup successfully`() {
        every { storage.findKkm("kkm-1") } returns testKkmInfo
        every { pinHasher.hash("1234") } returns "hash-1"
        every { storage.findUserByPin("kkm-1", "hash-1") } returns testUser

        val responseJson = kotlinx.serialization.json.Json.parseToJsonElement(
            """{
                "payload": {
                    "nomenclature": {
                        "result": { "code": 0 },
                        "elements": [
                            {
                                "id": 123,
                                "title": "Soda",
                                "titleKk": "Soda Kk",
                                "item": {
                                    "ntin": "ntin-123",
                                    "barcode": "5449000176431",
                                    "sellPrice": {
                                        "bills": 150,
                                        "coins": 0
                                    },
                                    "measureUnitCode": "163",
                                    "taxes": [
                                        {
                                            "taxType": "VAT",
                                            "taxPercent": 16
                                        }
                                    ]
                                }
                            }
                        ]
                    }
                }
            }"""
        ) as kotlinx.serialization.json.JsonObject

        val ofdCommandResult = OfdCommandResult(
            status = DomainOfdCommandStatus.OK,
            resultCode = 0,
            responseJson = responseJson
        )
        every { ofd.send(any()) } returns ofdCommandResult

        val response = api.lookupNomenclature("1234", NomenclatureLookupRequest("kkm-1", "5449000176431"))
        assertEquals("OK", response.resultText)
        assertTrue(response.found)
        assertEquals(0, response.resultCode)
        val item = response.item
        assertNotNull(item)
        assertEquals(123L, item.id)
        assertEquals("5449000176431", item.barcode)
        assertEquals("Soda", item.name)
        assertEquals("Soda Kk", item.nameKk)
        assertEquals(Decimal.parse("150.0"), item.price)
        assertEquals("163", item.measureUnitCode)
        assertEquals("VAT_16", item.vatGroup)
    }

    @Test
    fun testOfflineQueueApi() {
        every { queue.canSendDirectly("kkm-1") } returns true
        every { queue.processOfflineBatch("kkm-1", 10) } returns 5

        every { storage.listQueueTasksByCashbox("kkm-1", "OFFLINE", limit = 500) } returns listOf(
            io.github.texport.superkassa.core.domain.api.model.queue.QueueTask(
                id = "1", cashboxId = "kkm-1", lane = "OFFLINE", type = "TICKET", payloadRef = "ref-1",
                createdAt = 1000L, status = "PENDING", attempt = 0, nextAttemptAt = null, lastError = null
            ),
            io.github.texport.superkassa.core.domain.api.model.queue.QueueTask(
                id = "2", cashboxId = "kkm-1", lane = "OFFLINE", type = "TICKET", payloadRef = "ref-2",
                createdAt = 2000L, status = "FAILED", attempt = 1, nextAttemptAt = null, lastError = null
            ),
            io.github.texport.superkassa.core.domain.api.model.queue.QueueTask(
                id = "3", cashboxId = "kkm-1", lane = "OFFLINE", type = "TICKET", payloadRef = "ref-3",
                createdAt = 3000L, status = "SENT", attempt = 1, nextAttemptAt = null, lastError = null
            )
        )

                val listQueueItemsUseCase = mockk<ListQueueItemsUseCase>()
        val retryFailedQueueItemsUseCase = mockk<RetryFailedQueueItemsUseCase>()
        val queueApi: OfflineQueueApi = OfflineQueueApiImpl(
            queue,
            GetQueueStatusUseCase(storage),
            listQueueItemsUseCase,
            retryFailedQueueItemsUseCase
        )

        every { listQueueItemsUseCase.execute("kkm-1", "1234") } returns listOf(
            ListQueueItemsUseCase.QueueItemView(
                id = "1", lane = "OFFLINE", type = "TICKET", status = "PENDING", attempt = 0, nextAttemptAt = null, lastError = null
            )
        )
        every { retryFailedQueueItemsUseCase.execute("kkm-1", "1234") } returns 1

        assertTrue(queueApi.canSendDirectly("kkm-1"))
        assertEquals(5, queueApi.processOfflineBatch("kkm-1"))

        val status = queueApi.getQueueStatus(QueueStatusRequest("kkm-1"))
        assertTrue(status.hasPendingItems)
        assertEquals(2, status.pendingCount)

        val queueList = queueApi.listQueue("kkm-1", "1234")
        assertEquals(1, queueList.size)
        assertEquals("1", queueList.first().id)

        val retryCount = queueApi.retryFailed("kkm-1", "1234")
        assertEquals(1, retryCount)

        val json = kotlinx.serialization.json.Json.encodeToString(QueueStatusResponse.serializer(), status)
        val decoded = kotlinx.serialization.json.Json.decodeFromString(QueueStatusResponse.serializer(), json)
        assertEquals(status, decoded)
    }

    @Test
    fun testNewReferences() {
        assertEquals(3, api.getOfdEnvironments().size)
        assertEquals("DEV", api.getOfdEnvironments()[0].code)
        assertEquals("TEST", api.getOfdEnvironments()[1].code)
        assertEquals("PROD", api.getOfdEnvironments()[2].code)

        // Справочник провайдеров повторяет доменный OfdProvider целиком:
        // приложению кассы незачем держать собственный список.
        assertEquals(2, api.getOfdProviders().size)
        assertEquals("KAZAKHTELECOM", api.getOfdProviders()[0].code)
        assertEquals("oofd.kz", api.getOfdProviders()[0].website)
        assertEquals("BFD", api.getOfdProviders()[1].code)
        assertEquals("ОФД БФД", api.getOfdProviders()[1].name.ru)
        assertEquals("БФД ОФД", api.getOfdProviders()[1].name.kk)

        assertEquals(2, api.getCoreModes().size)
        assertEquals("DESKTOP", api.getCoreModes()[0].code)
        assertEquals("SERVER", api.getCoreModes()[1].code)

        assertEquals(2, api.getAuthModes().size)
        assertEquals("NONE", api.getAuthModes()[0].code)
        assertEquals("BEARER", api.getAuthModes()[1].code)

        assertEquals(3, api.getReceiptLanguages().size)
        assertEquals("RU", api.getReceiptLanguages()[0].code)
        assertEquals("KK", api.getReceiptLanguages()[1].code)
        assertEquals("MIXED", api.getReceiptLanguages()[2].code)

        assertEquals(3, api.getReceiptLayoutTypes().size)
        assertEquals("TAPE_80MM", api.getReceiptLayoutTypes()[0].code)
        assertEquals("TAPE_58MM", api.getReceiptLayoutTypes()[1].code)
        assertEquals("FULLSCREEN", api.getReceiptLayoutTypes()[2].code)

        assertEquals(4, api.getPrintDocumentTypes().size)
        assertEquals("DOCUMENT", api.getPrintDocumentTypes()[0].code)
        assertEquals("X_REPORT", api.getPrintDocumentTypes()[1].code)
        assertEquals("OPEN_SHIFT", api.getPrintDocumentTypes()[2].code)
        assertEquals("CLOSE_SHIFT", api.getPrintDocumentTypes()[3].code)

        assertEquals(7, api.getOfdCommandTypes().size)
        assertEquals("TICKET", api.getOfdCommandTypes()[0].code)

        assertEquals(2, api.getCashOperationTypes().size)
        assertEquals("CASH_IN", api.getCashOperationTypes()[0].code)
        assertEquals("CASH_OUT", api.getCashOperationTypes()[1].code)
    }

    @Test
    fun testPaymentTypesCarryProtocolSupport() {
        // Схема 2.0.4 не содержит кредита и тары: справочник обязан сказать
        // об этом заранее, а не отказом уже пробитого чека.
        every { coreSettings.ofdProtocolVersion } returns "204"
        val apiOn204 = SuperkassaApiImpl(
            storage = storage,
            queuePort = queue,
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
            timeValidator = timeValidator,
            printApi = printApi
        )

        val types = apiOn204.getPaymentTypes().associateBy { it.code }
        assertEquals(6, types.size)
        assertEquals(true, types.getValue("CASH").supported)
        assertEquals(false, types.getValue("CREDIT").supported)
        assertEquals(false, types.getValue("TARE").supported)
        // Названия заведены на всех трёх языках, а не оставлены голым кодом.
        assertEquals("Оплата в кредит", types.getValue("CREDIT").name.ru)
        assertEquals("Несиеге төлеу", types.getValue("CREDIT").name.kk)
        assertEquals("Credit Payment", types.getValue("CREDIT").name.en)
        assertEquals("Оплата тарой", types.getValue("TARE").name.ru)
        assertEquals("Ыдыспен төлеу", types.getValue("TARE").name.kk)
        assertEquals("Payment by Tare", types.getValue("TARE").name.en)
    }

    @Test
    fun testReceiptDomainTypesReference() {
        val kinds = api.getReceiptDomainTypes()
        assertEquals(6, kinds.size)
        assertEquals("DOMAIN_TRADING", kinds[0].code)
        assertEquals("Торговля", kinds[0].name.ru)
        assertEquals("Сауда", kinds[0].name.kk)
        assertEquals("Trading", kinds[0].name.en)
        assertEquals("DOMAIN_PARKING", kinds[5].code)
        assertEquals("Тұрақ", kinds[5].name.kk)
    }

    @Test
    fun testVatGroupsReference() {
        val rates = api.listVatRates().associateBy { it.code }
        assertEquals(6, rates.size)
        assertEquals(16, rates.getValue("VAT_16").percent)
        assertEquals("ҚҚС 16%", rates.getValue("VAT_16").name.kk)
        // Ставка до 2026 года: без неё нечем пробить возврат по старому чеку.
        assertEquals(12, rates.getValue("VAT_12").percent)
        assertEquals("ҚҚС 12%", rates.getValue("VAT_12").name.kk)
    }

    @Test
    fun testOtherReferencesAndAuth() {
        assertNotNull(api.getPaymentTypes())
        assertNotNull(api.getDocumentTypes())
        assertNotNull(api.getUserRoles())
        assertNotNull(api.getTaxRegimes())
        assertNotNull(api.getPaperWidths())
        assertNotNull(api.getBrandingColors())
        assertNotNull(api.getKkmStates())
        assertNotNull(api.getKkmModes())
        assertNotNull(api.getShiftStatuses())
        assertNotNull(api.getDeliveryStatuses())
        assertNotNull(api.getOfdCommandStatuses())
        assertNotNull(api.getReceiptOperationTypes())

        every { storage.findKkm("kkm-1") } returns testKkmInfo
        every { pinHasher.hash("1234") } returns "hash1234"
        every { storage.findUserByPin("kkm-1", "hash1234") } returns KkmUser(
            id = "user-1",
            name = "Cashier",
            role = io.github.texport.superkassa.core.domain.api.model.auth.UserRole.CASHIER,
            createdAt = 123456789L
        )

        val authResponse = api.authenticate("kkm-1", "1234")
        assertEquals("user-1", authResponse.userId)

        every { storage.findUserByPin("kkm-1", "hash1234") } returns null
        assertFailsWith<io.github.texport.superkassa.core.domain.api.exception.ForbiddenException> {
            api.authenticate("kkm-1", "1234")
        }
    }
}

/** Момент, к которому 72 часа автономной работы заведомо прошли. */
private const val AUTONOMOUS_LIMIT_PASSED = 400L * 60L * 60L * 1000L
