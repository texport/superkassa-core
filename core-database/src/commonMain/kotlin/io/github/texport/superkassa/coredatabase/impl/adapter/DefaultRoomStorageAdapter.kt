package io.github.texport.superkassa.coredatabase.impl.adapter

import io.github.texport.superkassa.core.domain.api.model.auth.KkmUser
import io.github.texport.superkassa.core.domain.api.model.auth.UserRole
import io.github.texport.superkassa.core.domain.api.model.common.CounterSnapshot
import io.github.texport.superkassa.core.domain.api.model.common.Money
import io.github.texport.superkassa.core.domain.api.model.kkm.FiscalDocumentSnapshot
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptDocumentTypes
import io.github.texport.superkassa.core.domain.api.model.kkm.KkmInfo
import io.github.texport.superkassa.core.domain.api.model.queue.QueueTask
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptRequest
import io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptStoredPayload
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftStatus
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.logging.getLogger
import io.github.texport.superkassa.coredatabase.impl.dao.CounterDao
import io.github.texport.superkassa.coredatabase.impl.dao.FiscalDocumentDao
import io.github.texport.superkassa.coredatabase.impl.dao.KkmDao
import io.github.texport.superkassa.coredatabase.impl.dao.KkmUserDao
import io.github.texport.superkassa.coredatabase.impl.dao.QueueCommandDao
import io.github.texport.superkassa.coredatabase.impl.dao.ShiftDao
import io.github.texport.superkassa.coredatabase.impl.entity.CounterEntity
import io.github.texport.superkassa.coredatabase.impl.entity.FiscalDocumentEntity
import io.github.texport.superkassa.coredatabase.impl.entity.KkmEntity
import io.github.texport.superkassa.coredatabase.impl.entity.KkmUserEntity
import io.github.texport.superkassa.coredatabase.impl.entity.QueueCommandEntity
import io.github.texport.superkassa.coredatabase.impl.entity.ShiftEntity
import io.github.texport.superkassa.offlinequeue.api.model.QueueCommand
import io.github.texport.superkassa.offlinequeue.api.model.QueueLane
import io.github.texport.superkassa.offlinequeue.api.model.QueueStatus
import io.github.texport.superkassa.offlinequeue.api.port.QueueStoragePort
import kotlinx.coroutines.runBlocking

/**
 * Реализация хранилища по умолчанию на базе Room KMP.
 *
 * Все данные ККМ, смен, кассиров, чеков, отчетов и счетчиков сохраняются 100% на диск в SQLite через Room DAO.
 * Хранение состояния в RAM (HashMap / mutableMapOf) полностью исключено.
 */
internal class DefaultRoomStorageAdapter(
    private val queueDao: QueueCommandDao,
    private val kkmDao: KkmDao,
    private val userDao: KkmUserDao,
    private val shiftDao: ShiftDao,
    private val fiscalDocumentDao: FiscalDocumentDao,
    private val counterDao: CounterDao
) : QueueStoragePort, StoragePort {

    private val logger = getLogger(DefaultRoomStorageAdapter::class)

    // --- QueueStoragePort ---

    override fun enqueue(command: QueueCommand): Boolean = runBlocking {
        logger.debug("enqueue: adding command id=${command.id} cashboxId=${command.cashboxId} lane=${command.lane}")
        queueDao.insert(QueueCommandEntity.fromDomain(command))
        logger.info("enqueue: successfully saved command id=${command.id} to SQLite DB queue")
        true
    }

    override fun getCommandsByStatus(cashboxId: String, lane: QueueLane, statuses: Set<QueueStatus>): List<QueueCommand> = runBlocking {
        val list = queueDao.getByStatus(cashboxId, lane.name, statuses.map { it.name })
            .map { it.toDomain() }
        logger.trace(
            "getCommandsByStatus: fetched ${list.size} commands for cashboxId=$cashboxId lane=$lane statuses=$statuses"
        )
        list
    }

    override fun updateStatus(
        id: String,
        status: QueueStatus,
        attempt: Int,
        lastError: String?,
        nextAttemptAt: Long?
    ): Boolean = runBlocking {
        logger.debug("updateStatus: updating command id=$id status=$status attempt=$attempt err=$lastError")
        queueDao.updateStatus(id, status.name, attempt, lastError, nextAttemptAt)
        logger.trace("updateStatus: command id=$id status updated to $status")
        true
    }

    override fun markInProgress(id: String, now: Long): Boolean = runBlocking {
        logger.debug("markInProgress: marking command id=$id IN_PROGRESS")
        queueDao.updateStatus(id, QueueStatus.IN_PROGRESS.name, 1, null, null)
        true
    }

    override fun listByCashbox(cashboxId: String, lane: QueueLane, limit: Int, offset: Int): List<QueueCommand> = runBlocking {
        val list = queueDao.listByCashbox(cashboxId, lane.name, limit, offset)
            .map { it.toDomain() }
        logger.trace("listByCashbox: retrieved ${list.size} commands for cashboxId=$cashboxId")
        list
    }

    override fun deleteByCashbox(cashboxId: String): Boolean = runBlocking {
        logger.info("deleteByCashbox: deleting queue commands for cashboxId=$cashboxId")
        queueDao.deleteByCashbox(cashboxId)
        true
    }

    // --- StoragePort ---

    override fun createKkm(info: KkmInfo): Boolean = runBlocking {
        logger.debug("createKkm: inserting KKM id=${info.id} regNo=${info.registrationNumber}")
        kkmDao.insert(KkmEntity.fromDomain(info))
        logger.info("createKkm: successfully saved KKM to SQLite DB: id=${info.id}")
        true
    }

    override fun updateKkm(info: KkmInfo): Boolean = runBlocking {
        logger.debug("updateKkm: updating KKM id=${info.id}")
        kkmDao.insert(KkmEntity.fromDomain(info))
        logger.info("updateKkm: successfully updated KKM in SQLite DB: id=${info.id}")
        true
    }

    override fun findKkm(id: String): KkmInfo? = runBlocking {
        val entity = kkmDao.getById(id)
        if (entity != null) {
            logger.trace("findKkm: found KKM in DB: id=${entity.id} regNo=${entity.registrationNumber}")
        } else {
            logger.debug("findKkm: KKM not found in DB: id=$id")
        }
        entity?.toDomain()
    }

    override fun findKkmForUpdate(id: String): KkmInfo? = findKkm(id)

    override fun findKkmByRegistrationNumber(registrationNumber: String): KkmInfo? = runBlocking {
        val result = kkmDao.list(10000, 0).find { it.registrationNumber == registrationNumber }?.toDomain()
        if (result != null) {
            logger.trace("findKkmByRegistrationNumber: found KKM regNo=$registrationNumber")
        } else {
            logger.debug("findKkmByRegistrationNumber: no KKM found with regNo=$registrationNumber")
        }
        result
    }

    override fun findKkmBySystemId(systemId: String): KkmInfo? = runBlocking {
        findKkm(systemId)
    }

    override fun listKkms(
        limit: Int,
        offset: Int,
        state: String?,
        search: String?,
        sortBy: String,
        sortOrder: String
    ): List<KkmInfo> = runBlocking {
        val list = kkmDao.list(limit, offset).map { it.toDomain() }
        logger.debug("listKkms: retrieved ${list.size} KKMs from DB (limit=$limit, offset=$offset)")
        list
    }

    override fun countKkms(state: String?, search: String?): Int = runBlocking {
        val count = kkmDao.list(10000, 0).size
        logger.trace("countKkms: count=$count")
        count
    }

    override fun deleteKkm(id: String): Boolean = runBlocking {
        logger.info("deleteKkm: deleting KKM id=$id")
        kkmDao.deleteById(id)
        true
    }

    override fun deleteKkmCompletely(kkmId: String): Boolean = runBlocking {
        logger.warn("deleteKkmCompletely: wiping all data for KKM kkmId=$kkmId")
        kkmDao.deleteById(kkmId)
        userDao.deleteByKkm(kkmId)
        shiftDao.deleteByKkm(kkmId)
        fiscalDocumentDao.deleteByKkm(kkmId)
        counterDao.deleteByPrefix("$kkmId:")
        logger.info("deleteKkmCompletely: successfully wiped all records for kkmId=$kkmId")
        true
    }

    override fun updateKkmToken(id: String, tokenEncryptedBase64: String, updatedAt: Long): Boolean = runBlocking {
        val existing = kkmDao.getById(id)?.toDomain() ?: return@runBlocking false
        val updated = existing.copy(
            tokenEncryptedBase64 = tokenEncryptedBase64,
            tokenUpdatedAt = updatedAt,
            updatedAt = updatedAt
        )
        kkmDao.insert(KkmEntity.fromDomain(updated))
        logger.info("updateKkmToken: updated token for KKM id=$id")
        true
    }

    override fun enqueueQueueTask(dto: QueueTask): Boolean = true
    override fun listQueueTasksByCashbox(
        cashboxId: String,
        lane: String,
        limit: Int,
        offset: Int
    ): List<QueueTask> = emptyList()
    override fun getQueueTasksByStatus(
        cashboxId: String,
        lane: String,
        statuses: Set<String>
    ): List<QueueTask> = emptyList()
    override fun updateQueueTaskStatus(
        id: String,
        status: String,
        attempt: Int,
        lastError: String?,
        nextAttemptAt: Long?
    ): Boolean = true
    override fun markQueueTaskInProgress(id: String, now: Long): Boolean = true
    override fun deleteQueueTasksByCashbox(cashboxId: String): Boolean = true

    override fun tryAcquireQueueLock(cashboxId: String, ownerId: String, leaseUntil: Long, acquiredAt: Long): Boolean = true
    override fun renewQueueLock(cashboxId: String, ownerId: String, leaseUntil: Long, now: Long): Boolean = true
    override fun releaseQueueLock(cashboxId: String, ownerId: String): Boolean = true

    override fun findShiftById(shiftId: String): ShiftInfo? = runBlocking {
        val result = shiftDao.getById(shiftId)?.toDomain()
        if (result != null) {
            logger.trace("findShiftById: found shift id=$shiftId shiftNo=${result.shiftNo}")
        } else {
            logger.debug("findShiftById: shift not found id=$shiftId")
        }
        result
    }

    override fun findOpenShift(kkmId: String): ShiftInfo? = runBlocking {
        val result = shiftDao.findOpenShift(kkmId)?.toDomain()
        if (result != null) {
            logger.debug("findOpenShift: found open shift id=${result.id} shiftNo=${result.shiftNo} for kkmId=$kkmId")
        } else {
            logger.debug("findOpenShift: no open shift for kkmId=$kkmId")
        }
        result
    }

    override fun listShifts(kkmId: String, limit: Int, offset: Int): List<ShiftInfo> = runBlocking {
        val list = shiftDao.listByKkm(kkmId, limit, offset).map { it.toDomain() }
        logger.debug("listShifts: retrieved ${list.size} shifts for kkmId=$kkmId")
        list
    }

    override fun createShift(shift: ShiftInfo): Boolean = runBlocking {
        logger.info("createShift: creating shift id=${shift.id} shiftNo=${shift.shiftNo} kkmId=${shift.kkmId}")
        shiftDao.insert(ShiftEntity.fromDomain(shift))
        logger.info("createShift: shift successfully created in DB: shiftNo=${shift.shiftNo}")
        true
    }

    override fun closeShift(
        shiftId: String,
        status: ShiftStatus,
        closedAt: Long,
        closeDocumentId: String?
    ): Boolean = runBlocking {
        logger.info("closeShift: closing shift id=$shiftId status=$status")
        val existing = findShiftById(shiftId) ?: run {
            logger.error("closeShift: shift not found for shiftId=$shiftId")
            return@runBlocking false
        }
        val updated = existing.copy(
            status = status,
            closedAt = closedAt,
            closeDocumentId = closeDocumentId
        )
        shiftDao.insert(ShiftEntity.fromDomain(updated))
        logger.info("closeShift: shift successfully closed in DB: shiftId=$shiftId")
        true
    }

    override fun saveReceipt(request: ReceiptRequest, documentId: String, shiftId: String, createdAt: Long): Boolean = runBlocking {
        logger.debug(
            "saveReceipt: saving receipt docId=$documentId kkmId=${request.kkmId} shiftId=$shiftId op=${request.operation}"
        )
        val kkm = findKkm(request.kkmId)
        val shift = findShiftById(shiftId)
        val docNo = (fiscalDocumentDao.listByShift(request.kkmId, shiftId, 10000, 0).size + 1).toLong()
        val doc = FiscalDocumentSnapshot(
            id = documentId,
            cashboxId = request.kkmId,
            shiftId = shiftId,
            docType = request.operation.name,
            docNo = docNo,
            shiftNo = shift?.shiftNo,
            createdAt = createdAt,
            totalAmount = request.total.bills,
            currency = "KZT",
            fiscalSign = null,
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = "PENDING",
            deliveredAt = null,
            registrationNumber = kkm?.registrationNumber,
            taxpayerName = kkm?.ofdServiceInfo?.orgTitle,
            ofdProvider = kkm?.ofdProvider
        )
        // Чек сохраняется целиком: без позиций и оплат фискальный документ
        // потом не собрать, а восстановить их неоткуда.
        val jsonPayload = receiptPayloadJson(request)
        fiscalDocumentDao.insert(FiscalDocumentEntity.fromDomain(doc, jsonPayload))
        logger.info("saveReceipt: successfully saved receipt docId=$documentId to SQLite DB")
        true
    }

    override fun saveCashOperation(
        kkmId: String,
        type: String,
        amount: Money,
        documentId: String,
        shiftId: String,
        createdAt: Long
    ): Boolean = runBlocking {
        logger.debug("saveCashOperation: saving docId=$documentId type=$type amount=${amount.bills} kkmId=$kkmId")
        val kkm = findKkm(kkmId)
        val shift = findShiftById(shiftId)
        val docNo = (fiscalDocumentDao.listByShift(kkmId, shiftId, 10000, 0).size + 1).toLong()
        val doc = FiscalDocumentSnapshot(
            id = documentId,
            cashboxId = kkmId,
            shiftId = shiftId,
            docType = type,
            docNo = docNo,
            shiftNo = shift?.shiftNo,
            createdAt = createdAt,
            totalAmount = amount.bills,
            currency = "KZT",
            fiscalSign = null,
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = "PENDING",
            deliveredAt = null,
            registrationNumber = kkm?.registrationNumber,
            taxpayerName = kkm?.ofdServiceInfo?.orgTitle,
            taxpayerBin = kkm?.ofdServiceInfo?.orgInn,
            taxpayerAddress = kkm?.ofdServiceInfo?.orgAddress,
            factoryNumber = kkm?.factoryNumber,
            ofdProvider = kkm?.ofdProvider
        )
        fiscalDocumentDao.insert(FiscalDocumentEntity.fromDomain(doc))
        logger.info("saveCashOperation: successfully saved cash document docId=$documentId to SQLite DB")
        true
    }

    override fun saveShiftDocument(
        kkmId: String,
        type: String,
        documentId: String,
        shiftId: String,
        createdAt: Long
    ): Boolean = runBlocking {
        logger.debug("saveShiftDocument: saving docId=$documentId type=$type kkmId=$kkmId")
        val kkm = findKkm(kkmId)
        val shift = findShiftById(shiftId)
        val docNo = (fiscalDocumentDao.listByShift(kkmId, shiftId, 10000, 0).size + 1).toLong()
        val doc = FiscalDocumentSnapshot(
            id = documentId,
            cashboxId = kkmId,
            shiftId = shiftId,
            docType = type,
            docNo = docNo,
            shiftNo = shift?.shiftNo,
            createdAt = createdAt,
            totalAmount = null,
            currency = "KZT",
            fiscalSign = null,
            autonomousSign = null,
            isAutonomous = false,
            ofdStatus = "PENDING",
            deliveredAt = null,
            registrationNumber = kkm?.registrationNumber,
            taxpayerName = kkm?.ofdServiceInfo?.orgTitle,
            taxpayerBin = kkm?.ofdServiceInfo?.orgInn,
            taxpayerAddress = kkm?.ofdServiceInfo?.orgAddress,
            factoryNumber = kkm?.factoryNumber,
            ofdProvider = kkm?.ofdProvider
        )
        fiscalDocumentDao.insert(FiscalDocumentEntity.fromDomain(doc))
        logger.info("saveShiftDocument: successfully saved shift report docId=$documentId to SQLite DB")
        true
    }

    override fun updateReceiptStatus(
        documentId: String,
        fiscalSign: String?,
        autonomousSign: String?,
        ofdStatus: String,
        ofdErrorCode: Int?,
        deliveredAt: Long?,
        isAutonomous: Boolean?,
        ofdErrorText: String?
    ): Boolean = runBlocking {
        logger.debug("updateReceiptStatus: updating docId=$documentId status=$ofdStatus sign=$fiscalSign")
        val current = findFiscalDocumentById(documentId) ?: run {
            logger.warn("updateReceiptStatus: document not found for id=$documentId")
            return@runBlocking false
        }
        val updated = current.copy(
            fiscalSign = fiscalSign ?: current.fiscalSign,
            autonomousSign = autonomousSign ?: current.autonomousSign,
            ofdStatus = ofdStatus,
            ofdErrorCode = ofdErrorCode ?: current.ofdErrorCode,
            ofdErrorText = ofdErrorText ?: current.ofdErrorText,
            deliveredAt = deliveredAt,
            isAutonomous = isAutonomous ?: current.isAutonomous
        )
        fiscalDocumentDao.insert(FiscalDocumentEntity.fromDomain(updated, storedPayload(documentId)))
        logger.info("updateReceiptStatus: updated document status to $ofdStatus for docId=$documentId")
        true
    }

    override fun firstPaymentTimeInShift(shiftId: String): Long? = runBlocking {
        fiscalDocumentDao.firstPaymentTime(shiftId, ReceiptDocumentTypes.ALL)
    }

    override fun listFiscalDocumentsByShift(kkmId: String, shiftId: String, limit: Int, offset: Int): List<FiscalDocumentSnapshot> = runBlocking {
        val list = fiscalDocumentDao.listByShift(kkmId, shiftId, limit, offset).map { it.toDomain() }
        logger.trace("listFiscalDocumentsByShift: retrieved ${list.size} docs for shiftId=$shiftId")
        list
    }

    override fun listFiscalDocumentsByPeriod(
        kkmId: String,
        fromInclusive: Long,
        toExclusive: Long,
        limit: Int,
        offset: Int
    ): List<FiscalDocumentSnapshot> = runBlocking {
        val list = fiscalDocumentDao.listByPeriod(
            kkmId,
            fromInclusive,
            toExclusive,
            limit,
            offset
        ).map { it.toDomain() }
        logger.trace("listFiscalDocumentsByPeriod: retrieved ${list.size} docs for period $fromInclusive..$toExclusive")
        list
    }

    override fun findFiscalDocumentById(id: String): FiscalDocumentSnapshot? = runBlocking {
        val doc = fiscalDocumentDao.getById(id)?.toDomain()
        if (doc != null) {
            logger.trace("findFiscalDocumentById: found doc id=$id type=${doc.docType}")
        } else {
            logger.debug("findFiscalDocumentById: doc not found id=$id")
        }
        doc
    }

    override fun updatePrintedDocumentNumber(documentId: String, number: Long): Boolean = runBlocking {
        val current = findFiscalDocumentById(documentId) ?: return@runBlocking false
        val updated = current.copy(printedDocumentNumber = number)
        fiscalDocumentDao.insert(FiscalDocumentEntity.fromDomain(updated, storedPayload(documentId)))
        true
    }

    override fun updateDocumentNumber(documentId: String, docNo: Long): Boolean = runBlocking {
        logger.debug("updateDocumentNumber: docId=$documentId docNo=$docNo")
        val current = findFiscalDocumentById(documentId) ?: return@runBlocking false
        val updated = current.copy(docNo = docNo)
        fiscalDocumentDao.insert(FiscalDocumentEntity.fromDomain(updated, storedPayload(documentId)))
        true
    }

    override fun findFiscalDocumentWithReceiptPayload(
        documentId: String
    ): Pair<FiscalDocumentSnapshot, ReceiptRequest>? = runBlocking {
        val entity = fiscalDocumentDao.getById(documentId) ?: return@runBlocking null
        val doc = entity.toDomain()
        val op = try {
            io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType.valueOf(doc.docType)
        } catch (_: Exception) {
            io.github.texport.superkassa.core.domain.api.model.receipt.ReceiptOperationType.SELL
        }
        val restored = restoreReceipt(entity.receiptPayloadJson)
        if (restored != null) {
            return@runBlocking doc to restored
        }
        logger.warn(
            "findFiscalDocumentWithReceiptPayload: no stored receipt for docId=$documentId, " +
                "returning totals only"
        )
        return@runBlocking doc to ReceiptRequest(
            kkmId = doc.cashboxId,
            pin = "",
            operation = op,
            items = emptyList(),
            payments = emptyList(),
            total = Money(bills = doc.totalAmount ?: 0L, coins = 0),
            idempotencyKey = ""
        )
    }

    /** Формат хранения чека: неизвестные ключи не роняют разбор старых записей. */
    private val receiptJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    /** Сериализует чек для хранения рядом с фискальным документом. */
    private fun receiptPayloadJson(request: ReceiptRequest): String =
        receiptJson.encodeToString(ReceiptStoredPayload.fromReceiptRequest(request))

    /**
     * Восстанавливает чек из сохранённого представления.
     *
     * Возвращает null, если сохранённого чека нет или он не разбирается:
     * подставлять пустой чек вместо настоящего нельзя, из него собрался бы
     * фискальный документ без позиций и оплат.
     */
    private fun restoreReceipt(payloadJson: String?): ReceiptRequest? {
        val raw = payloadJson?.takeIf { it.isNotBlank() } ?: return null
        return try {
            val stored = receiptJson.decodeFromString<ReceiptStoredPayload>(raw)
            // toReceiptRequest намеренно очищает ключ идемпотентности как
            // конфиденциальный. Хранилище его хранит, поэтому возвращает само.
            stored.toReceiptRequest().copy(idempotencyKey = stored.idempotencyKey)
        } catch (e: kotlinx.serialization.SerializationException) {
            logger.warn("restoreReceipt: stored receipt cannot be parsed: ${e.message}")
            null
        }
    }

    /** Сохранённый чек документа: обновления статуса не должны его стирать. */
    private suspend fun storedPayload(documentId: String): String? =
        fiscalDocumentDao.getById(documentId)?.receiptPayloadJson

    override fun countFiscalDocuments(docType: String?): Long = runBlocking {
        fiscalDocumentDao.listByPeriod("", 0, Long.MAX_VALUE, 10000, 0).let { docs ->
            if (docType == null) docs.size.toLong() else docs.count { it.docType == docType }.toLong()
        }
    }
    override fun countClosedShifts(): Long = 0L
    override fun countOfflineQueue(): Long = 0L

    override fun loadCounters(kkmId: String, scope: String, shiftId: String?): Map<String, Long> = runBlocking {
        val prefix = "$kkmId:$scope:${shiftId ?: ""}:"
        val map = counterDao.listByPrefix(prefix).associate { it.key.removePrefix(prefix) to it.value }
        logger.trace("loadCounters: loaded ${map.size} counters for scope=$scope shiftId=$shiftId")
        map
    }

    override fun listCounters(kkmId: String): List<CounterSnapshot> = runBlocking {
        val prefix = "$kkmId:"
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val list = counterDao.listByPrefix(prefix).map { (k, v) ->
            val parts = k.split(":")
            CounterSnapshot(
                scope = parts.getOrNull(1) ?: "",
                shiftId = parts.getOrNull(2).takeIf { !it.isNullOrEmpty() },
                key = parts.getOrNull(3) ?: "",
                value = v,
                updatedAt = now
            )
        }
        logger.trace("listCounters: retrieved ${list.size} counter snapshots for kkmId=$kkmId")
        list
    }

    override fun upsertCounter(kkmId: String, scope: String, shiftId: String?, key: String, value: Long): Boolean = runBlocking {
        val compositeKey = "$kkmId:$scope:${shiftId ?: ""}:$key"
        logger.trace("upsertCounter: key=$compositeKey value=$value")
        counterDao.insert(CounterEntity(compositeKey, value))
        true
    }

    override fun createUser(
        kkmId: String,
        userId: String,
        name: String,
        role: UserRole,
        pinHash: String,
        createdAt: Long
    ): Boolean = runBlocking {
        logger.info("createUser: inserting user id=$userId name=$name role=$role kkmId=$kkmId")
        userDao.insert(
            KkmUserEntity(
                id = userId,
                kkmId = kkmId,
                name = name,
                role = role.name,
                pinHash = pinHash,
                createdAt = createdAt
            )
        )
        logger.info("createUser: user successfully created in DB: id=$userId")
        true
    }

    override fun updateUser(
        kkmId: String,
        userId: String,
        name: String?,
        role: UserRole?,
        pinHash: String?
    ): Boolean = runBlocking {
        logger.debug("updateUser: updating user id=$userId")
        val existing = userDao.getById(userId) ?: run {
            logger.warn("updateUser: user not found id=$userId")
            return@runBlocking false
        }
        val updated = existing.copy(
            name = name ?: existing.name,
            role = role?.name ?: existing.role,
            pinHash = pinHash ?: existing.pinHash
        )
        userDao.insert(updated)
        logger.info("updateUser: user successfully updated in DB: id=$userId")
        true
    }

    override fun deleteUser(kkmId: String, userId: String): Boolean = runBlocking {
        logger.info("deleteUser: deleting user id=$userId kkmId=$kkmId")
        userDao.deleteById(userId)
        true
    }

    override fun listUsers(kkmId: String): List<KkmUser> = runBlocking {
        val list = userDao.listByKkm(kkmId).map {
            KkmUser(
                id = it.id,
                name = it.name,
                role = UserRole.valueOf(it.role),
                createdAt = it.createdAt
            )
        }
        logger.trace("listUsers: retrieved ${list.size} users for kkmId=$kkmId")
        list
    }

    override fun findUserByPin(kkmId: String, pinHash: String): KkmUser? = runBlocking {
        val user = userDao.listByKkm(kkmId).find { it.pinHash == pinHash }?.let {
            KkmUser(
                id = it.id,
                name = it.name,
                role = UserRole.valueOf(it.role),
                createdAt = it.createdAt
            )
        }
        if (user != null) {
            logger.trace("findUserByPin: authenticated user id=${user.id} name=${user.name}")
        } else {
            logger.debug("findUserByPin: user authentication failed for kkmId=$kkmId")
        }
        user
    }

    override fun findUserById(kkmId: String, userId: String): KkmUser? = runBlocking {
        val user = userDao.getById(userId)?.let {
            KkmUser(
                id = it.id,
                name = it.name,
                role = UserRole.valueOf(it.role),
                createdAt = it.createdAt
            )
        }
        if (user != null) {
            logger.trace("findUserById: found user id=$userId name=${user.name}")
        } else {
            logger.debug("findUserById: user not found id=$userId")
        }
        user
    }

    override fun insertIdempotency(kkmId: String, idempotencyKey: String, operation: String): Boolean = true
    override fun findIdempotencyResponse(kkmId: String, idempotencyKey: String): String? = null
    override fun updateIdempotencyResponse(kkmId: String, idempotencyKey: String, responseRef: String?): Boolean = true
}
