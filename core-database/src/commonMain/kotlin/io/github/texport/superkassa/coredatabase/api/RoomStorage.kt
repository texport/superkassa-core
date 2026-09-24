package io.github.texport.superkassa.coredatabase.api

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.texport.superkassa.core.domain.api.port.integration.PinAttemptsPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.coredatabase.impl.adapter.DefaultRoomStorageAdapter
import io.github.texport.superkassa.coredatabase.impl.adapter.RoomDeliveryTasks
import io.github.texport.superkassa.coredatabase.impl.adapter.RoomPinAttempts
import io.github.texport.superkassa.coredatabase.impl.db.ALL_MIGRATIONS
import io.github.texport.superkassa.coredatabase.impl.db.SuperkassaAppDatabase
import io.github.texport.superkassa.offlinequeue.api.port.QueueStoragePort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Открытая база кассы на Room и её порты.
 *
 * Закрывается владельцем: касса в процессе приложения живёт меньше
 * процесса, и её файл должен освобождаться вместе с ней.
 */
class RoomStorage internal constructor(
    private val database: SuperkassaAppDatabase,
    adapter: DefaultRoomStorageAdapter,
    /**
     * Счёт неверных пинов в той же базе: блокировка переживает перезапуск.
     * Передаётся ядру явно — хранилище его не несёт.
     */
    val pinAttempts: PinAttemptsPort
) {
    /** Хранилище ядра. */
    val storagePort: StoragePort = adapter

    /** Хранилище офлайн-очереди — та же база. */
    val queueStoragePort: QueueStoragePort = adapter

    /** Закрывает базу. */
    fun close() = database.close()
}

/**
 * Открывает базу кассы строго.
 *
 * Здесь нет ни сноса таблиц при незнакомой схеме, ни удаления файла после
 * ошибки, ни тихой подмены базой в памяти: в базе лежат смены и фискальные
 * документы, и любая из этих «починок» теряла их незаметно для кассира.
 * Не открылась база — касса не поднимается и называет причину.
 *
 * @param builder построитель базы для платформы: путь к файлу задаёт вызывающий.
 * @throws IllegalStateException если схема базы не совпадает и шага миграции нет.
 */
fun openRoomStorage(builder: RoomDatabase.Builder<SuperkassaAppDatabase>): RoomStorage {
    val database = strictDatabase(builder)
    try {
        // Room открывает файл при первом запросе, и там же сверяет схему.
        // Спрашиваем сразу: сломанная база должна отказать здесь, а не на чеке.
        runBlocking { database.kkmDao().list(1, 0) }
    } catch (e: Exception) {
        database.close()
        throw e
    }
    val pinAttempts = RoomPinAttempts(database.pinAttemptDao())
    val adapter = DefaultRoomStorageAdapter(
        queueDao = database.queueCommandDao(),
        kkmDao = database.kkmDao(),
        userDao = database.kkmUserDao(),
        shiftDao = database.shiftDao(),
        fiscalDocumentDao = database.fiscalDocumentDao(),
        counterDao = database.counterDao(),
        idempotencyDao = database.idempotencyDao(),
        pinAttempts = pinAttempts,
        deliveryTasks = RoomDeliveryTasks(database.deliveryTaskDao())
    )
    return RoomStorage(database, adapter, pinAttempts)
}

/** База без сноса и без подмены: общий порядок сборки строгого открытия и переноса. */
internal fun strictDatabase(builder: RoomDatabase.Builder<SuperkassaAppDatabase>): SuperkassaAppDatabase =
    builder
        .addMigrations(*ALL_MIGRATIONS)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()
