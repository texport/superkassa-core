package io.github.texport.superkassa.coredatabase.impl.adapter

import io.github.texport.superkassa.core.domain.api.model.common.CounterSnapshot
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftInfo
import io.github.texport.superkassa.core.domain.api.model.shift.ShiftStatus
import io.github.texport.superkassa.coredatabase.impl.dao.CounterDao
import io.github.texport.superkassa.coredatabase.impl.dao.ShiftDao
import io.github.texport.superkassa.coredatabase.impl.entity.CounterEntity
import io.github.texport.superkassa.coredatabase.impl.entity.ShiftEntity
import kotlinx.coroutines.runBlocking
import kotlin.time.Clock

/** Смены кассы и её счётчики. */
internal class RoomShifts(private val shiftDao: ShiftDao, private val counterDao: CounterDao) {

    fun find(shiftId: String): ShiftInfo? = runBlocking { shiftDao.getById(shiftId)?.toDomain() }

    fun findOpen(kkmId: String): ShiftInfo? = runBlocking { shiftDao.findOpenShift(kkmId)?.toDomain() }

    fun list(kkmId: String, limit: Int, offset: Int): List<ShiftInfo> = runBlocking {
        shiftDao.listByKkm(kkmId, limit, offset).map { it.toDomain() }
    }

    fun create(shift: ShiftInfo): Boolean = runBlocking {
        shiftDao.insert(ShiftEntity.fromDomain(shift))
        true
    }

    fun close(shiftId: String, status: ShiftStatus, closedAt: Long, closeDocumentId: String?): Boolean = runBlocking {
        val current = shiftDao.getById(shiftId) ?: return@runBlocking false
        shiftDao.insert(current.copy(status = status.name, closedAt = closedAt, closeDocumentId = closeDocumentId))
        true
    }

    fun deleteByKkm(kkmId: String) = runBlocking {
        shiftDao.deleteByKkm(kkmId)
        counterDao.deleteByPrefix("$kkmId:")
    }

    fun loadCounters(kkmId: String, scope: String, shiftId: String?): Map<String, Long> = runBlocking {
        val prefix = counterKey(kkmId, scope, shiftId, "")
        counterDao.listByPrefix(prefix).associate { it.key.removePrefix(prefix) to it.value }
    }

    fun listCounters(kkmId: String): List<CounterSnapshot> = runBlocking {
        val now = Clock.System.now().toEpochMilliseconds()
        counterDao.listByPrefix("$kkmId:").map { (key, value) ->
            val parts = key.split(":")
            CounterSnapshot(
                scope = parts.getOrNull(1) ?: "",
                shiftId = parts.getOrNull(2).takeIf { !it.isNullOrEmpty() },
                key = parts.getOrNull(3) ?: "",
                value = value,
                updatedAt = now
            )
        }
    }

    fun upsertCounter(kkmId: String, scope: String, shiftId: String?, key: String, value: Long): Boolean = runBlocking {
        counterDao.insert(CounterEntity(counterKey(kkmId, scope, shiftId, key), value))
        true
    }
}
