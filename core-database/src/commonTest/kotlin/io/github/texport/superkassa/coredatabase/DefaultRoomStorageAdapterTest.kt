package io.github.texport.superkassa.coredatabase

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmMode
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmState
import io.github.texport.superkassa.coredatabase.api.RoomStorageFactory
import io.github.texport.superkassa.offlinequeue.api.model.QueueCommand
import io.github.texport.superkassa.offlinequeue.api.model.QueueCommandType
import io.github.texport.superkassa.offlinequeue.api.model.QueueLane
import io.github.texport.superkassa.offlinequeue.api.model.QueueStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefaultRoomStorageAdapterTest {

    @Test
    fun testKkmFullLifecycle() {
        val pair = RoomStorageFactory.createDefaultStorage()
        val storage = pair.storagePort

        val now = 10000L
        val kkm = KkmInfo(
            id = "kkm-test-1",
            createdAt = now,
            updatedAt = now,
            mode = KkmMode.REGISTRATION.name,
            state = KkmState.ACTIVE.name,
            registrationNumber = "REG-999888",
            factoryNumber = "FN-777",
            autoCloseShift = true,
            autoCashout = true
        )

        // 1. Create KKM
        assertTrue(storage.createKkm(kkm))

        // 2. Find by ID & System ID
        val foundById = storage.findKkm("kkm-test-1")
        assertNotNull(foundById)
        assertEquals("REG-999888", foundById.registrationNumber)

        val foundBySystemId = storage.findKkmBySystemId("kkm-test-1")
        assertNotNull(foundBySystemId)
        assertEquals("FN-777", foundBySystemId.factoryNumber)

        val foundForUpdate = storage.findKkmForUpdate("kkm-test-1")
        assertNotNull(foundForUpdate)

        // 3. Find by Registration Number
        val foundByRegNum = storage.findKkmByRegistrationNumber("REG-999888")
        assertNotNull(foundByRegNum)
        assertEquals("kkm-test-1", foundByRegNum.id)

        // 4. List and Count KKM
        val list = storage.listKkms(10, 0, null, null, "createdAt", "ASC")
        assertTrue(list.any { it.id == "kkm-test-1" })

        val count = storage.countKkms(null, null)
        assertTrue(count >= 1)

        // 5. Update KKM
        val updatedKkm = kkm.copy(factoryNumber = "FN-777-UPDATED")
        assertTrue(storage.updateKkm(updatedKkm))
        val fetchedUpdated = storage.findKkm("kkm-test-1")
        assertNotNull(fetchedUpdated)
        assertEquals("FN-777-UPDATED", fetchedUpdated.factoryNumber)

        // 6. Delete KKM
        assertTrue(storage.deleteKkm("kkm-test-1"))
        assertNull(storage.findKkm("kkm-test-1"))
    }

    @Test
    fun testKkmNameIsStoredAndRead() {
        val storage = RoomStorageFactory.createDefaultStorage().storagePort

        val now = 20000L
        val kkm = KkmInfo(
            id = "kkm-named",
            createdAt = now,
            updatedAt = now,
            mode = KkmMode.REGISTRATION.name,
            state = KkmState.ACTIVE.name,
            registrationNumber = "260940000021",
            name = "Касса 2 на Достык"
        )
        assertTrue(storage.createKkm(kkm))
        assertEquals("Касса 2 на Достык", storage.findKkm("kkm-named")?.name)

        assertTrue(storage.updateKkm(kkm.copy(name = "Касса у входа")))
        assertEquals("Касса у входа", storage.findKkm("kkm-named")?.name)

        assertTrue(storage.updateKkm(kkm.copy(name = null)))
        assertNull(storage.findKkm("kkm-named")?.name)

        assertTrue(storage.deleteKkm("kkm-named"))
    }

    @Test
    fun testKkmWithoutNameStaysWithoutIt() {
        val storage = RoomStorageFactory.createDefaultStorage().storagePort

        val kkm = KkmInfo(
            id = "kkm-unnamed",
            createdAt = 30000L,
            updatedAt = 30000L,
            mode = KkmMode.REGISTRATION.name,
            state = KkmState.ACTIVE.name,
            registrationNumber = "260940000020"
        )
        assertTrue(storage.createKkm(kkm))
        assertNull(storage.findKkm("kkm-unnamed")?.name)
        assertTrue(storage.deleteKkm("kkm-unnamed"))
    }

    @Test
    fun testDocumentPersistenceAndQueries() {
        val pair = RoomStorageFactory.createDefaultStorage()
        val storage = pair.storagePort

        val now = 100000L
        val req = io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest(
            kkmId = "kkm-doc-1",
            pin = "1111",
            operation = io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType.BUY,
            items = emptyList(),
            payments = emptyList(),
            total = io.github.texport.superkassa.core.domain.api.model.common.Money.fromTenge(Decimal.parse("1000.0")),
            idempotencyKey = "idemp-1"
        )

        // Save receipt & cash operation & shift document
        assertTrue(storage.saveReceipt(req, "doc-1", "shift-10", now))
        assertTrue(storage.saveCashOperation("kkm-doc-1", "CASH_IN", io.github.texport.superkassa.core.domain.api.model.common.Money.fromTenge(Decimal.parse("500.0")), "doc-2", "shift-10", now + 10))
        assertTrue(storage.saveShiftDocument("kkm-doc-1", "REPORT_X", "doc-3", "shift-10", now + 20))

        // Find by ID
        val doc1 = storage.findFiscalDocumentById("doc-1")
        assertNotNull(doc1)
        assertEquals("BUY", doc1.docType)

        // Find with receipt payload
        val pairPayload = storage.findFiscalDocumentWithReceiptPayload("doc-1")
        assertNotNull(pairPayload)
        assertEquals("idemp-1", pairPayload.second.idempotencyKey)

        // Update status & docNo
        assertTrue(storage.updateReceiptStatus("doc-1", "FS-123", "AS-123", "SENT", null, now + 100, false))
        assertTrue(storage.updateDocumentNumber("doc-1", 777L))
        // Печатный номер касса ведёт сама, отдельно от номера, присвоенного ОФД.
        assertTrue(storage.updatePrintedDocumentNumber("doc-1", 42L))

        val updatedDoc1 = storage.findFiscalDocumentById("doc-1")
        assertNotNull(updatedDoc1)
        assertEquals(777L, updatedDoc1.docNo)
        assertEquals(42L, updatedDoc1.printedDocumentNumber)
        assertEquals("FS-123", updatedDoc1.fiscalSign)
        assertEquals("SENT", updatedDoc1.ofdStatus)

        // List by period & shift
        val periodDocs = storage.listFiscalDocumentsByPeriod("kkm-doc-1", now - 100, now + 500, 10, 0)
        assertEquals(3, periodDocs.size)

        val shiftDocs = storage.listFiscalDocumentsByShift("kkm-doc-1", "shift-10", 10, 0)
        assertEquals(3, shiftDocs.size)

        val countAll = storage.countFiscalDocuments(null)
        assertEquals(3L, countAll)

        val countReportX = storage.countFiscalDocuments("REPORT_X")
        assertEquals(1L, countReportX)
    }

    @Test
    fun testUserManagementLifecycle() {
        val pair = RoomStorageFactory.createDefaultStorage()
        val storage = pair.storagePort

        val now = 50000L
        assertTrue(storage.createUser("kkm-usr-1", "usr-1", "Admin", io.github.texport.superkassa.core.domain.api.model.auth.UserRole.ADMIN, "hash0000", now))

        val user = storage.findUserById("kkm-usr-1", "usr-1")
        assertNotNull(user)
        assertEquals("Admin", user.name)

        val userByPin = storage.findUserByPin("kkm-usr-1", "hash0000")
        assertNotNull(userByPin)
        assertEquals("usr-1", userByPin.id)

        val users = storage.listUsers("kkm-usr-1")
        assertEquals(1, users.size)

        assertTrue(storage.updateUser("kkm-usr-1", "usr-1", "SuperAdmin", null, null))
        val updatedUser = storage.findUserById("kkm-usr-1", "usr-1")
        assertNotNull(updatedUser)
        assertEquals("SuperAdmin", updatedUser.name)

        assertTrue(storage.deleteUser("kkm-usr-1", "usr-1"))
        assertNull(storage.findUserById("kkm-usr-1", "usr-1"))
    }

    @Test
    fun testCountersAndTokens() {
        val pair = RoomStorageFactory.createDefaultStorage()
        val storage = pair.storagePort

        val now = 60000L
        val kkm = KkmInfo(id = "kkm-cnt-1", createdAt = now, updatedAt = now, mode = KkmMode.REGISTRATION.name, state = KkmState.ACTIVE.name)
        storage.createKkm(kkm)

        assertTrue(storage.updateKkmToken("kkm-cnt-1", "enc-token-base64", now + 10))
        val tokenUpdated = storage.findKkm("kkm-cnt-1")
        assertNotNull(tokenUpdated)
        assertEquals("enc-token-base64", tokenUpdated.tokenEncryptedBase64)

        assertTrue(storage.upsertCounter("kkm-cnt-1", "SHIFT", "s-1", "SALES_COUNT", 5L))
        val loaded = storage.loadCounters("kkm-cnt-1", "SHIFT", "s-1")
        assertEquals(5L, loaded["SALES_COUNT"])

        val counterList = storage.listCounters("kkm-cnt-1")
        assertTrue(counterList.any { it.key == "SALES_COUNT" && it.value == 5L })

        assertTrue(storage.deleteKkmCompletely("kkm-cnt-1"))
        assertNull(storage.findKkm("kkm-cnt-1"))
    }

    @Test
    fun testQueueCommandFullLifecycle() {
        val pair = RoomStorageFactory.createDefaultStorage()
        val queue = pair.queueStoragePort

        val now = 20000L
        val cmd1 = QueueCommand(
            id = "cmd-test-1",
            cashboxId = "cashbox-100",
            lane = QueueLane.OFFLINE,
            type = QueueCommandType.TICKET,
            payloadRef = "payload-ref-1",
            createdAt = now,
            status = QueueStatus.PENDING,
            attempt = 0
        )

        val cmd2 = QueueCommand(
            id = "cmd-test-2",
            cashboxId = "cashbox-100",
            lane = QueueLane.OFFLINE,
            type = QueueCommandType.CLOSE_SHIFT,
            payloadRef = "payload-ref-2",
            createdAt = now + 10,
            status = QueueStatus.PENDING,
            attempt = 0
        )

        // 1. Enqueue commands
        assertTrue(queue.enqueue(cmd1))
        assertTrue(queue.enqueue(cmd2))

        // 2. Get commands by status
        val pendingCmds = queue.getCommandsByStatus("cashbox-100", QueueLane.OFFLINE, setOf(QueueStatus.PENDING))
        assertEquals(2, pendingCmds.size)

        // 3. Mark in progress
        assertTrue(queue.markInProgress("cmd-test-1", now + 50))

        val inProgressCmds = queue.getCommandsByStatus("cashbox-100", QueueLane.OFFLINE, setOf(QueueStatus.IN_PROGRESS))
        assertEquals(1, inProgressCmds.size)
        assertEquals("cmd-test-1", inProgressCmds.first().id)

        // 4. Update status with error and retry
        assertTrue(queue.updateStatus("cmd-test-1", QueueStatus.FAILED, 1, "Network timeout", now + 1000))
        val failedCmds = queue.getCommandsByStatus("cashbox-100", QueueLane.OFFLINE, setOf(QueueStatus.FAILED))
        assertEquals(1, failedCmds.size)
        assertEquals("Network timeout", failedCmds.first().lastError)

        // 5. List by cashbox with pagination
        val listed = queue.listByCashbox("cashbox-100", QueueLane.OFFLINE, 10, 0)
        assertEquals(2, listed.size)

        // 6. Delete by cashbox
        assertTrue(queue.deleteByCashbox("cashbox-100"))
        val afterDelete = queue.listByCashbox("cashbox-100", QueueLane.OFFLINE, 10, 0)
        assertTrue(afterDelete.isEmpty())
    }

    @Test
    fun testShiftOperationsAndLocks() {
        val pair = RoomStorageFactory.createDefaultStorage()
        val storage = pair.storagePort

        val now = 30000L
        val shift = io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo(
            id = "shift-100",
            kkmId = "kkm-shift-1",
            shiftNo = 5L,
            openedAt = now,
            status = io.github.texport.superkassa.core.domain.api.model.shift.ShiftStatus.OPEN
        )

        assertTrue(storage.createShift(shift))
        val openShift = storage.findOpenShift("kkm-shift-1")
        assertNotNull(openShift)
        assertEquals("shift-100", openShift.id)

        val foundShift = storage.findShiftById("shift-100")
        assertNotNull(foundShift)

        val shifts = storage.listShifts("kkm-shift-1", 10, 0)
        assertEquals(1, shifts.size)

        assertTrue(storage.closeShift("shift-100", io.github.texport.superkassa.core.domain.api.model.shift.ShiftStatus.CLOSED, now + 3600, "close-doc-1"))
        assertNull(storage.findOpenShift("kkm-shift-1"))

        // Queue locks
        assertTrue(storage.tryAcquireQueueLock("kkm-shift-1", "owner-1", now + 1000, now))
        assertTrue(storage.renewQueueLock("kkm-shift-1", "owner-1", now + 2000, now + 500))
        assertTrue(storage.releaseQueueLock("kkm-shift-1", "owner-1"))
    }

    @Test
    fun testQueueTaskAndIdempotency() {
        val pair = RoomStorageFactory.createDefaultStorage()
        val storage = pair.storagePort

        val task = io.github.texport.superkassa.core.domain.api.model.queue.QueueTask(
            id = "task-1",
            cashboxId = "kkm-1",
            lane = "OFFLINE",
            type = "TICKET",
            payloadRef = "ref-1",
            createdAt = 1000L,
            status = "PENDING",
            attempt = 0,
            nextAttemptAt = null,
            lastError = null
        )

        assertTrue(storage.enqueueQueueTask(task))
        assertTrue(storage.listQueueTasksByCashbox("kkm-1", "OFFLINE", 10, 0).isEmpty())
        assertTrue(storage.getQueueTasksByStatus("kkm-1", "OFFLINE", setOf("PENDING")).isEmpty())
        assertTrue(storage.updateQueueTaskStatus("task-1", "SENT", 1, null, null))
        assertTrue(storage.markQueueTaskInProgress("task-1", 1000L))
        assertTrue(storage.deleteQueueTasksByCashbox("kkm-1"))

        assertTrue(storage.insertIdempotency("kkm-1", "key-1", "BUY"))
        assertNull(storage.findIdempotencyResponse("kkm-1", "key-1"))
        assertTrue(storage.updateIdempotencyResponse("kkm-1", "key-1", "resp-ref"))
    }
}
