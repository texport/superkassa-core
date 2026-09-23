package io.github.texport.superkassa.coredatabase.impl.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.texport.superkassa.coredatabase.impl.entity.KkmEntity

/**
 * DAO интерфейс Room для доступа к настройкам ККМ.
 */
@Dao
interface KkmDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: KkmEntity)

    @Query("SELECT * FROM kkms WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): KkmEntity?

    @Query("SELECT * FROM kkms WHERE registrationNumber = :registrationNumber LIMIT 1")
    suspend fun getByRegistrationNumber(registrationNumber: String): KkmEntity?

    /** Касса по её номеру в ОФД: по нему узнаётся повторная регистрация той же кассы. */
    @Query("SELECT * FROM kkms WHERE systemId = :systemId LIMIT 1")
    suspend fun getBySystemId(systemId: String): KkmEntity?

    @Query("SELECT * FROM kkms ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    suspend fun list(limit: Int, offset: Int): List<KkmEntity>

    @Query("DELETE FROM kkms WHERE id = :id")
    suspend fun deleteById(id: String)
}
