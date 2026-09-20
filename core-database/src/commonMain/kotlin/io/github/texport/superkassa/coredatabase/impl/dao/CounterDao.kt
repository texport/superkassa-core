package io.github.texport.superkassa.coredatabase.impl.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.texport.superkassa.coredatabase.impl.entity.CounterEntity

/**
 * DAO интерфейс Room для фискальных счетчиков.
 */
@Dao
interface CounterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CounterEntity)

    @Query("SELECT * FROM counters WHERE `key` = :key LIMIT 1")
    suspend fun getByKey(key: String): CounterEntity?

    @Query("SELECT * FROM counters WHERE `key` LIKE :prefix || '%'")
    suspend fun listByPrefix(prefix: String): List<CounterEntity>

    @Query("DELETE FROM counters WHERE `key` LIKE :prefix || '%'")
    suspend fun deleteByPrefix(prefix: String)
}
