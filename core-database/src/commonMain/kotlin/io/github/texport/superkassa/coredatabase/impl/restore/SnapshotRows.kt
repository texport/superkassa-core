package io.github.texport.superkassa.coredatabase.impl.restore

import io.github.texport.superkassa.core.domain.api.model.queue.QueueTask
import io.github.texport.superkassa.coredatabase.api.StorageSnapshot
import io.github.texport.superkassa.coredatabase.api.StoredCounter
import io.github.texport.superkassa.coredatabase.api.StoredDocument
import io.github.texport.superkassa.coredatabase.api.StoredIdempotencyKey
import io.github.texport.superkassa.coredatabase.api.StoredUser
import io.github.texport.superkassa.coredatabase.impl.adapter.StoredReceiptJson
import io.github.texport.superkassa.coredatabase.impl.adapter.counterKey
import io.github.texport.superkassa.coredatabase.impl.adapter.toCommand
import io.github.texport.superkassa.coredatabase.impl.entity.CounterEntity
import io.github.texport.superkassa.coredatabase.impl.entity.FiscalDocumentEntity
import io.github.texport.superkassa.coredatabase.impl.entity.IdempotencyEntity
import io.github.texport.superkassa.coredatabase.impl.entity.KkmEntity
import io.github.texport.superkassa.coredatabase.impl.entity.KkmUserEntity
import io.github.texport.superkassa.coredatabase.impl.entity.QueueCommandEntity
import io.github.texport.superkassa.coredatabase.impl.entity.ShiftEntity

/**
 * Записи набора в виде строк таблиц Room.
 *
 * Собираются до открытия транзакции: запись, которую Room потом
 * не прочтёт (незнакомая дорожка очереди, двоеточие в ключе счётчика),
 * отказывает здесь, а не у кассира на первом чтении.
 */
internal class SnapshotRows(
    val kkms: List<KkmEntity>,
    val users: List<KkmUserEntity>,
    val shifts: List<ShiftEntity>,
    val documents: List<FiscalDocumentEntity>,
    val counters: List<CounterEntity>,
    val queue: List<QueueCommandEntity>,
    val idempotencyKeys: List<IdempotencyEntity>
) {
    companion object {
        fun of(snapshot: StorageSnapshot): SnapshotRows = SnapshotRows(
            kkms = snapshot.kkms.map(KkmEntity::fromDomain),
            users = snapshot.users.map(::userRow),
            shifts = snapshot.shifts.map(ShiftEntity::fromDomain),
            documents = snapshot.documents.map(::documentRow),
            counters = counterRows(snapshot.counters),
            queue = snapshot.queue.map(::queueRow),
            idempotencyKeys = snapshot.idempotencyKeys.map(::idempotencyRow)
        )

        private fun userRow(user: StoredUser) =
            KkmUserEntity(user.id, user.kkmId, user.name, user.role.name, user.pinHash, user.createdAt)

        private fun documentRow(document: StoredDocument) =
            FiscalDocumentEntity.fromDomain(document.snapshot, document.receipt?.let(StoredReceiptJson::encodeToString))

        /** Задачу, которую очередь кассы не прочтёт, класть нельзя: она встала бы навсегда. */
        private fun queueRow(task: QueueTask): QueueCommandEntity = try {
            QueueCommandEntity.fromDomain(task.toCommand())
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("Queue task ${task.id} does not fit the queue model", e)
        }

        private fun idempotencyRow(key: StoredIdempotencyKey) =
            IdempotencyEntity(key.kkmId, key.key, key.operation, key.responseRef, key.createdAt)

        /** Два значения под одним ключом Room слил бы в одно — какое, решил бы порядок. */
        private fun counterRows(counters: List<StoredCounter>): List<CounterEntity> {
            val rows = counters.map(::counterRow)
            val repeated = rows.groupingBy { it.key }.eachCount().filterValues { it > 1 }.keys
            require(repeated.isEmpty()) { "Counters repeat under one key: $repeated" }
            return rows
        }

        /**
         * Двоеточие разделяет части ключа счётчика; в самой части оно
         * сделало бы ключ неразборным при чтении списка счётчиков.
         */
        private fun counterRow(counter: StoredCounter): CounterEntity {
            val parts = listOfNotNull(counter.kkmId, counter.scope, counter.shiftId, counter.key)
            require(
                parts.none { SEPARATOR in it }
            ) { "Counter ${counter.key} of ${counter.kkmId} contains '$SEPARATOR'" }
            return CounterEntity(counterKey(counter.kkmId, counter.scope, counter.shiftId, counter.key), counter.value)
        }

        private const val SEPARATOR = ':'
    }
}
