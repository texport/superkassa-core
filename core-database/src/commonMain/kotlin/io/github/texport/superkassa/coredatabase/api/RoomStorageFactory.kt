package io.github.texport.superkassa.coredatabase.api

import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.coredatabase.impl.adapter.DefaultRoomStorageAdapter
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
import io.github.texport.superkassa.offlinequeue.api.port.QueueStoragePort

import io.github.texport.superkassa.core.domain.impl.logging.getLogger

import kotlinx.coroutines.runBlocking

/**
 * Публичная фабрика создания локального хранилища данных на базе Room KMP.
 */
object RoomStorageFactory {

    private val logger = getLogger(RoomStorageFactory::class)

    /**
     * Создает экземпляр адаптера локального хранилища Room KMP со встроенным SQLite-движком.
     *
     * @param dbPath Опциональный путь к файлу SQLite базы данных (по умолчанию "superkassa.db").
     * @return Пара интерфейсов [StoragePort] и [QueueStoragePort].
     */
    fun createRoomStorage(dbPath: String = "superkassa.db"): RoomStoragePair {
        logger.info("Initializing Room KMP SQLite storage with dbPath='{}'...", dbPath)
        return try {
            val builder = getDatabaseBuilder(dbPath)
            val db = getRoomDatabase(builder)
            runBlocking {
                val warmupEntity = KkmEntity(
                    id = "warmup_schema_test",
                    registrationNumber = null,
                    factoryNumber = null,
                    state = "TEST",
                    mode = "TEST",
                    autoCloseShift = false,
                    autoCashout = false,
                    tokenEncryptedBase64 = null,
                    tokenUpdatedAt = null,
                    createdAt = 0L,
                    updatedAt = 0L,
                    systemId = null,
                    ofdProvider = null,
                    lastShiftNo = null,
                    taxRegime = null,
                    defaultVatGroup = null
                )
                db.kkmDao().insert(warmupEntity)
                db.kkmDao().deleteById("warmup_schema_test")
            }
            val adapter = DefaultRoomStorageAdapter(
                queueDao = db.queueCommandDao(),
                kkmDao = db.kkmDao(),
                userDao = db.kkmUserDao(),
                shiftDao = db.shiftDao(),
                fiscalDocumentDao = db.fiscalDocumentDao(),
                counterDao = db.counterDao()
            )
            logger.info("Room KMP SQLite DB successfully opened and initialized for '{}'", dbPath)
            RoomStoragePair(adapter, adapter)
        } catch (e: Throwable) {
            logger.error("ERROR initializing Room KMP SQLite DB for '$dbPath'", e)
            try {
                deleteDatabaseFile(dbPath)
                val builder = getDatabaseBuilder(dbPath)
                val db = getRoomDatabase(builder)
                runBlocking {
                    val warmupEntity = KkmEntity(
                        id = "warmup_schema_test",
                        registrationNumber = null,
                        factoryNumber = null,
                        state = "TEST",
                        mode = "TEST",
                        autoCloseShift = false,
                        autoCashout = false,
                        tokenEncryptedBase64 = null,
                        tokenUpdatedAt = null,
                        createdAt = 0L,
                        updatedAt = 0L,
                        systemId = null,
                        ofdProvider = null,
                        lastShiftNo = null,
                        taxRegime = null,
                        defaultVatGroup = null
                    )
                    db.kkmDao().insert(warmupEntity)
                    db.kkmDao().deleteById("warmup_schema_test")
                }
                val adapter = DefaultRoomStorageAdapter(
                    queueDao = db.queueCommandDao(),
                    kkmDao = db.kkmDao(),
                    userDao = db.kkmUserDao(),
                    shiftDao = db.shiftDao(),
                    fiscalDocumentDao = db.fiscalDocumentDao(),
                    counterDao = db.counterDao()
                )
                logger.warn("Re-created clean Room KMP SQLite DB for '{}' after initialization error", dbPath)
                RoomStoragePair(adapter, adapter)
            } catch (e2: Throwable) {
                logger.error("CRITICAL SECONDARY ERROR initializing Room DB for '$dbPath'", e2)
                createDefaultStorage()
            }
        }
    }

    /**
     * Создает экземпляр in-memory хранилища для юнит-тестирования без привязки к дисковой SQLite БД.
     *
     * @return Пара интерфейсов [StoragePort] и [QueueStoragePort].
     */
    fun createDefaultStorage(): RoomStoragePair {
        val queueMap = mutableMapOf<String, QueueCommandEntity>()
        val kkmMap = mutableMapOf<String, KkmEntity>()
        val userMap = mutableMapOf<String, KkmUserEntity>()
        val shiftMap = mutableMapOf<String, ShiftEntity>()
        val docMap = mutableMapOf<String, FiscalDocumentEntity>()
        val counterMap = mutableMapOf<String, CounterEntity>()

        val queueDao = object : QueueCommandDao {
            override suspend fun insert(entity: QueueCommandEntity) { queueMap[entity.id] = entity }
            override suspend fun getByStatus(
                cashboxId: String,
                lane: String,
                statuses: List<String>
            ): List<QueueCommandEntity> =
                queueMap.values.filter { it.cashboxId == cashboxId && it.lane == lane && it.status in statuses }
            override suspend fun listByCashbox(
                cashboxId: String,
                lane: String,
                limit: Int,
                offset: Int
            ): List<QueueCommandEntity> =
                queueMap.values.filter { it.cashboxId == cashboxId && it.lane == lane }.drop(offset).take(limit)
            override suspend fun updateStatus(
                id: String,
                status: String,
                attempt: Int,
                lastError: String?,
                nextAttemptAt: Long?
            ) {
                queueMap[id]?.let {
                    queueMap[id] = it.copy(status = status, attempt = attempt, lastError = lastError, nextAttemptAt = nextAttemptAt)
                }
            }
            override suspend fun deleteByCashbox(cashboxId: String) {
                queueMap.entries.removeAll { it.value.cashboxId == cashboxId }
            }
        }

        val kkmDao = object : KkmDao {
            override suspend fun insert(entity: KkmEntity) { kkmMap[entity.id] = entity }
            override suspend fun getById(id: String): KkmEntity? = kkmMap[id]
            override suspend fun list(limit: Int, offset: Int): List<KkmEntity> = kkmMap.values.drop(offset).take(limit)
            override suspend fun deleteById(id: String) { kkmMap.remove(id) }
        }

        val userDao = object : KkmUserDao {
            override suspend fun insert(entity: KkmUserEntity) { userMap[entity.id] = entity }
            override suspend fun getById(id: String): KkmUserEntity? = userMap[id]
            override suspend fun listByKkm(
                kkmId: String
            ): List<KkmUserEntity> = userMap.values.filter { it.kkmId == kkmId }
            override suspend fun deleteById(id: String) { userMap.remove(id) }
            override suspend fun deleteByKkm(kkmId: String) { userMap.entries.removeAll { it.value.kkmId == kkmId } }
        }

        val shiftDao = object : ShiftDao {
            override suspend fun insert(entity: ShiftEntity) { shiftMap[entity.id] = entity }
            override suspend fun getById(id: String): ShiftEntity? = shiftMap[id]
            override suspend fun findOpenShift(kkmId: String): ShiftEntity? = shiftMap.values.firstOrNull {
                it.kkmId == kkmId && it.status == "OPEN"
            }
            override suspend fun listByKkm(
                kkmId: String,
                limit: Int,
                offset: Int
            ): List<ShiftEntity> = shiftMap.values.filter {
                it.kkmId == kkmId
            }.drop(
                offset
            ).take(limit)
            override suspend fun deleteByKkm(kkmId: String) { shiftMap.entries.removeAll { it.value.kkmId == kkmId } }
        }

        val fiscalDocumentDao = object : FiscalDocumentDao {
            override suspend fun insert(entity: FiscalDocumentEntity) { docMap[entity.id] = entity }
            override suspend fun getById(id: String): FiscalDocumentEntity? = docMap[id]
            override suspend fun firstPaymentTime(shiftId: String, docTypes: Collection<String>): Long? =
                docMap.values
                    .filter { it.shiftId == shiftId && it.docType in docTypes }
                    .minOfOrNull { it.createdAt }
            override suspend fun listByShift(
                kkmId: String,
                shiftId: String,
                limit: Int,
                offset: Int
            ): List<FiscalDocumentEntity> =
                docMap.values.filter { it.cashboxId == kkmId && it.shiftId == shiftId }.sortedByDescending { it.createdAt }.drop(
                    offset
                ).take(limit)
            override suspend fun listByPeriod(
                kkmId: String,
                fromInclusive: Long,
                toExclusive: Long,
                limit: Int,
                offset: Int
            ): List<FiscalDocumentEntity> =
                docMap.values.filter {
                    (kkmId.isEmpty() || it.cashboxId == kkmId) && it.createdAt in fromInclusive..<toExclusive
                }.sortedByDescending { it.createdAt }.drop(
                    offset
                ).take(limit)
            override suspend fun deleteByKkm(kkmId: String) { docMap.entries.removeAll { it.value.cashboxId == kkmId } }
        }

        val counterDao = object : CounterDao {
            override suspend fun insert(entity: CounterEntity) { counterMap[entity.key] = entity }
            override suspend fun getByKey(key: String): CounterEntity? = counterMap[key]
            override suspend fun listByPrefix(prefix: String): List<CounterEntity> = counterMap.values.filter {
                it.key.startsWith(
                    prefix
                )
            }
            override suspend fun deleteByPrefix(prefix: String) {
                counterMap.entries.removeAll {
                    it.key.startsWith(
                        prefix
                    )
                }
            }
        }

        val adapter = DefaultRoomStorageAdapter(
            queueDao = queueDao,
            kkmDao = kkmDao,
            userDao = userDao,
            shiftDao = shiftDao,
            fiscalDocumentDao = fiscalDocumentDao,
            counterDao = counterDao
        )
        return RoomStoragePair(adapter, adapter)
    }
}

/**
 * Пара портов локального хранилища.
 */
data class RoomStoragePair(
    val storagePort: StoragePort,
    val queueStoragePort: QueueStoragePort
)
