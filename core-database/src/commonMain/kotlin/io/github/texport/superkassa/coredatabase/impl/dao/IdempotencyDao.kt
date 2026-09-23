package io.github.texport.superkassa.coredatabase.impl.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.texport.superkassa.coredatabase.impl.entity.IdempotencyEntity

/**
 * DAO ключей повтора фискальных операций.
 */
@Dao
interface IdempotencyDao {

    /** Заводит ключ; уже заведённый не трогает и возвращает -1. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: IdempotencyEntity): Long

    @Query("SELECT * FROM idempotency_keys WHERE kkmId = :kkmId AND idempotencyKey = :key LIMIT 1")
    suspend fun find(kkmId: String, key: String): IdempotencyEntity?

    @Query("UPDATE idempotency_keys SET responseRef = :responseRef WHERE kkmId = :kkmId AND idempotencyKey = :key")
    suspend fun complete(kkmId: String, key: String, responseRef: String?): Int

    @Query("DELETE FROM idempotency_keys WHERE kkmId = :kkmId AND idempotencyKey = :key AND responseRef IS NULL")
    suspend fun forgetUnfinished(kkmId: String, key: String)

    @Query("DELETE FROM idempotency_keys WHERE kkmId = :kkmId")
    suspend fun deleteByKkm(kkmId: String)
}
