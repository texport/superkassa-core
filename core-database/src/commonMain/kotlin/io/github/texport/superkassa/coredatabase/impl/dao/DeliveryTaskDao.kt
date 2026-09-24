package io.github.texport.superkassa.coredatabase.impl.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import io.github.texport.superkassa.coredatabase.impl.entity.DeliveryTaskEntity

/** Задачи доставки чека покупателю. */
@Dao
interface DeliveryTaskDao {

    /** Ставит задачи; уже поставленные не трогает — доставленная не становится снова ожидающей. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entities: List<DeliveryTaskEntity>)

    @Query(
        "SELECT * FROM delivery_tasks WHERE status = 'PENDING' AND nextAttemptAt <= :now ORDER BY nextAttemptAt ASC, id ASC LIMIT :limit"
    )
    suspend fun due(now: Long, limit: Int): List<DeliveryTaskEntity>

    /** Занимает ожидающую задачу со сроком не позже [now] одним изменением: второй отправитель её не займёт. */
    @Query(
        "UPDATE delivery_tasks SET attempts = attempts + 1, nextAttemptAt = :leaseUntil, updatedAt = :now " +
            "WHERE id = :id AND status = 'PENDING' AND nextAttemptAt <= :now"
    )
    suspend fun claim(id: String, now: Long, leaseUntil: Long): Int

    @Update
    suspend fun update(entity: DeliveryTaskEntity)

    @Query("SELECT * FROM delivery_tasks WHERE documentId = :documentId ORDER BY createdAt ASC, id ASC")
    suspend fun byDocument(documentId: String): List<DeliveryTaskEntity>

    @Query("DELETE FROM delivery_tasks WHERE kkmId = :kkmId")
    suspend fun deleteByKkm(kkmId: String)
}
