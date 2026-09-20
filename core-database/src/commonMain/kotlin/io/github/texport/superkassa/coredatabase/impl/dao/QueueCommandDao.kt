package io.github.texport.superkassa.coredatabase.impl.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.texport.superkassa.coredatabase.impl.entity.QueueCommandEntity

/**
 * DAO интерфейс Room для доступа к командам офлайн-очереди.
 */
@Dao
interface QueueCommandDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: QueueCommandEntity)

    @Query("SELECT * FROM queue_commands WHERE cashboxId = :cashboxId AND lane = :lane AND status IN (:statuses)")
    suspend fun getByStatus(cashboxId: String, lane: String, statuses: List<String>): List<QueueCommandEntity>

    @Query(
        "SELECT * FROM queue_commands WHERE cashboxId = :cashboxId AND lane = :lane ORDER BY createdAt ASC LIMIT :limit OFFSET :offset"
    )
    suspend fun listByCashbox(cashboxId: String, lane: String, limit: Int, offset: Int): List<QueueCommandEntity>

    @Query(
        "UPDATE queue_commands SET status = :status, attempt = :attempt, lastError = :lastError, nextAttemptAt = :nextAttemptAt WHERE id = :id"
    )
    suspend fun updateStatus(
        id: String,
        status: String,
        attempt: Int,
        lastError: String?,
        nextAttemptAt: Long?
    )

    @Query("DELETE FROM queue_commands WHERE cashboxId = :cashboxId")
    suspend fun deleteByCashbox(cashboxId: String)
}
