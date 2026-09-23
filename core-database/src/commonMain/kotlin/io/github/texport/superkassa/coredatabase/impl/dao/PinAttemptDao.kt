package io.github.texport.superkassa.coredatabase.impl.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.texport.superkassa.coredatabase.impl.entity.PinAttemptEntity

/** Счёт неверных пинов по кассам. */
@Dao
interface PinAttemptDao {

    @Query("SELECT * FROM pin_attempts WHERE kkmId = :kkmId LIMIT 1")
    suspend fun find(kkmId: String): PinAttemptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: PinAttemptEntity)

    @Query("DELETE FROM pin_attempts WHERE kkmId = :kkmId")
    suspend fun delete(kkmId: String)
}
