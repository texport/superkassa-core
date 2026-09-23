package io.github.texport.superkassa.coredatabase.impl.adapter

import io.github.texport.superkassa.core.domain.api.model.auth.KkmUser
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.model.common.CounterSnapshot
import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.queue.QueueTask
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftStatus
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.coredatabase.impl.dao.CounterDao
import io.github.texport.superkassa.coredatabase.impl.dao.FiscalDocumentDao
import io.github.texport.superkassa.coredatabase.impl.dao.IdempotencyDao
import io.github.texport.superkassa.coredatabase.impl.dao.KkmDao
import io.github.texport.superkassa.coredatabase.impl.dao.KkmUserDao
import io.github.texport.superkassa.coredatabase.impl.dao.QueueCommandDao
import io.github.texport.superkassa.coredatabase.impl.dao.ShiftDao
import io.github.texport.superkassa.offlinequeue.api.model.QueueCommand
import io.github.texport.superkassa.offlinequeue.api.model.QueueLane
import io.github.texport.superkassa.offlinequeue.api.model.QueueStatus
import io.github.texport.superkassa.offlinequeue.api.port.QueueStoragePort
import kotlinx.coroutines.runBlocking

/**
 * Хранилище кассы на Room, фискально равное хранилищу узла.
 *
 * Здесь только сборка: каждый вызов порта уходит в часть, которая ведёт
 * свой сценарий, — кассы и кассиры, смены и счётчики, запись и чтение
 * документов, очередь досылки, транзакции и ключи повтора. Счёт неверных
 * пинов ведёт отдельный порт той же базы. Что хранится
 * и что уходит в ОФД, совпадает с узлом: единицы сумм, типы, номера,
 * статусы и то, что ставится в очередь.
 */
internal class DefaultRoomStorageAdapter(
    queueDao: QueueCommandDao,
    kkmDao: KkmDao,
    userDao: KkmUserDao,
    shiftDao: ShiftDao,
    fiscalDocumentDao: FiscalDocumentDao,
    counterDao: CounterDao,
    private val idempotencyDao: IdempotencyDao,
    /** Счёт неверных пинов: ядру он передаётся отдельно, здесь — только чтобы уйти вместе с кассой. */
    private val pinAttempts: RoomPinAttempts
) : QueueStoragePort, StoragePort {

    private val writer = RoomWriter(idempotencyDao)
    private val kkms = RoomKkms(kkmDao, userDao)
    private val shifts = RoomShifts(shiftDao, counterDao)
    private val documents = RoomDocuments(fiscalDocumentDao, shiftDao)
    private val reads = RoomDocumentReads(fiscalDocumentDao, kkmDao)
    private val queue = RoomQueue(queueDao)

    // --- Очередь ---
    override fun enqueue(command: QueueCommand): Boolean = queue.enqueue(command)
    override fun getCommandsByStatus(cashboxId: String, lane: QueueLane, statuses: Set<QueueStatus>) =
        queue.byStatus(cashboxId, lane, statuses)
    override fun updateStatus(id: String, status: QueueStatus, attempt: Int, lastError: String?, nextAttemptAt: Long?) =
        queue.updateStatus(id, status, attempt, lastError, nextAttemptAt)
    override fun markInProgress(id: String, now: Long): Boolean = queue.markInProgress(id, now)
    override fun listByCashbox(cashboxId: String, lane: QueueLane, limit: Int, offset: Int) =
        queue.list(cashboxId, lane, limit, offset)
    override fun deleteByCashbox(cashboxId: String): Boolean = queue.deleteByCashbox(cashboxId)

    // Очередь ядра пишет через порт кассы, а не через порт очереди: таблица одна на оба.
    override fun enqueueQueueTask(dto: QueueTask): Boolean = queue.enqueue(dto.toCommand())
    override fun listQueueTasksByCashbox(cashboxId: String, lane: String, limit: Int, offset: Int): List<QueueTask> =
        queue.list(cashboxId, QueueLane.valueOf(lane), limit, offset).map { it.toTask() }
    override fun getQueueTasksByStatus(cashboxId: String, lane: String, statuses: Set<String>): List<QueueTask> =
        queue.byStatus(cashboxId, QueueLane.valueOf(lane), statuses.toQueueStatuses()).map { it.toTask() }
    override fun updateQueueTaskStatus(
        id: String,
        status: String,
        attempt: Int,
        lastError: String?,
        nextAttemptAt: Long?
    ) =
        queue.updateStatus(id, QueueStatus.valueOf(status), attempt, lastError, nextAttemptAt)
    override fun markQueueTaskInProgress(id: String, now: Long): Boolean = queue.markInProgress(id, now)
    override fun deleteQueueTasksByCashbox(cashboxId: String): Boolean = queue.deleteByCashbox(cashboxId)
    override fun countOfflineQueue(): Long = 0L
    override fun tryAcquireQueueLock(cashboxId: String, ownerId: String, leaseUntil: Long, acquiredAt: Long) =
        queue.acquire(cashboxId, ownerId, leaseUntil, acquiredAt)
    override fun renewQueueLock(cashboxId: String, ownerId: String, leaseUntil: Long, now: Long) =
        queue.renew(cashboxId, ownerId, leaseUntil, now)
    override fun releaseQueueLock(cashboxId: String, ownerId: String): Boolean = queue.release(cashboxId, ownerId)

    // --- Транзакции и ключи повтора ---
    override fun startTransaction() = writer.begin()
    override fun commitTransaction() = writer.commit()
    override fun rollbackTransaction() = writer.rollback()
    override fun insertIdempotency(kkmId: String, idempotencyKey: String, operation: String) =
        writer.insertKey(kkmId, idempotencyKey, operation)
    override fun findIdempotencyResponse(kkmId: String, idempotencyKey: String): String? =
        writer.responseOf(kkmId, idempotencyKey)
    override fun updateIdempotencyResponse(kkmId: String, idempotencyKey: String, responseRef: String?) =
        writer.complete(kkmId, idempotencyKey, responseRef)

    // --- Кассы и кассиры ---
    override fun createKkm(info: KkmInfo): Boolean = kkms.create(info)
    override fun updateKkm(info: KkmInfo): Boolean = kkms.update(info)
    override fun findKkm(id: String): KkmInfo? = kkms.find(id)
    override fun findKkmForUpdate(id: String): KkmInfo? = kkms.find(id)
    override fun findKkmByRegistrationNumber(registrationNumber: String): KkmInfo? =
        kkms.byRegistrationNumber(registrationNumber)
    override fun findKkmBySystemId(systemId: String): KkmInfo? = kkms.bySystemId(systemId)
    override fun listKkms(limit: Int, offset: Int, state: String?, search: String?, sortBy: String, sortOrder: String) =
        kkms.list(limit, offset)
    override fun countKkms(state: String?, search: String?): Int = kkms.count()
    override fun deleteKkm(id: String): Boolean = kkms.delete(id)
    override fun updateKkmToken(id: String, tokenEncryptedBase64: String, updatedAt: Long) =
        kkms.updateToken(id, tokenEncryptedBase64, updatedAt)
    override fun createUser(
        kkmId: String,
        userId: String,
        name: String,
        role: UserRole,
        pinHash: String,
        createdAt: Long
    ) =
        kkms.createUser(kkmId, userId, name, role, pinHash, createdAt)
    override fun updateUser(kkmId: String, userId: String, name: String?, role: UserRole?, pinHash: String?) =
        kkms.updateUser(kkmId, userId, name, role, pinHash)
    override fun deleteUser(kkmId: String, userId: String): Boolean = kkms.deleteUser(kkmId, userId)
    override fun listUsers(kkmId: String): List<KkmUser> = kkms.users(kkmId)
    override fun findUserById(kkmId: String, userId: String): KkmUser? = kkms.userById(kkmId, userId)
    override fun findUserByPin(kkmId: String, pinHash: String): KkmUser? = kkms.userByPin(kkmId, pinHash)

    /** Касса уходит целиком: кассиры, смены, документы, счётчики, очередь, ключи повтора и счёт пинов. */
    override fun deleteKkmCompletely(kkmId: String): Boolean {
        kkms.deleteWithUsers(kkmId)
        pinAttempts.clear(kkmId)
        shifts.deleteByKkm(kkmId)
        documents.deleteByKkm(kkmId)
        queue.deleteByCashbox(kkmId)
        runBlocking { idempotencyDao.deleteByKkm(kkmId) }
        return true
    }

    // --- Смены и счётчики ---
    override fun findShiftById(shiftId: String): ShiftInfo? = shifts.find(shiftId)
    override fun findOpenShift(kkmId: String): ShiftInfo? = shifts.findOpen(kkmId)
    override fun listShifts(kkmId: String, limit: Int, offset: Int): List<ShiftInfo> = shifts.list(kkmId, limit, offset)
    override fun createShift(shift: ShiftInfo): Boolean = shifts.create(shift)
    override fun closeShift(shiftId: String, status: ShiftStatus, closedAt: Long, closeDocumentId: String?) =
        shifts.close(shiftId, status, closedAt, closeDocumentId)
    override fun countClosedShifts(): Long = 0L
    override fun loadCounters(kkmId: String, scope: String, shiftId: String?) =
        shifts.loadCounters(kkmId, scope, shiftId)
    override fun listCounters(kkmId: String): List<CounterSnapshot> = shifts.listCounters(kkmId)
    override fun upsertCounter(kkmId: String, scope: String, shiftId: String?, key: String, value: Long) =
        shifts.upsertCounter(kkmId, scope, shiftId, key, value)

    // --- Документы ---
    override fun saveReceipt(request: ReceiptRequest, documentId: String, shiftId: String, createdAt: Long) =
        documents.saveReceipt(request, documentId, shiftId, createdAt)
    override fun saveCashOperation(
        kkmId: String,
        type: String,
        amount: Money,
        documentId: String,
        shiftId: String,
        createdAt: Long
    ) =
        documents.saveCashOperation(kkmId, type, amount, documentId, shiftId, createdAt)
    override fun saveShiftDocument(kkmId: String, type: String, documentId: String, shiftId: String, createdAt: Long) =
        documents.saveShiftDocument(kkmId, type, documentId, shiftId, createdAt)
    override fun updateReceiptStatus(
        documentId: String,
        fiscalSign: String?,
        autonomousSign: String?,
        ofdStatus: String,
        ofdErrorCode: Int?,
        deliveredAt: Long?,
        isAutonomous: Boolean?,
        ofdErrorText: String?
    ): Boolean = documents.updateStatus(
        documentId,
        DeliveryUpdate(fiscalSign, autonomousSign, ofdStatus, ofdErrorCode, deliveredAt, isAutonomous, ofdErrorText)
    )
    override fun saveReceiptUrl(documentId: String, receiptUrl: String): Boolean =
        documents.saveReceiptUrl(documentId, receiptUrl)
    override fun updateDocumentNumber(documentId: String, docNo: Long): Boolean =
        documents.updateDocumentNumber(documentId, docNo)
    override fun updatePrintedDocumentNumber(documentId: String, number: Long) =
        documents.updatePrintedNumber(documentId, number)
    override fun findFiscalDocumentById(id: String): FiscalDocumentSnapshot? = reads.byId(id)
    override fun findFiscalDocumentWithReceiptPayload(documentId: String) = reads.withReceipt(documentId)
    override fun firstPaymentTimeInShift(shiftId: String): Long? = reads.firstPaymentTime(shiftId)
    override fun listFiscalDocumentsByShift(kkmId: String, shiftId: String, limit: Int, offset: Int) =
        reads.byShift(kkmId, shiftId, limit, offset)
    override fun listFiscalDocumentsByPeriod(
        kkmId: String,
        fromInclusive: Long,
        toExclusive: Long,
        limit: Int,
        offset: Int
    ) =
        reads.byPeriod(kkmId, fromInclusive, toExclusive, limit, offset)
    override fun countFiscalDocuments(docType: String?): Long = reads.count(docType)
}
