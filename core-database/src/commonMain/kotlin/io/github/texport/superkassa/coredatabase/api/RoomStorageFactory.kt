package io.github.texport.superkassa.coredatabase.api

import io.github.texport.superkassa.core.domain.api.port.integration.PinAttemptsPort
import io.github.texport.superkassa.core.domain.api.port.integration.StoragePort
import io.github.texport.superkassa.core.domain.impl.logging.getLogger
import io.github.texport.superkassa.coredatabase.impl.adapter.DefaultRoomStorageAdapter
import io.github.texport.superkassa.coredatabase.impl.adapter.RoomPinAttempts
import io.github.texport.superkassa.coredatabase.impl.dao.InMemoryCounterDao
import io.github.texport.superkassa.coredatabase.impl.dao.InMemoryFiscalDocumentDao
import io.github.texport.superkassa.coredatabase.impl.dao.InMemoryIdempotencyDao
import io.github.texport.superkassa.coredatabase.impl.dao.InMemoryKkmDao
import io.github.texport.superkassa.coredatabase.impl.dao.InMemoryKkmUserDao
import io.github.texport.superkassa.coredatabase.impl.dao.InMemoryPinAttemptDao
import io.github.texport.superkassa.coredatabase.impl.dao.InMemoryQueueCommandDao
import io.github.texport.superkassa.coredatabase.impl.dao.InMemoryShiftDao
import io.github.texport.superkassa.offlinequeue.api.port.QueueStoragePort

/**
 * Публичная фабрика создания локального хранилища данных на базе Room KMP.
 */
object RoomStorageFactory {

    private val logger = getLogger(RoomStorageFactory::class)

    /**
     * Открывает базу кассы в файле [dbPath] строго, как [openRoomStorage].
     *
     * Прежде ошибка открытия здесь удаляла файл и заводила пустую базу,
     * а вторая ошибка — базу в памяти: касса поднималась без смен и чеков
     * и теряла всё при остановке. Теперь база не открылась — касса
     * не поднимается, файл остаётся как был, причина — в исключении.
     *
     * @param dbPath путь к файлу базы; база в памяти этим путём не заказывается.
     * @throws StorageOpenException если базу нельзя открыть или её схема незнакома.
     */
    fun createRoomStorage(dbPath: String = "superkassa.db"): RoomStoragePair {
        logger.info("Opening cash register database '{}'", dbPath)
        val storage = try {
            openRoomStorage(getDatabaseBuilder(dbPath))
        } catch (e: Exception) {
            throw StorageOpenException(dbPath, e)
        }
        return RoomStoragePair(storage.storagePort, storage.queueStoragePort, storage.pinAttempts)
    }

    /**
     * Создает экземпляр in-memory хранилища для юнит-тестирования без привязки к дисковой SQLite БД.
     *
     * @return Пара интерфейсов [StoragePort] и [QueueStoragePort].
     */
    fun createDefaultStorage(): RoomStoragePair {
        val pinAttempts = RoomPinAttempts(InMemoryPinAttemptDao())
        val adapter = DefaultRoomStorageAdapter(
            queueDao = InMemoryQueueCommandDao(),
            kkmDao = InMemoryKkmDao(),
            userDao = InMemoryKkmUserDao(),
            shiftDao = InMemoryShiftDao(),
            fiscalDocumentDao = InMemoryFiscalDocumentDao(),
            counterDao = InMemoryCounterDao(),
            idempotencyDao = InMemoryIdempotencyDao(),
            pinAttempts = pinAttempts
        )
        return RoomStoragePair(adapter, adapter, pinAttempts)
    }
}

/**
 * База кассы не открылась, и файл базы не тронут.
 *
 * Касса с такой базой не поднимается: пустая касса на её месте выглядела бы
 * как потерянные смены и чеки. Причина — в [cause]: файл не база SQLite,
 * схема новее приложения, нет шага миграции.
 */
class StorageOpenException(dbPath: String, cause: Throwable) :
    IllegalStateException(
        "Cash register database '$dbPath' cannot be opened, the file is left untouched: ${cause.message}",
        cause
    )

/**
 * Порты локального хранилища.
 *
 * @property pinAttempts счёт неверных пинов той же базы; ядру передаётся явно.
 */
data class RoomStoragePair(
    val storagePort: StoragePort,
    val queueStoragePort: QueueStoragePort,
    val pinAttempts: PinAttemptsPort
)
