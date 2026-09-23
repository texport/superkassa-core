package io.github.texport.superkassa.coredatabase.impl.restore

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import io.github.texport.superkassa.coredatabase.impl.db.SuperkassaAppDatabase

/**
 * Пишет строки набора в базу одной транзакцией.
 *
 * Пишет только в пустую базу: слить перенос с уже работающей кассой
 * значило бы получить две истории одних и тех же номеров.
 */
internal class SnapshotWriter(private val database: SuperkassaAppDatabase) {

    suspend fun write(rows: SnapshotRows) {
        database.useWriterConnection { transactor ->
            transactor.immediateTransaction {
                check(database.kkmDao().list(1, 0).isEmpty()) { "Target database already holds cash registers" }
                writeRegisters(rows)
                writeLedger(rows)
            }
        }
    }

    private suspend fun writeRegisters(rows: SnapshotRows) {
        rows.kkms.forEach { database.kkmDao().insert(it) }
        rows.users.forEach { database.kkmUserDao().insert(it) }
        rows.shifts.forEach { database.shiftDao().insert(it) }
    }

    private suspend fun writeLedger(rows: SnapshotRows) {
        rows.documents.forEach { database.fiscalDocumentDao().insert(it) }
        rows.counters.forEach { database.counterDao().insert(it) }
        rows.queue.forEach { database.queueCommandDao().insert(it) }
        rows.idempotencyKeys.forEach {
            check(database.idempotencyDao().insert(it) != -1L) { "Idempotency key of ${it.kkmId} is duplicated" }
        }
    }
}
