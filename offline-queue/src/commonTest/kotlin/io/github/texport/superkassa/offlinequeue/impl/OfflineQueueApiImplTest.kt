package io.github.texport.superkassa.offlinequeue.impl

import io.github.texport.superkassa.offlinequeue.api.createOfflineQueueApi
import io.github.texport.superkassa.offlinequeue.api.model.DispatchResult
import io.github.texport.superkassa.offlinequeue.api.model.DispatchStatus
import io.github.texport.superkassa.offlinequeue.api.model.QueueCommand
import io.github.texport.superkassa.offlinequeue.api.model.QueueCommandType
import io.github.texport.superkassa.core.string.api.CoreStrings
import io.github.texport.superkassa.core.string.api.TrilingualMessage
import io.github.texport.superkassa.offlinequeue.api.model.QueueLane
import io.github.texport.superkassa.offlinequeue.api.model.QueueStatus
import io.github.texport.superkassa.offlinequeue.api.port.LeaseLockPort
import io.github.texport.superkassa.offlinequeue.api.port.QueueCommandHandlerPort
import io.github.texport.superkassa.offlinequeue.api.port.QueueStoragePort
import io.github.texport.superkassa.offlinequeue.impl.policy.BackoffPolicy
import io.github.texport.superkassa.offlinequeue.impl.policy.DefaultBackoffPolicy
import io.github.texport.superkassa.offlinequeue.impl.policy.SystemTimeProvider
import io.github.texport.superkassa.offlinequeue.impl.policy.TimeProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OfflineQueueApiImplTest {

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
            val nextAttemptAt: Long?,
            val lastErrorCode: Int? = null
        )

        data class NextPendingParams(val cashboxId: String, val lane: QueueLane)

        override fun enqueue(command: QueueCommand): Boolean {
            commands.add(command)
            return true
        }

        override fun getCommandsByStatus(cashboxId: String, lane: QueueLane, statuses: Set<QueueStatus>): List<QueueCommand> {
            lastNextPendingParams = NextPendingParams(cashboxId, lane)
            nextPendingError?.let { throw it }
            return listOfNotNull(nextPendingResponse)
        }

        override fun updateStatus(
            id: String,
            status: QueueStatus,
            attempt: Int,
            lastError: String?,
            nextAttemptAt: Long?,
            lastErrorCode: Int?
        ): Boolean {
            lastStatusUpdate = StatusUpdate(id, status, attempt, lastError, nextAttemptAt, lastErrorCode)
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
        type: QueueCommandType = QueueCommandType.TICKET,
        nextAttemptAt: Long? = null
    ): QueueCommand = QueueCommand(
        id = id,
        cashboxId = cashboxId,
        lane = QueueLane.OFFLINE,
        type = type,
        payloadRef = "ref$id",
        createdAt = 1000L,
        status = status,
        attempt = attempt,
        nextAttemptAt = nextAttemptAt
    )

    private fun service(
        storage: StubStorage = StubStorage(),
        lock: StubLock = StubLock(),
        handler: QueueCommandHandlerPort = QueueCommandHandlerPort { _, _ -> DispatchResult(DispatchStatus.SENT) },
        backoffPolicy: BackoffPolicy = BackoffPolicy { _, _ -> 0L },
        timeProvider: TimeProvider = TimeProvider { 10000L },
        maxAttempts: Int = Int.MAX_VALUE,
        now: Long? = null
    ): OfflineQueueApiImpl = OfflineQueueApiImpl(
        storage = storage,
        lockPort = lock,
        handler = handler,
        backoffPolicy = backoffPolicy,
        ownerId = "node-1",
        timeProvider = now?.let { moment -> TimeProvider { moment } } ?: timeProvider,
        maxAttempts = maxAttempts
    )

    /** Срок аренды очереди по умолчанию: на нём построен расчёт брошенных задач. */
    private val LEASE_MS = 15_000L

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
            handler = { _, _ -> throw QueueDispatchException(CoreStrings.invalidDispatchStatus("PENDING")) }
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
    fun testHasOfflineQueueApi() {
        val storage = StubStorage()
        val service = service(storage)

        storage.nextPendingResponse = command()
        assertTrue(service.hasOfflineQueue("c1"))

        storage.nextPendingResponse = null
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
    fun testWaitingHeadIsNotOvertakenByALaterCommand() {
        // Спецификация CPCR: накопленные сообщения досылаются строго в том
        // порядке, в каком они попали в очередь. Голова очереди ждёт повтора,
        // и более поздний чек не имеет права уйти в ОФД впереди неё.
        val storage = StubStorage()
        val dispatched = mutableListOf<String>()
        val service = service(
            storage,
            handler = QueueCommandHandlerPort { cmd, _ ->
                dispatched.add(cmd.id)
                DispatchResult(DispatchStatus.SENT)
            },
            timeProvider = TimeProvider { 10_000L }
        )
        storage.commands.add(
            command(id = "ранний", status = QueueStatus.FAILED)
                .copy(createdAt = 1000L, nextAttemptAt = 50_000L)
        )
        storage.commands.add(command(id = "поздний").copy(createdAt = 2000L))

        assertFalse(service.processNext("c1", QueueLane.OFFLINE))
        assertEquals(emptyList(), dispatched)
    }

    @Test
    fun testProcessNextSuccess() {
        val storage = StubStorage()
        val lock = StubLock()
        var handledCommand: QueueCommand? = null
        val handler = QueueCommandHandlerPort { cmd, _ ->
            handledCommand = cmd
            DispatchResult(DispatchStatus.SENT)
        }
        val timeProvider = TimeProvider { 5000L }
        val service = OfflineQueueApiImpl(
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

        // Проверка, что блокировка была захвачена и освобождена
        assertTrue(lock.lockAcquired.not())
        assertEquals("c1", lock.releasedId)
        assertEquals(StubLock.AcquireParams("c1", "node-1", 15000L, 5000L), lock.lastAcquireParams)

        // Проверка, что обработчик обработал правильную команду
        assertEquals("1", handledCommand?.id)

        // Проверка, что статус обновлен
        assertEquals("1", storage.lastInProgressId)
        assertEquals(StubStorage.StatusUpdate("1", QueueStatus.SENT, 1, null, null), storage.lastStatusUpdate)
    }

    @Test
    fun testProcessNextLeaseAutoRenewal() {
        val storage = StubStorage()
        val lock = StubLock()
        val handler = QueueCommandHandlerPort { _, renew ->
            val ok = renew() // Вызов лямбды автопродления блокировки
            assertTrue(ok)
            DispatchResult(DispatchStatus.SENT)
        }
        val timeProvider = TimeProvider { 5000L }
        val service = OfflineQueueApiImpl(
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

        // Проверка обновления статуса ошибки с корректной попыткой и retryTime
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

        // Блокировка должна быть успешно освобождена
        assertTrue(lock.lockAcquired.not())

        // Статус команды должен обновиться на FAILED с сообщением об ошибке
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
        val now = SystemTimeProvider.now()
        assertTrue(now > 0)
    }

    @Test
    fun testProcessNextNoPending() {
        val storage = StubStorage()
        val lock = StubLock()
        val service = service(storage, lock)
        storage.nextPendingResponse = null
        assertFalse(service.processNext("c1", QueueLane.OFFLINE))
        assertTrue(lock.lockAcquired.not()) // Освобождается в блоке finally
        assertEquals(StubStorage.NextPendingParams("c1", QueueLane.OFFLINE), storage.lastNextPendingParams)
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
            handler = QueueCommandHandlerPort { _, _ ->
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
            handler = QueueCommandHandlerPort { _, _ -> DispatchResult(DispatchStatus.FAILED, "failed") }
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
            handler = QueueCommandHandlerPort { _, _ ->
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

    /**
     * Отвергнутая окончательно команда снимается с повтора совсем.
     *
     * Времени следующей попытки у неё нет и быть не может: повторять
     * нечего. Без этого состояния очередь повторяла негодную команду
     * вечно — на стенде счётчик дошёл до 530 попыток.
     */
    /**
     * Задачу, брошенную умершим обработчиком, очередь забирает обратно.
     *
     * Без этого обычный перезапуск узла терял документы навсегда: они
     * оставались в `IN_PROGRESS`, выборка их не брала, и до ОФД они
     * не доходили никогда. На стенде так осталось шесть чеков.
     */
    @Test
    fun testAbandonedCommandIsPickedUpAgain() {
        val claimedAt = 1_000L
        val storage = StubStorage().apply {
            nextPendingResponse = command(status = QueueStatus.IN_PROGRESS, nextAttemptAt = claimedAt)
        }
        val service = service(
            storage = storage,
            handler = QueueCommandHandlerPort { _, _ -> DispatchResult(DispatchStatus.SENT) },
            now = claimedAt + LEASE_MS * 2
        )

        assertTrue(service.processNext("c1", QueueLane.OFFLINE))
        assertEquals(QueueStatus.SENT, storage.lastStatusUpdate?.status)
    }

    /** Пока обработчик жив, задачу у него не отбирают: документ ушёл бы дважды. */
    @Test
    fun testCommandInFlightIsLeftAlone() {
        val claimedAt = 1_000L
        val storage = StubStorage().apply {
            nextPendingResponse = command(status = QueueStatus.IN_PROGRESS, nextAttemptAt = claimedAt)
        }
        val service = service(
            storage = storage,
            handler = QueueCommandHandlerPort { _, _ -> error("Живую задачу забирать нельзя") },
            now = claimedAt + LEASE_MS
        )

        assertFalse(service.processNext("c1", QueueLane.OFFLINE))
    }

    /**
     * Захват без времени оставила прежняя версия: она его не записывала.
     * Такие задачи висели в `IN_PROGRESS` вечно, и вернуть их было не по чему.
     */
    @Test
    fun testCommandClaimedWithoutTimeIsAbandoned() {
        val storage = StubStorage().apply {
            nextPendingResponse = command(status = QueueStatus.IN_PROGRESS, nextAttemptAt = null)
        }
        val service = service(
            storage = storage,
            handler = QueueCommandHandlerPort { _, _ -> DispatchResult(DispatchStatus.SENT) }
        )

        assertTrue(service.processNext("c1", QueueLane.OFFLINE))
        assertEquals(QueueStatus.SENT, storage.lastStatusUpdate?.status)
    }

    @Test
    fun testRejectedDispatchLeavesNoNextAttempt() {
        val storage = StubStorage().apply { nextPendingResponse = command() }
        val service = service(
            storage = storage,
            handler = QueueCommandHandlerPort { _, _ ->
                DispatchResult(DispatchStatus.REJECTED, "document is not valid", errorCode = 13)
            },
            backoffPolicy = BackoffPolicy { _, _ -> error("Backoff should not be called") }
        )

        assertTrue(service.processNext("c1", QueueLane.OFFLINE))
        assertEquals(
            StubStorage.StatusUpdate(
                "1",
                QueueStatus.REJECTED,
                1,
                trilingual("document is not valid"),
                null,
                lastErrorCode = 13
            ),
            storage.lastStatusUpdate
        )
    }

    @Test
    fun testFailedDispatchKeepsRecipientCodeBesideText() {
        val storage = StubStorage().apply { nextPendingResponse = command() }
        val service = service(
            storage = storage,
            handler = QueueCommandHandlerPort { _, _ ->
                DispatchResult(DispatchStatus.FAILED, "busy", retryAt = 500L, errorCode = 254)
            }
        )

        assertTrue(service.processNext("c1", QueueLane.OFFLINE))
        assertEquals(254, storage.lastStatusUpdate?.lastErrorCode)
    }

    @Test
    fun testFailedDispatchStopsRetryAtMaxAttempts() {
        val storage = StubStorage().apply { nextPendingResponse = command(attempt = 2) }
        val service = service(
            storage = storage,
            handler = QueueCommandHandlerPort { _, _ ->
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
            OfflineQueueApiImpl(
                storage = storage,
                lockPort = lock,
                handler = { _, _ -> DispatchResult(DispatchStatus.SENT) },
                backoffPolicy = { _, _ -> 0L },
                ownerId = "node-1",
                leaseMs = 0
            )
        }
        assertFailsWith<IllegalArgumentException> {
            OfflineQueueApiImpl(
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
        val service = OfflineQueueApiImpl(
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

        // Первый запуск пачки обрабатывает 1 команду
        assertEquals(1, service.processBatch("c1", QueueLane.OFFLINE, 10))
        assertEquals(1, processCount)

        // Если захват блокировки не удался, обработка пачки прекращается
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
        val customError = TrilingualMessage(
            ru = "Ошибка",
            kk = "Қате",
            en = "Error"
        )

        assertEquals(QueueStatus.SENT, sent.status)
        assertEquals("RU: ok | KK: ok | EN: ok", sent.error?.compact())
        assertEquals(QueueStatus.FAILED, failed.status)
        assertEquals("RU: failed | KK: failed | EN: failed", failed.error?.compact())
        assertEquals(null, failedWithoutMessage.error)
        assertEquals(null, compatibleWithoutMessage.error)
        assertEquals("RU: compatible | KK: compatible | EN: compatible", compatibleWithMessage.error?.compact())
        assertEquals("Ошибка", customError.ru)
        assertEquals("Қате", customError.kk)
        assertEquals("Error", customError.en)
        assertEquals("RU: Ошибка | KK: Қате | EN: Error", customError.compact())
        assertEquals(DispatchStatus.SENT, DispatchResult(QueueStatus.SENT).dispatchStatus)
        assertEquals(DispatchStatus.FAILED, DispatchResult(QueueStatus.FAILED).dispatchStatus)
        val ex1 = assertFailsWith<QueueDispatchException> {
            DispatchResult(QueueStatus.PENDING)
        }
        assertEquals("PENDING", ex1.error.ru.substringAfter("(").substringBefore(")"))

        val ex2 = assertFailsWith<QueueDispatchException> {
            DispatchResult(QueueStatus.IN_PROGRESS)
        }
        assertEquals("IN_PROGRESS", ex2.error.ru.substringAfter("(").substringBefore(")"))
    }

    @Test
    fun testProcessNextPreservesCommandType() {
        val storage = StubStorage()
        val types = QueueCommandType.entries

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

    @Test
    fun testCreateOfflineQueueApi() {
        val storage = StubStorage()
        val lock = StubLock()
        val handler = QueueCommandHandlerPort { _, _ -> DispatchResult(DispatchStatus.SENT) }
        val queue = createOfflineQueueApi(storage, lock, handler, "node-1")

        val cmd = command()
        assertTrue(queue.enqueue(cmd))

        storage.nextPendingResponse = cmd
        assertTrue(queue.processNext("c1", QueueLane.OFFLINE))

        storage.hasPendingCommandsResult = true
        assertTrue(queue.hasOfflineQueue("c1"))

        storage.nextPendingResponse = cmd
        assertEquals(1, queue.processBatch("c1", QueueLane.OFFLINE, 1))

        assertTrue(queue.clearQueue("c1"))
    }
}
