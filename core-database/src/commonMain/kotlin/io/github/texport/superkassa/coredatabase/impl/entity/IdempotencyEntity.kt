package io.github.texport.superkassa.coredatabase.impl.entity

import androidx.room.Entity

/**
 * Ключ повтора фискальной операции.
 *
 * Пока у ключа нет [responseRef], операция по нему ещё идёт или сорвалась;
 * с ним — завершена, и повтор возвращает тот же документ, а не новый.
 */
@Entity(tableName = "idempotency_keys", primaryKeys = ["kkmId", "idempotencyKey"])
data class IdempotencyEntity(
    val kkmId: String,
    val idempotencyKey: String,
    val operation: String,
    val responseRef: String?,
    val createdAt: Long
)
