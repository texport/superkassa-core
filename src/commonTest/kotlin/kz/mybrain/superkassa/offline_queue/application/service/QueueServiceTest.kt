package kz.mybrain.superkassa.offline_queue.application.service

import kz.mybrain.superkassa.offline_queue.application.model.DispatchResult
import kz.mybrain.superkassa.offline_queue.application.model.DispatchStatus
import kz.mybrain.superkassa.offline_queue.application.model.QueueDispatchException
import kz.mybrain.superkassa.offline_queue.application.model.QueueErrorMessage
import kz.mybrain.superkassa.offline_queue.application.model.QueueErrorMessages
import kz.mybrain.superkassa.offline_queue.application.policy.BackoffPolicy
import kz.mybrain.superkassa.offline_queue.application.policy.DefaultBackoffPolicy
import kz.mybrain.superkassa.offline_queue.application.policy.TimeProvider
import kz.mybrain.superkassa.offline_queue.domain.model.QueueCommand
import kz.mybrain.superkassa.offline_queue.domain.model.QueueCommandType
import kz.mybrain.superkassa.offline_queue.domain.model.QueueLane
import kz.mybrain.superkassa.offline_queue.domain.model.QueueStatus
import kz.mybrain.superkassa.offline_queue.domain.port.LeaseLockPort
import kz.mybrain.superkassa.offline_queue.domain.port.QueueStoragePort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QueueServiceTest {

    private class StubStorage : QueueStoragePort {
        val commands = mutableListOf<QueueCommand>()
        var nextPendingResponse: QueueCommand? = null
        var nextPendingError: Throwable? = null
        var lastStatusUpdate: StatusUpdate? = null
        var lastInProgressId: String? = null
        var lastNextPendingParams: NextPendingParams? = null
        var markInProgressResult = true
        var updateStatusResult = true
        var hasPendingCommandsResult = true

        data class StatusUpdate(
            val id: String,
            val status: QueueStatus,
            val attempt: Int,
            val lastError: String?,
            val nextAttemptAt: Long?
        )

        data class NextPendingParams(val cashboxId: String, val lane: QueueLane, val now: Long)

        override fun enqueue(command: QueueCommand): Boolean {
            commands.add(command)
            return true
        }

        override fun nextPending(cashboxId: String, lane: QueueLane, now: Long): QueueCommand? {
            lastNextPendingParams = NextPendingParams(cashboxId, lane, now)
            nextPendingError?.let { throw it }
            return nextPendingResponse
        }

        override fun updateStatus(
            id: String,
            status: QueueStatus,
            attempt: Int,
            lastError: String?,
            nextAttemptAt: Long?
        ): Boolean {
            lastStatusUpdate = StatusUpdate(id, status, attempt, lastError, nextAttemptAt)
            return updateStatusResult
        }

        override fun markInProgress(id: String, now: Long): Boolean {
            lastInProgressId = id
            return markInProgressResult
        }

        override fun listByCashbox(cashboxId: String, lane: QueueLane, limit: Int, offset: Int): List<QueueCommand> {
            return commands
                .filter { it.cashboxId == cashboxId && it.lane == lane }
                .drop(offset)
                .take(limit)
        }

        override fun deleteByCashbox(cashboxId: String): Boolean {
            commands.removeAll { it.cashboxId == cashboxId }
            return true
        }

        override fun hasPendingCommands(cashboxId: String, lane: QueueLane): Boolean {
            return hasPendingCommandsResult
        }
    }

    private class StubLock : LeaseLockPort {
        var lockAcquired = false
        var lockBusy = false
        var releaseResult = true
        var renewResult = true
        var lastAcquireParams: AcquireParams? = null
        var lastRenewParams: RenewParams? = null
        var releasedId: String? = null

        data class AcquireParams(val cashboxId: String, val ownerId: String, val leaseUntil: Long, val now: Long)
        data class RenewParams(val cashboxId: String, val ownerId: String, val leaseUntil: Long, val now: Long)

        override fun tryAcquire(cashboxId: String, ownerId: String, leaseUntil: Long, now: Long): Boolean {
            if (lockBusy) return false
            lockAcquired = true
            lastAcquireParams = AcquireParams(cashboxId, ownerId, leaseUntil, now)
            return true
        }

        override fun renew(cashboxId: String, ownerId: String, leaseUntil: Long, now: Long): Boolean {
            lastRenewParams = RenewParams(cashboxId, ownerId, leaseUntil, now)
            return renewResult
        }

        override fun release(cashboxId: String, ownerId: String): Boolean {
            lockAcquired = false
            releasedId = cashboxId
            return releaseResult
        }
    }

    private fun command(
        id: String = "1",
        cashboxId: String = "c1",
        status: QueueStatus = QueueStatus.PENDING,
        attempt: Int = 0,
        type: QueueCommandType = QueueCommandType.TICKET
    ): QueueCommand = QueueCommand(
        id = id,
        cashboxId = cashboxId,
        lane = QueueLane.OFFLINE,
        type = type,
        payloadRef = "ref$id",
        createdAt = 1000L,
        status = status,
        attempt = attempt
    )

    private fun service(
        storage: StubStorage = StubStorage(),
        lock: StubLock = StubLock(),
        handler: QueueCommandHandler = QueueCommandHandler { _, _ -> DispatchResult(DispatchStatus.SENT) },
        backoffPolicy: BackoffPolicy = BackoffPolicy { _, _ -> 0L },
        timeProvider: TimeProvider = TimeProvider { 10000L },
        maxAttempts: Int = Int.MAX_VALUE
    ): QueueService = QueueService(
        storage = storage,
        lockPort = lock,
        handler = handler,
        backoffPolicy = backoffPolicy,
        ownerId = "node-1",
        timeProvider = timeProvider,
        maxAttempts = maxAttempts
    )

    private fun trilingual(message: String): String = "RU: $message | KK: $message | EN: $message"

    @Test
    fun testEnqueue() {
        val storage = StubStorage()
        val service = service(storage)
        val cmd = command().copy(nextAttemptAt = 12000L, lastError = "previous")
        assertEquals(QueueStatus.PENDING, cmd.status)
        assertTrue(service.enqueue(cmd))
        assertEquals(1, storage.commands.size)
        assertEquals("1", storage.commands[0].id)
        assertEquals("ref1", storage.commands[0].payloadRef)
        assertEquals(1000L, storage.commands[0].createdAt)
        assertEquals(12000L, storage.commands[0].nextAttemptAt)
        assertEquals("previous", storage.commands[0].lastError)
    }

    @Test
    fun testProcessNextHandlerThrowsQueueDispatchException() {
        val storage = StubStorage()
        val service = service(
            storage = storage,
            handler = { _, _ -> throw QueueDispatchException(QueueErrorMessages.invalidDispatchStatus("PENDING")) }
        )
        storage.nextPendingResponse = command()

        assertTrue(service.processNext("c1", QueueLane.OFFLINE))
        assertEquals(QueueStatus.FAILED, storage.lastStatusUpdate?.status)
        assertTrue(storage.lastStatusUpdate?.lastError?.contains("PENDING") == true)
    }

    @Test
    fun testStorageDefaultListOffset() {
        val storage = StubStorage()
        storage.commands.add(command(id = "first"))
        storage.commands.add(command(id = "second"))

        assertEquals(listOf("first"), storage.listByCashbox("c1", QueueLane.OFFLINE, 1).map { it.id })
    }

    @Test
    fun testHasOfflineQueue() {
        val storage = StubStorage()
        val service = service(storage)

        storage.hasPendingCommandsResult = true
        assertTrue(service.hasOfflineQueue("c1"))

        storage.hasPendingCommandsResult = false
        assertFalse(service.hasOfflineQueue("c1"))
    }

    @Test
    fun testClearQueue() {
        val storage = StubStorage()
        val service = service(storage)
        storage.commands.add(command(id = "1"))
        storage.commands.add(command(id = "2"))

        assertTrue(service.clearQueue("c1"))
        assertEquals(0, storage.commands.size)
    }

    @Test
    fun testProcessNextSuccess() {
        val storage = StubStorage()
        val lock = StubLock()
        var handledCommand: QueueCommand? = null
        val handler = QueueCommandHandler { cmd, _ ->
            handledCommand = cmd
            DispatchResult(DispatchStatus.SENT)
        }
        val timeProvider = TimeProvider { 5000L }
        val service = QueueService(
            storage = storage,
            lockPort = lock,
            handler = handler,
            backoffPolicy = { _, _ -> 0L },
            ownerId = "node-1",
            leaseMs = 10000,
            maxAttempts = Int.MAX_VALUE,
            timeProvider = timeProvider
        )

        val cmd = command()
        storage.nextPendingResponse = cmd

        assertTrue(service.processNext("c1", QueueLane.OFFLINE))

        // Check lock was acquired and released
        assertTrue(lock.lockAcquired.not())
        assertEquals("c1", lock.releasedId)
        assertEquals(StubLock.AcquireParams("c1", "node-1", 15000L, 5000L), lock.lastAcquireParams)

        // Check handler processed correct command
        assertEquals("1", handledCommand?.id)

        // Check status updated
        assertEquals("1", storage.lastInProgressId)
        assertEquals(StubStorage.StatusUpdate("1", QueueStatus.SENT, 1, null, null), storage.lastStatusUpdate)
    }

    @Test
    fun testProcessNextLeaseAutoRenewal() {
        val storage = StubStorage()
        val lock = StubLock()
        val handler = QueueCommandHandler { _, renew ->
            val ok = renew() // Call the auto-renewal lambda
            assertTrue(ok)
            DispatchResult(DispatchStatus.SENT)
        }
        val timeProvider = TimeProvider { 5000L }
        val service = QueueService(
            storage = storage,
            lockPort = lock,
            handler = handler,
            backoffPolicy = { _, _ -> 0L },
            ownerId = "node-1",
            leaseMs = 10000,
            maxAttempts = Int.MAX_VALUE,
            timeProvider = timeProvider
        )

        storage.nextPendingResponse = command()

        assertTrue(service.processNext("c1", QueueLane.OFFLINE))
        assertEquals(StubLock.RenewParams("c1", "node-1", 15000L, 5000L), lock.lastRenewParams)
    }

    @Test
    fun testProcessNextLockBusy() {
        val storage = StubStorage()
        val lock = StubLock().apply { lockBusy = true }
        val service = service(storage, lock)
        assertFalse(service.processNext("c1", QueueLane.OFFLINE))
        assertEquals(null, storage.lastNextPendingParams)
        assertEquals(null, lock.releasedId)
    }

    @Test
    fun testProcessNextFailureWithBackoff() {
        val storage = StubStorage()
        val lock = StubLock()
        val backoff = BackoffPolicy { now, attempt -> now + attempt * 2000L }
        val service = service(
            storage = storage,
            lock = lock,
            handler = { _, _ -> DispatchResult(DispatchStatus.FAILED, errorMessage = "OFD Timeout") },
            backoffPolicy = backoff
        )

        val cmd = command(attempt = 1)
        storage.nextPendingResponse = cmd

        assertTrue(service.processNext("c1", QueueLane.OFFLINE))

        // Check failure status update with correct attempt and retryTime
        assertEquals(
            StubStorage.StatusUpdate("1", QueueStatus.FAILED, 2, trilingual("OFD Timeout"), 14000L),
            storage.lastStatusUpdate
        )
    }

    @Test
    fun testProcessNextHandlerThrowsException() {
        val storage = StubStorage()
        val lock = StubLock()
        val backoff = BackoffPolicy { now, attempt -> now + attempt * 2000L }
        val service = service(
            storage = storage,
            lock = lock,
            handler = { _, _ -> throw IllegalStateException("Database Connection Lost") },
            backoffPolicy = backoff
        )

        val cmd = command()
        storage.nextPendingResponse = cmd

        assertTrue(service.processNext("c1", QueueLane.OFFLINE))

        // Lock should be released successfully
        assertTrue(lock.lockAcquired.not())

        // The command status should be updated to FAILED with the exception message
        assertEquals(
            StubStorage.StatusUpdate(
                "1",
                QueueStatus.FAILED,
                1,
                "RU: Ошибка связи или внутренняя ошибка кассы при обработке очереди. Пожалуйста, убедитесь в наличии интернета и повторите попытку. (Технические детали: Database Connection Lost) | " +
                    "KK: Кезекпен жұмыс істеу кезінде байланыс немесе кассаның ішкі қатесі орын алды. Интернет байланысын тексеріп, әрекетті қайталаңыз. (Техникалық мәліметтер: Database Connection Lost) | " +
                    "EN: Connection failure or internal cashbox error while processing queue. Please verify internet connection and retry the operation. (Technical details: Database Connection Lost)",
                12000L
            ),
            storage.lastStatusUpdate
        )
    }

    @Test
    fun testProcessNextHandlerThrowsAnonymousExceptionWithoutMessage() {
        val storage = StubStorage().apply { nextPendingResponse = command() }
        val service = service(
            storage = storage,
            handler = { _, _ -> throw object : RuntimeException() {} },
            backoffPolicy = BackoffPolicy { now, attempt -> now + attempt }
        )

        assertTrue(service.processNext("c1", QueueLane.OFFLINE))
        assertEquals(
            StubStorage.StatusUpdate(
                "1",
                QueueStatus.FAILED,
                1,
                "RU: Ошибка связи или внутренняя ошибка кассы при обработке очереди. Пожалуйста, убедитесь в наличии интернета и повторите попытку. (Технические детали: Exception) | " +
                    "KK: Кезекпен жұмыс істеу кезінде байланыс немесе кассаның ішкі қатесі орын алды. Интернет байланысын тексеріп, әрекетті қайталаңыз. (Техникалық мәліметтер: Exception) | " +
                    "EN: Connection failure or internal cashbox error while processing queue. Please verify internet connection and retry the operation. (Technical details: Exception)",
                10001L
            ),
            storage.lastStatusUpdate
        )
    }

    @Test
    fun testProcessNextHandlerThrowsExceptionWithoutMessage() {
        val storage = StubStorage().apply { nextPendingResponse = command() }
        val service = service(
            storage = storage,
            handler = { _, _ -> throw NullPointerException() },
            backoffPolicy = BackoffPolicy { now, attempt -> now + attempt }
        )

        assertTrue(service.processNext("c1", QueueLane.OFFLINE))
        assertEquals(
            StubStorage.StatusUpdate(
                "1",
                QueueStatus.FAILED,
                1,
                "RU: Ошибка связи или внутренняя ошибка кассы при обработке очереди. Пожалуйста, убедитесь в наличии интернета и повторите попытку. (Технические детали: NullPointerException) | " +
                    "KK: Кезекпен жұмыс істеу кезінде байланыс немесе кассаның ішкі қатесі орын алды. Интернет байланысын тексеріп, әрекетті қайталаңыз. (Техникалық мәліметтер: NullPointerException) | " +
                    "EN: Connection failure or internal cashbox error while processing queue. Please verify internet connection and retry the operation. (Technical details: NullPointerException)",
                10001L
            ),
            storage.lastStatusUpdate
        )
    }

    @Test
    fun testProcessNextHandlerReturnsInternalStatusAsFailure() {
        val storage = StubStorage().apply { nextPendingResponse = command() }
        val service = service(
            storage = storage,
            handler = { _, _ -> DispatchResult(QueueStatus.FAILED) },
            backoffPolicy = { now, attempt -> now + attempt }
        )

        assertTrue(service.processNext("c1", QueueLane.OFFLINE))
        assertEquals(
            StubStorage.StatusUpdate(
                "1",
                QueueStatus.FAILED,
                1,
                null,
                10001L
            ),
            storage.lastStatusUpdate
        )
    }

    @Test
    fun testDefaultBackoffPolicy() {
        val policy = DefaultBackoffPolicy(1000, 60000)
        assertEquals(11000L, policy.nextAttemptAt(10000L, 0)) // 10000 + 1000 * 2^0 = 11000
        assertEquals(12000L, policy.nextAttemptAt(10000L, 1)) // 10000 + 1000 * 2^1 = 12000
        assertEquals(14000L, policy.nextAttemptAt(10000L, 2)) // 10000 + 1000 * 2^2 = 14000
        assertEquals(70000L, policy.nextAttemptAt(10000L, 10)) // 10000 + 60000 (max) = 70000

        val defaultPolicy = DefaultBackoffPolicy()
        assertEquals(11000L, defaultPolicy.nextAttemptAt(10000L, 0))
    }

    @Test
    fun testSystemTimeProvider() {
        val now = kz.mybrain.superkassa.offline_queue.application.policy.SystemTimeProvider.now()
        assertTrue(now > 0)
    }

    @Test
    fun testProcessNextNoPending() {
        val storage = StubStorage()
        val lock = StubLock()
        val service = service(storage, lock)
        storage.nextPendingResponse = null
        assertFalse(service.processNext("c1", QueueLane.OFFLINE))
        assertTrue(lock.lockAcquired.not()) // Released in finally
        assertEquals(StubStorage.NextPendingParams("c1", QueueLane.OFFLINE, 10000L), storage.lastNextPendingParams)
        assertEquals("c1", lock.releasedId)
        assertEquals(null, storage.lastInProgressId)
    }

    @Test
    fun testProcessNextStopsWhenMarkInProgressFails() {
        val storage = StubStorage().apply {
            nextPendingResponse = command()
            markInProgressResult = false
        }
        var handled = false
        val service = service(
            storage = storage,
            handler = QueueCommandHandler { _, _ ->
                handled = true
                DispatchResult(DispatchStatus.SENT)
            }
        )

        assertFalse(service.processNext("c1", QueueLane.OFFLINE))
        assertFalse(handled)
        assertEquals("1", storage.lastInProgressId)
        assertEquals(null, storage.lastStatusUpdate)
    }

    @Test
    fun testProcessNextReturnsFalseWhenStatusUpdateFails() {
        val storage = StubStorage().apply {
            nextPendingResponse = command()
            updateStatusResult = false
        }
        val service = service(storage)

        assertFalse(service.processNext("c1", QueueLane.OFFLINE))
        assertEquals(StubStorage.StatusUpdate("1", QueueStatus.SENT, 1, null, null), storage.lastStatusUpdate)
    }

    @Test
    fun testFailedDispatchReturnsFalseWhenStatusUpdateFails() {
        val storage = StubStorage().apply {
            nextPendingResponse = command()
            updateStatusResult = false
        }
        val service = service(
            storage = storage,
            handler = QueueCommandHandler { _, _ -> DispatchResult(DispatchStatus.FAILED, "failed") }
        )

        assertFalse(service.processNext("c1", QueueLane.OFFLINE))
        assertEquals(
            StubStorage.StatusUpdate("1", QueueStatus.FAILED, 1, trilingual("failed"), 0L),
            storage.lastStatusUpdate
        )
    }

    @Test
    fun testProcessNextReturnsTrueWhenReleaseFailsAfterSuccessfulUpdate() {
        val storage = StubStorage().apply { nextPendingResponse = command() }
        val lock = StubLock().apply { releaseResult = false }
        val service = service(storage, lock)

        assertTrue(service.processNext("c1", QueueLane.OFFLINE))
        assertEquals("c1", lock.releasedId)
    }

    @Test
    fun testFailedDispatchUsesExplicitRetryAt() {
        val storage = StubStorage().apply { nextPendingResponse = command() }
        val service = service(
            storage = storage,
            handler = QueueCommandHandler { _, _ ->
                DispatchResult(DispatchStatus.FAILED, "try later", retryAt = 42000L)
            },
            backoffPolicy = BackoffPolicy { _, _ -> error("Backoff should not be called") }
        )

        assertTrue(service.processNext("c1", QueueLane.OFFLINE))
        assertEquals(
            StubStorage.StatusUpdate("1", QueueStatus.FAILED, 1, trilingual("try later"), 42000L),
            storage.lastStatusUpdate
        )
    }

    @Test
    fun testFailedDispatchStopsRetryAtMaxAttempts() {
        val storage = StubStorage().apply { nextPendingResponse = command(attempt = 2) }
        val service = service(
            storage = storage,
            handler = QueueCommandHandler { _, _ ->
                DispatchResult(DispatchStatus.FAILED, "permanent")
            },
            backoffPolicy = BackoffPolicy { now, attempt -> now + attempt },
            maxAttempts = 3
        )

        assertTrue(service.processNext("c1", QueueLane.OFFLINE))
        assertEquals(
            StubStorage.StatusUpdate("1", QueueStatus.FAILED, 3, trilingual("permanent"), null),
            storage.lastStatusUpdate
        )
    }

    @Test
    fun testFailedDispatchRetriesBeforeMaxAttempts() {
        val storage = StubStorage().apply { nextPendingResponse = command(attempt = 1) }
        val service = service(
            storage = storage,
            handler = { _, _ -> DispatchResult(DispatchStatus.FAILED, "temporary") },
            backoffPolicy = BackoffPolicy { now, attempt -> now + attempt * 1000L },
            maxAttempts = 3
        )

        assertTrue(service.processNext("c1", QueueLane.OFFLINE))
        assertEquals(
            StubStorage.StatusUpdate("1", QueueStatus.FAILED, 2, trilingual("temporary"), 12000L),
            storage.lastStatusUpdate
        )
    }

    @Test
    fun testFailedDispatchAllowsMissingErrorMessage() {
        val storage = StubStorage().apply { nextPendingResponse = command() }
        val service = service(
            storage = storage,
            handler = { _, _ -> DispatchResult(QueueStatus.FAILED) },
            backoffPolicy = BackoffPolicy { now, attempt -> now + attempt }
        )

        assertTrue(service.processNext("c1", QueueLane.OFFLINE))
        assertEquals(
            StubStorage.StatusUpdate("1", QueueStatus.FAILED, 1, null, 10001L),
            storage.lastStatusUpdate
        )
    }

    @Test
    fun testConstructorRejectsInvalidLeaseAndAttemptLimits() {
        val storage = StubStorage()
        val lock = StubLock()

        assertFailsWith<IllegalArgumentException> {
            QueueService(
                storage = storage,
                lockPort = lock,
                handler = { _, _ -> DispatchResult(DispatchStatus.SENT) },
                backoffPolicy = { _, _ -> 0L },
                ownerId = "node-1",
                leaseMs = 0
            )
        }
        assertFailsWith<IllegalArgumentException> {
            QueueService(
                storage = storage,
                lockPort = lock,
                handler = { _, _ -> DispatchResult(DispatchStatus.SENT) },
                backoffPolicy = { _, _ -> 0L },
                ownerId = "node-1",
                maxAttempts = 0
            )
        }
    }

    @Test
    fun testProcessBatch() {
        val storage = StubStorage()
        val lock = StubLock()
        var processCount = 0
        val service = QueueService(
            storage = storage,
            lockPort = lock,
            handler = { _, _ ->
                processCount++
                storage.nextPendingResponse = null
                DispatchResult(DispatchStatus.SENT)
            },
            backoffPolicy = { _, _ -> 0L },
            ownerId = "node-1"
        )

        val cmd = command()
        storage.nextPendingResponse = cmd

        // First batch run processes 1
        assertEquals(1, service.processBatch("c1", QueueLane.OFFLINE, 10))
        assertEquals(1, processCount)

        // If lock acquisition fails, stops batch processing
        lock.lockBusy = true
        assertEquals(0, service.processBatch("c1", QueueLane.OFFLINE, 10))
    }

    @Test
    fun testProcessBatchAllowsZeroLimit() {
        val service = service()

        assertEquals(0, service.processBatch("c1", QueueLane.OFFLINE, 0))
    }

    @Test
    fun testProcessBatchRejectsNegativeLimit() {
        val service = service()

        assertFailsWith<IllegalArgumentException> {
            service.processBatch("c1", QueueLane.OFFLINE, -1)
        }
    }

    @Test
    fun testDispatchResultRejectsInternalQueueStatuses() {
        val sent = DispatchResult(DispatchStatus.SENT, errorMessage = "ok")
        val failed = DispatchResult(DispatchStatus.FAILED, errorMessage = "failed")
        val failedWithoutMessage = DispatchResult(DispatchStatus.FAILED)
        val compatibleWithoutMessage = DispatchResult(QueueStatus.FAILED)
        val compatibleWithMessage = DispatchResult(QueueStatus.FAILED, errorMessage = "compatible")
        val customError = QueueErrorMessage(
            messageRu = "Ошибка",
            messageKk = "Қате",
            messageEn = "Error"
        )

        assertEquals(QueueStatus.SENT, sent.status)
        assertEquals("RU: ok | KK: ok | EN: ok", sent.error?.compact())
        assertEquals(QueueStatus.FAILED, failed.status)
        assertEquals("RU: failed | KK: failed | EN: failed", failed.error?.compact())
        assertEquals(null, failedWithoutMessage.error)
        assertEquals(null, compatibleWithoutMessage.error)
        assertEquals("RU: compatible | KK: compatible | EN: compatible", compatibleWithMessage.error?.compact())
        assertEquals("Ошибка", customError.messageRu)
        assertEquals("Қате", customError.messageKk)
        assertEquals("Error", customError.messageEn)
        assertEquals("RU: Ошибка | KK: Қате | EN: Error", customError.compact())
        assertEquals(DispatchStatus.SENT, DispatchResult(QueueStatus.SENT).dispatchStatus)
        assertEquals(DispatchStatus.FAILED, DispatchResult(QueueStatus.FAILED).dispatchStatus)
        val ex1 = assertFailsWith<QueueDispatchException> {
            DispatchResult(QueueStatus.PENDING)
        }
        assertEquals("PENDING", ex1.error.messageRu.substringAfter("(").substringBefore(")"))

        val ex2 = assertFailsWith<QueueDispatchException> {
            DispatchResult(QueueStatus.IN_PROGRESS)
        }
        assertEquals("IN_PROGRESS", ex2.error.messageRu.substringAfter("(").substringBefore(")"))
    }

    @Test
    fun testProcessNextPreservesCommandType() {
        val storage = StubStorage()
        val types = QueueCommandType.values()

        for (type in types) {
            val cmd = command(id = type.name, type = type)
            storage.nextPendingResponse = cmd

            var receivedType: QueueCommandType? = null
            val service = service(
                storage = storage,
                handler = { receivedCmd, _ ->
                    receivedType = receivedCmd.type
                    DispatchResult(DispatchStatus.SENT)
                }
            )

            assertTrue(service.processNext("c1", QueueLane.OFFLINE))
            assertEquals(type, receivedType)
        }
    }

    @Test
    fun testProcessNextReturnsFalseWhenQueueIsEmptyAndReleaseFails() {
        val storage = StubStorage().apply { nextPendingResponse = null }
        val lock = StubLock().apply { releaseResult = false }
        val service = service(storage, lock)

        assertFalse(service.processNext("c1", QueueLane.OFFLINE))
        assertEquals("c1", lock.releasedId)
    }

    @Test
    fun testProcessNextPropagatesStorageExceptionWhenReleaseSucceeds() {
        val storage = StubStorage().apply { nextPendingError = RuntimeException("Storage failure") }
        val lock = StubLock().apply { releaseResult = true }
        val service = service(storage, lock)

        assertFailsWith<RuntimeException> {
            service.processNext("c1", QueueLane.OFFLINE)
        }
        assertEquals("c1", lock.releasedId)
    }

    @Test
    fun testProcessNextPropagatesStorageExceptionWhenReleaseFails() {
        val storage = StubStorage().apply { nextPendingError = RuntimeException("Storage failure") }
        val lock = StubLock().apply { releaseResult = false }
        val service = service(storage, lock)

        assertFailsWith<RuntimeException> {
            service.processNext("c1", QueueLane.OFFLINE)
        }
        assertEquals("c1", lock.releasedId)
    }
}
