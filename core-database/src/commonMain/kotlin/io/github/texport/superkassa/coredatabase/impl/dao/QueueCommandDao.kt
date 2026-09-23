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

    /**
     * Ставит команду; уже поставленную не трогает и возвращает -1.
     *
     * Идентификатор команды — касса, тип и документ, поэтому повторная
     * постановка того же документа приходит с тем же идентификатором.
     * Замена сбросила бы отправленную команду в «ждёт» и сдвинула бы её
     * в хвост очереди — документ ушёл бы в ОФД второй раз и не по порядку.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: QueueCommandEntity): Long

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
    ): Int

    /** Захват команды обработчиком: число попыток не трогается, время захвата — в [now]. */
    @Query("UPDATE queue_commands SET status = 'IN_PROGRESS', nextAttemptAt = :now WHERE id = :id")
    suspend fun markInProgress(id: String, now: Long): Int

    @Query("DELETE FROM queue_commands WHERE cashboxId = :cashboxId")
    suspend fun deleteByCashbox(cashboxId: String)
}
