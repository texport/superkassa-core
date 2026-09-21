package io.github.texport.superkassa.coredatabase.impl.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.texport.superkassa.coredatabase.impl.entity.FiscalDocumentEntity

/**
 * DAO интерфейс Room для фискальных документов ККМ.
 */
@Dao
interface FiscalDocumentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: FiscalDocumentEntity)

    @Query("SELECT * FROM fiscal_documents WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): FiscalDocumentEntity?

    @Query(
        "SELECT * FROM fiscal_documents WHERE cashboxId = :kkmId AND shiftId = :shiftId ORDER BY createdAt DESC LIMIT :limit OFFSET :offset"
    )
    suspend fun listByShift(kkmId: String, shiftId: String, limit: Int, offset: Int): List<FiscalDocumentEntity>

    @Query(
        "SELECT * FROM fiscal_documents WHERE cashboxId = :kkmId AND createdAt >= :fromInclusive AND createdAt < :toExclusive ORDER BY createdAt DESC LIMIT :limit OFFSET :offset"
    )
    suspend fun listByPeriod(
        kkmId: String,
        fromInclusive: Long,
        toExclusive: Long,
        limit: Int,
        offset: Int
    ): List<FiscalDocumentEntity>

    /**
     * Время первого платёжного документа смены.
     *
     * Одним запросом, а не перебором документов смены: проверка
     * продолжительности смены идёт на каждую кассовую операцию, а чеков
     * за смену бывают сотни.
     */
    @Query(
        "SELECT MIN(createdAt) FROM fiscal_documents WHERE shiftId = :shiftId AND docType IN (:docTypes)"
    )
    suspend fun firstPaymentTime(shiftId: String, docTypes: Collection<String>): Long?

    @Query("DELETE FROM fiscal_documents WHERE cashboxId = :kkmId")
    suspend fun deleteByKkm(kkmId: String)
}
