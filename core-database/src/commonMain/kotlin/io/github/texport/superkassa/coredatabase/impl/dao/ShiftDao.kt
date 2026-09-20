package io.github.texport.superkassa.coredatabase.impl.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.texport.superkassa.coredatabase.impl.entity.ShiftEntity

/**
 * DAO интерфейс Room для смен ККМ.
 */
@Dao
interface ShiftDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ShiftEntity)

    @Query("SELECT * FROM shifts WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ShiftEntity?

    @Query("SELECT * FROM shifts WHERE kkmId = :kkmId AND status = 'OPEN' ORDER BY openedAt DESC LIMIT 1")
    suspend fun findOpenShift(kkmId: String): ShiftEntity?

    @Query("SELECT * FROM shifts WHERE kkmId = :kkmId ORDER BY shiftNo DESC LIMIT :limit OFFSET :offset")
    suspend fun listByKkm(kkmId: String, limit: Int, offset: Int): List<ShiftEntity>

    @Query("DELETE FROM shifts WHERE kkmId = :kkmId")
    suspend fun deleteByKkm(kkmId: String)
}
