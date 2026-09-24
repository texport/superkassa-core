package io.github.texport.superkassa.coredatabase.impl.dao

import io.github.texport.superkassa.coredatabase.impl.entity.CounterEntity
import io.github.texport.superkassa.coredatabase.impl.entity.FiscalDocumentEntity
import io.github.texport.superkassa.coredatabase.impl.entity.KkmEntity
import io.github.texport.superkassa.coredatabase.impl.entity.KkmUserEntity
import io.github.texport.superkassa.coredatabase.impl.entity.PinAttemptEntity
import io.github.texport.superkassa.coredatabase.impl.entity.QueueCommandEntity
import io.github.texport.superkassa.coredatabase.impl.entity.ShiftEntity

// Таблицы в памяти — для хранилища без диска: отвечают так же, как запросы DAO Room.

internal class InMemoryQueueCommandDao : QueueCommandDao {
    private val rows = mutableMapOf<String, QueueCommandEntity>()

    override suspend fun insert(entity: QueueCommandEntity): Long {
        if (entity.id in rows) return -1L
        rows[entity.id] = entity
        return rows.size.toLong()
    }

    override suspend fun getByStatus(cashboxId: String, lane: String, statuses: List<String>): List<QueueCommandEntity> =
        rows.values.filter { it.cashboxId == cashboxId && it.lane == lane && it.status in statuses }

    override suspend fun listByCashbox(cashboxId: String, lane: String, limit: Int, offset: Int): List<QueueCommandEntity> =
        rows.values.filter { it.cashboxId == cashboxId && it.lane == lane }.drop(offset).take(limit)

    override suspend fun updateStatus(
        id: String,
        status: String,
        attempt: Int,
        lastError: String?,
        lastErrorCode: Int?,
        nextAttemptAt: Long?
    ): Int {
        val current = rows[id] ?: return 0
        rows[id] = current.copy(
            status = status,
            attempt = attempt,
            lastError = lastError,
            lastErrorCode = lastErrorCode,
            nextAttemptAt = nextAttemptAt
        )
        return 1
    }

    override suspend fun markInProgress(id: String, now: Long): Int {
        val current = rows[id] ?: return 0
        rows[id] = current.copy(status = "IN_PROGRESS", nextAttemptAt = now)
        return 1
    }

    override suspend fun deleteByCashbox(cashboxId: String) {
        rows.entries.removeAll { it.value.cashboxId == cashboxId }
    }
}

internal class InMemoryKkmDao : KkmDao {
    private val rows = mutableMapOf<String, KkmEntity>()

    override suspend fun insert(entity: KkmEntity) {
        rows[entity.id] = entity
    }

    override suspend fun getById(id: String): KkmEntity? = rows[id]

    override suspend fun getByRegistrationNumber(registrationNumber: String): KkmEntity? =
        rows.values.firstOrNull { it.registrationNumber == registrationNumber }

    override suspend fun getBySystemId(systemId: String): KkmEntity? = rows.values.firstOrNull { it.systemId == systemId }

    override suspend fun list(limit: Int, offset: Int): List<KkmEntity> = rows.values.drop(offset).take(limit)

    override suspend fun deleteById(id: String) {
        rows.remove(id)
    }
}

internal class InMemoryKkmUserDao : KkmUserDao {
    private val rows = mutableMapOf<String, KkmUserEntity>()

    override suspend fun insert(entity: KkmUserEntity) {
        rows[entity.id] = entity
    }

    override suspend fun getById(id: String): KkmUserEntity? = rows[id]

    override suspend fun listByKkm(kkmId: String): List<KkmUserEntity> = rows.values.filter { it.kkmId == kkmId }

    override suspend fun deleteById(id: String) {
        rows.remove(id)
    }

    override suspend fun deleteByKkm(kkmId: String) {
        rows.entries.removeAll { it.value.kkmId == kkmId }
    }
}

internal class InMemoryShiftDao : ShiftDao {
    private val rows = mutableMapOf<String, ShiftEntity>()

    override suspend fun insert(entity: ShiftEntity) {
        rows[entity.id] = entity
    }

    override suspend fun getById(id: String): ShiftEntity? = rows[id]

    override suspend fun findOpenShift(kkmId: String): ShiftEntity? =
        rows.values.firstOrNull { it.kkmId == kkmId && it.status == "OPEN" }

    override suspend fun listByKkm(kkmId: String, limit: Int, offset: Int): List<ShiftEntity> =
        rows.values.filter { it.kkmId == kkmId }.drop(offset).take(limit)

    override suspend fun deleteByKkm(kkmId: String) {
        rows.entries.removeAll { it.value.kkmId == kkmId }
    }
}

internal class InMemoryFiscalDocumentDao : FiscalDocumentDao {
    private val rows = mutableMapOf<String, FiscalDocumentEntity>()

    override suspend fun insert(entity: FiscalDocumentEntity) {
        rows[entity.id] = entity
    }

    override suspend fun getById(id: String): FiscalDocumentEntity? = rows[id]

    override suspend fun firstPaymentTime(shiftId: String, docTypes: Collection<String>): Long? =
        rows.values.filter { it.shiftId == shiftId && it.docType in docTypes }.minOfOrNull { it.createdAt }

    override suspend fun listByShift(kkmId: String, shiftId: String, limit: Int, offset: Int): List<FiscalDocumentEntity> =
        newestFirst { it.cashboxId == kkmId && it.shiftId == shiftId }.drop(offset).take(limit)

    override suspend fun listByPeriod(
        kkmId: String,
        fromInclusive: Long,
        toExclusive: Long,
        limit: Int,
        offset: Int
    ): List<FiscalDocumentEntity> =
        newestFirst { (kkmId.isEmpty() || it.cashboxId == kkmId) && it.createdAt in fromInclusive..<toExclusive }
            .drop(offset)
            .take(limit)

    override suspend fun deleteByKkm(kkmId: String) {
        rows.entries.removeAll { it.value.cashboxId == kkmId }
    }

    private fun newestFirst(filter: (FiscalDocumentEntity) -> Boolean): List<FiscalDocumentEntity> =
        rows.values.filter(filter).sortedByDescending { it.createdAt }
}

internal class InMemoryCounterDao : CounterDao {
    private val rows = mutableMapOf<String, CounterEntity>()

    override suspend fun insert(entity: CounterEntity) {
        rows[entity.key] = entity
    }

    override suspend fun getByKey(key: String): CounterEntity? = rows[key]

    override suspend fun listByPrefix(prefix: String): List<CounterEntity> = rows.values.filter {
        it.key.startsWith(prefix)
    }

    override suspend fun deleteByPrefix(prefix: String) {
        rows.entries.removeAll { it.key.startsWith(prefix) }
    }
}

/** Счёт неверных пинов в памяти — для хранилища без файла. */
internal class InMemoryPinAttemptDao : PinAttemptDao {
    private val rows = mutableMapOf<String, PinAttemptEntity>()

    override suspend fun find(kkmId: String): PinAttemptEntity? = rows[kkmId]

    override suspend fun save(entity: PinAttemptEntity) {
        rows[entity.kkmId] = entity
    }

    override suspend fun delete(kkmId: String) {
        rows.remove(kkmId)
    }
}
