package io.github.texport.superkassa.coredatabase.impl.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import io.github.texport.superkassa.coredatabase.impl.dao.CounterDao
import io.github.texport.superkassa.coredatabase.impl.dao.DeliveryTaskDao
import io.github.texport.superkassa.coredatabase.impl.dao.FiscalDocumentDao
import io.github.texport.superkassa.coredatabase.impl.dao.IdempotencyDao
import io.github.texport.superkassa.coredatabase.impl.dao.KkmDao
import io.github.texport.superkassa.coredatabase.impl.dao.KkmUserDao
import io.github.texport.superkassa.coredatabase.impl.dao.PinAttemptDao
import io.github.texport.superkassa.coredatabase.impl.dao.QueueCommandDao
import io.github.texport.superkassa.coredatabase.impl.dao.ShiftDao
import io.github.texport.superkassa.coredatabase.impl.entity.CounterEntity
import io.github.texport.superkassa.coredatabase.impl.entity.DeliveryTaskEntity
import io.github.texport.superkassa.coredatabase.impl.entity.FiscalDocumentEntity
import io.github.texport.superkassa.coredatabase.impl.entity.IdempotencyEntity
import io.github.texport.superkassa.coredatabase.impl.entity.KkmEntity
import io.github.texport.superkassa.coredatabase.impl.entity.KkmUserEntity
import io.github.texport.superkassa.coredatabase.impl.entity.PinAttemptEntity
import io.github.texport.superkassa.coredatabase.impl.entity.QueueCommandEntity
import io.github.texport.superkassa.coredatabase.impl.entity.ShiftEntity

/**
 * База данных Room KMP для локального хранения конфигурации ККМ, смен, чеков, счетчиков и офлайн-очереди.
 */
@Database(
    entities = [
        QueueCommandEntity::class,
        KkmEntity::class,
        KkmUserEntity::class,
        ShiftEntity::class,
        FiscalDocumentEntity::class,
        CounterEntity::class,
        IdempotencyEntity::class,
        PinAttemptEntity::class,
        DeliveryTaskEntity::class
    ],
    version = 15,
    exportSchema = false
)
@ConstructedBy(SuperkassaAppDatabaseConstructor::class)
abstract class SuperkassaAppDatabase : RoomDatabase() {

    abstract fun queueCommandDao(): QueueCommandDao
    abstract fun kkmDao(): KkmDao
    abstract fun kkmUserDao(): KkmUserDao
    abstract fun shiftDao(): ShiftDao
    abstract fun fiscalDocumentDao(): FiscalDocumentDao
    abstract fun counterDao(): CounterDao
    abstract fun idempotencyDao(): IdempotencyDao
    abstract fun pinAttemptDao(): PinAttemptDao
    abstract fun deliveryTaskDao(): DeliveryTaskDao
}

/**
 * Конструктор RoomDatabaseConstructor для мультиплатформенной генерации Room KMP.
 */
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object SuperkassaAppDatabaseConstructor : RoomDatabaseConstructor<SuperkassaAppDatabase> {
    override fun initialize(): SuperkassaAppDatabase
}
