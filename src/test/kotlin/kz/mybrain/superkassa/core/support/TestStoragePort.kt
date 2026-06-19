package kz.mybrain.superkassa.core.support

import kz.mybrain.superkassa.core.domain.model.CounterSnapshot
import kz.mybrain.superkassa.core.domain.model.FiscalDocumentSnapshot
import kz.mybrain.superkassa.core.domain.model.KkmInfo
import kz.mybrain.superkassa.core.domain.model.KkmUser
import kz.mybrain.superkassa.core.domain.model.Money
import kz.mybrain.superkassa.core.domain.model.QueueTaskDto
import kz.mybrain.superkassa.core.domain.model.ReceiptRequest
import kz.mybrain.superkassa.core.domain.model.ShiftInfo
import kz.mybrain.superkassa.core.domain.model.ShiftStatus
import kz.mybrain.superkassa.core.domain.model.UserRole
import kz.mybrain.superkassa.core.domain.port.StoragePort

/**
 * Простая in-memory реализация StoragePort для unit/integration тестов core-слоя.
 */
class TestStoragePort : StoragePort {
    private val counters = mutableMapOf<String, MutableMap<String, Long>>()
    private val kkms = mutableMapOf<String, KkmInfo>()
    private val users = mutableMapOf<String, MutableList<KkmUser>>()
    private val userPinHashes = mutableMapOf<String, MutableMap<String, String>>()
    private val shifts = mutableMapOf<String, ShiftInfo>()
    private val documents = mutableMapOf<String, FiscalDocumentSnapshot>()
    private val receiptPayloads = mutableMapOf<String, ReceiptRequest>()
    private val idempotency = mutableMapOf<String, MutableMap<String, String?>>()
    private val queueTasks = mutableListOf<QueueTaskDto>()
    private val queueLocks = mutableMapOf<String, RoomQueueLock>()

    data class RoomQueueLock(val cashboxId: String, val ownerId: String, val leaseUntil: Long, val acquiredAt: Long)

    override fun <T> inTransaction(block: () -> T): T = block()

    override fun createKkm(info: KkmInfo): Boolean {
        kkms[info.id] = info
        return true
    }

    override fun updateKkm(info: KkmInfo): Boolean {
        kkms[info.id] = info
        return true
    }

    override fun findKkm(id: String): KkmInfo? = kkms[id]

    override fun findKkmByRegistrationNumber(registrationNumber: String): KkmInfo? =
        kkms.values.firstOrNull { it.registrationNumber == registrationNumber }

    override fun findKkmBySystemId(systemId: String): KkmInfo? =
        kkms.values.firstOrNull { it.systemId == systemId }

    override fun listKkms(
        limit: Int,
        offset: Int,
        state: String?,
        search: String?,
        sortBy: String,
        sortOrder: String
    ): List<KkmInfo> {
        var filtered = kkms.values.asSequence()
        if (state != null) filtered = filtered.filter { it.state == state }
        if (search != null) {
            filtered =
                filtered.filter {
                    it.registrationNumber?.contains(search, ignoreCase = true) == true ||
                        it.id.contains(search, ignoreCase = true)
                }
        }
        return filtered.drop(offset).take(limit).toList()
    }

    override fun countKkms(state: String?, search: String?): Int {
        var filtered = kkms.values.asSequence()
        if (state != null) filtered = filtered.filter { it.state == state }
        if (search != null) {
            filtered =
                filtered.filter {
                    it.registrationNumber?.contains(search, ignoreCase = true) == true ||
                        it.id.contains(search, ignoreCase = true)
                }
        }
        return filtered.count()
    }

    override fun deleteKkm(id: String): Boolean = kkms.remove(id) != null

    override fun hasOfflineQueue(kkmId: String): Boolean = false

    override fun enqueueQueueTask(dto: QueueTaskDto): Boolean {
        queueTasks.add(dto)
        return true
    }

    override fun listQueueTasksByCashbox(
        cashboxId: String,
        lane: String,
        limit: Int,
        offset: Int
    ): List<QueueTaskDto> = queueTasks.filter { it.cashboxId == cashboxId && it.lane == lane }
        .drop(offset).take(limit)

    override fun nextPendingQueueTask(cashboxId: String, lane: String, now: Long): QueueTaskDto? =
        queueTasks.firstOrNull { it.cashboxId == cashboxId && it.lane == lane && it.status == "PENDING" && (it.nextAttemptAt == null || now >= it.nextAttemptAt) }

    override fun updateQueueTaskStatus(
        id: String,
        status: String,
        attempt: Int,
        lastError: String?,
        nextAttemptAt: Long?
    ): Boolean {
        val index = queueTasks.indexOfFirst { it.id == id }
        if (index == -1) return false
        val current = queueTasks[index]
        queueTasks[index] = current.copy(
            status = status,
            attempt = attempt,
            lastError = lastError,
            nextAttemptAt = nextAttemptAt
        )
        return true
    }

    override fun markQueueTaskInProgress(id: String, now: Long): Boolean = true

    override fun deleteQueueTasksByCashbox(cashboxId: String): Boolean {
        queueTasks.removeIf { it.cashboxId == cashboxId }
        return true
    }

    override fun tryAcquireQueueLock(
        cashboxId: String,
        ownerId: String,
        leaseUntil: Long,
        acquiredAt: Long
    ): Boolean {
        val lock = queueLocks[cashboxId]
        val now = System.currentTimeMillis()
        return if (lock == null || lock.leaseUntil < now) {
            queueLocks[cashboxId] = RoomQueueLock(cashboxId, ownerId, leaseUntil, acquiredAt)
            true
        } else {
            lock.ownerId == ownerId
        }
    }

    override fun renewQueueLock(cashboxId: String, ownerId: String, leaseUntil: Long, now: Long): Boolean {
        val lock = queueLocks[cashboxId]
        return if (lock != null && lock.ownerId == ownerId && lock.leaseUntil >= now) {
            queueLocks[cashboxId] = lock.copy(leaseUntil = leaseUntil)
            true
        } else {
            false
        }
    }

    override fun releaseQueueLock(cashboxId: String, ownerId: String): Boolean {
        val lock = queueLocks[cashboxId]
        return if (lock != null && lock.ownerId == ownerId) {
            queueLocks.remove(cashboxId)
            true
        } else {
            false
        }
    }

    override fun deleteKkmCompletely(kkmId: String): Boolean {
        kkms.remove(kkmId)
        users.remove(kkmId)
        shifts.values.removeIf { it.kkmId == kkmId }
        documents.values.removeIf { it.cashboxId == kkmId }
        receiptPayloads.keys.retainAll(documents.keys)
        queueTasks.removeIf { it.cashboxId == kkmId }
        queueLocks.remove(kkmId)
        return true
    }

    override fun updateKkmToken(id: String, tokenEncryptedBase64: String, updatedAt: Long): Boolean {
        val current = kkms[id] ?: return false
        kkms[id] =
            current.copy(
                tokenEncryptedBase64 = tokenEncryptedBase64,
                tokenUpdatedAt = updatedAt,
                updatedAt = updatedAt
            )
        return true
    }

    override fun listUsers(kkmId: String): List<KkmUser> = users[kkmId]?.toList() ?: emptyList()

    override fun createUser(
        kkmId: String,
        userId: String,
        name: String,
        role: UserRole,
        pin: String,
        pinHash: String,
        createdAt: Long
    ): Boolean {
        val list = users.getOrPut(kkmId) { mutableListOf() }
        list.add(KkmUser(userId, name, role, pin, createdAt))
        userPinHashes.getOrPut(kkmId) { mutableMapOf() }[userId] = pinHash
        return true
    }

    override fun updateUser(
        kkmId: String,
        userId: String,
        name: String?,
        role: UserRole?,
        pin: String?,
        pinHash: String?
    ): Boolean {
        val list = users[kkmId] ?: return false
        val index = list.indexOfFirst { it.id == userId }
        if (index == -1) return false
        val current = list[index]
        list[index] =
            current.copy(
                name = name ?: current.name,
                role = role ?: current.role,
                pin = pin ?: current.pin
            )
        if (pinHash != null) {
            userPinHashes.getOrPut(kkmId) { mutableMapOf() }[userId] = pinHash
        }
        return true
    }

    override fun deleteUser(kkmId: String, userId: String): Boolean {
        val list = users[kkmId] ?: return false
        userPinHashes[kkmId]?.remove(userId)
        return list.removeIf { it.id == userId }
    }

    override fun findUserById(kkmId: String, userId: String): KkmUser? =
        users[kkmId]?.firstOrNull { it.id == userId }

    override fun findUserByPin(kkmId: String, pinHash: String): KkmUser? {
        val hashes = userPinHashes[kkmId] ?: return null
        val userId = hashes.entries.firstOrNull { it.value == pinHash }?.key ?: return null
        return findUserById(kkmId, userId)
    }

    override fun findOpenShift(kkmId: String): ShiftInfo? =
        shifts.values
            .firstOrNull { it.kkmId == kkmId && it.status == ShiftStatus.OPEN }

    override fun findShiftById(shiftId: String): ShiftInfo? = shifts[shiftId]

    override fun listShifts(kkmId: String, limit: Int, offset: Int): List<ShiftInfo> =
        shifts.values
            .filter { it.kkmId == kkmId }
            .sortedByDescending { it.openedAt }
            .drop(offset)
            .take(limit)

    override fun createShift(shift: ShiftInfo): Boolean {
        shifts[shift.id] = shift
        return true
    }

    override fun closeShift(
        shiftId: String,
        status: ShiftStatus,
        closedAt: Long,
        closeDocumentId: String?
    ): Boolean {
        val current = shifts[shiftId] ?: return false
        shifts[shiftId] =
            current.copy(
                status = status,
                closedAt = closedAt
            )
        return true
    }

    override fun saveReceipt(request: ReceiptRequest, documentId: String, shiftId: String, createdAt: Long): Boolean {
        val shift = shifts[shiftId]
        val snapshot =
            FiscalDocumentSnapshot(
                id = documentId,
                cashboxId = request.kkmId,
                shiftId = shiftId,
                docType = "CHECK",
                docNo = null,
                shiftNo = shift?.shiftNo,
                createdAt = createdAt,
                totalAmount = request.total.bills,
                currency = "KZT",
                fiscalSign = null,
                autonomousSign = null,
                isAutonomous = false,
                ofdStatus = "PENDING",
                deliveredAt = null
            )
        documents[documentId] = snapshot
        receiptPayloads[documentId] = request
        return true
    }

    override fun saveCashOperation(
        kkmId: String,
        type: String,
        amount: Money,
        documentId: String,
        shiftId: String,
        createdAt: Long
    ): Boolean {
        val shift = shifts[shiftId]
        documents[documentId] =
            FiscalDocumentSnapshot(
                id = documentId,
                cashboxId = kkmId,
                shiftId = shiftId,
                docType = type,
                docNo = null,
                shiftNo = shift?.shiftNo,
                createdAt = createdAt,
                totalAmount = amount.bills,
                currency = "KZT",
                fiscalSign = null,
                autonomousSign = null,
                isAutonomous = false,
                ofdStatus = "PENDING",
                deliveredAt = null
            )
        return true
    }

    override fun updateReceiptStatus(
        documentId: String,
        fiscalSign: String?,
        autonomousSign: String?,
        ofdStatus: String,
        deliveredAt: Long?,
        isAutonomous: Boolean?
    ): Boolean {
        val current = documents[documentId] ?: return false
        documents[documentId] =
            current.copy(
                fiscalSign = fiscalSign ?: current.fiscalSign,
                autonomousSign = autonomousSign ?: current.autonomousSign,
                ofdStatus = ofdStatus,
                deliveredAt = deliveredAt,
                isAutonomous = isAutonomous ?: current.isAutonomous
            )
        return true
    }

    override fun findFiscalDocumentById(id: String): FiscalDocumentSnapshot? = documents[id]

    override fun findFiscalDocumentWithReceiptPayload(
        documentId: String
    ): Pair<FiscalDocumentSnapshot, ReceiptRequest>? {
        val doc = documents[documentId] ?: return null
        val receipt = receiptPayloads[documentId] ?: return null
        return doc to receipt
    }

    override fun listFiscalDocumentsByShift(
        kkmId: String,
        shiftId: String,
        limit: Int,
        offset: Int
    ): List<FiscalDocumentSnapshot> =
        documents.values
            .filter { it.cashboxId == kkmId && it.shiftId == shiftId }
            .sortedBy { it.createdAt }
            .drop(offset)
            .take(limit)

    override fun listFiscalDocumentsByPeriod(
        kkmId: String,
        fromInclusive: Long,
        toExclusive: Long,
        limit: Int,
        offset: Int
    ): List<FiscalDocumentSnapshot> =
        documents.values
            .filter { it.cashboxId == kkmId && it.createdAt >= fromInclusive && it.createdAt < toExclusive }
            .sortedBy { it.createdAt }
            .drop(offset)
            .take(limit)

    override fun countFiscalDocuments(docType: String?): Long =
        if (docType == null) documents.size.toLong() else documents.values.count { it.docType == docType }.toLong()

    override fun countClosedShifts(): Long = shifts.values.count { it.status == ShiftStatus.CLOSED }.toLong()

    override fun countOfflineQueue(): Long = 0L

    override fun loadCounters(kkmId: String, scope: String, shiftId: String?): Map<String, Long> {
        val mapKey = "$kkmId:$scope:${shiftId ?: "-"}"
        return counters[mapKey]?.toMap() ?: emptyMap()
    }

    override fun listCounters(kkmId: String): List<CounterSnapshot> =
        counters.flatMap { (mapKey, values) ->
            val parts = mapKey.split(":", limit = 3)
            if (parts.size != 3 || parts[0] != kkmId) {
                emptyList()
            } else {
                val scope = parts[1]
                val shiftId = parts[2].takeIf { it != "-" }
                values.map { (key, value) ->
                    CounterSnapshot(
                        scope = scope,
                        shiftId = shiftId,
                        key = key,
                        value = value,
                        updatedAt = 0L
                    )
                }
            }
        }

    override fun upsertCounter(kkmId: String, scope: String, shiftId: String?, key: String, value: Long): Boolean {
        val mapKey = "$kkmId:$scope:${shiftId ?: "-"}"
        val map = counters.getOrPut(mapKey) { mutableMapOf() }
        map[key] = value
        return true
    }

    override fun insertIdempotency(kkmId: String, idempotencyKey: String, operation: String): Boolean {
        val map = idempotency.getOrPut(kkmId) { mutableMapOf() }
        if (map.containsKey(idempotencyKey)) return false
        map[idempotencyKey] = null
        return true
    }

    override fun findIdempotencyResponse(kkmId: String, idempotencyKey: String): String? =
        idempotency[kkmId]?.get(idempotencyKey)

    override fun updateIdempotencyResponse(kkmId: String, idempotencyKey: String, responseRef: String?): Boolean {
        val map = idempotency.getOrPut(kkmId) { mutableMapOf() }
        if (!map.containsKey(idempotencyKey)) return false
        map[idempotencyKey] = responseRef
        return true
    }

    fun addFiscalDocument(document: FiscalDocumentSnapshot, receipt: ReceiptRequest? = null) {
        documents[document.id] = document
        if (receipt != null) {
            receiptPayloads[document.id] = receipt
        }
    }

    fun clearAll() {
        counters.clear()
        kkms.clear()
        users.clear()
        userPinHashes.clear()
        shifts.clear()
        documents.clear()
        receiptPayloads.clear()
        idempotency.clear()
        queueTasks.clear()
        queueLocks.clear()
    }
}
