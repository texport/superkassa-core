package io.github.texport.superkassa.coredatabase.impl.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.texport.superkassa.coredatabase.impl.entity.KkmUserEntity

/**
 * DAO интерфейс Room для пользователей ККМ.
 */
@Dao
interface KkmUserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: KkmUserEntity)

    @Query("SELECT * FROM kkm_users WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): KkmUserEntity?

    @Query("SELECT * FROM kkm_users WHERE kkmId = :kkmId")
    suspend fun listByKkm(kkmId: String): List<KkmUserEntity>

    @Query("DELETE FROM kkm_users WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM kkm_users WHERE kkmId = :kkmId")
    suspend fun deleteByKkm(kkmId: String)
}
