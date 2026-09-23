package io.github.texport.superkassa.coredatabase.api

import androidx.room.RoomDatabase
import io.github.texport.superkassa.coredatabase.impl.db.SuperkassaAppDatabase
import io.github.texport.superkassa.coredatabase.impl.restore.SnapshotRows
import io.github.texport.superkassa.coredatabase.impl.restore.SnapshotWriter
import kotlinx.coroutines.runBlocking

/**
 * Кладёт набор записей в пустую базу кассы одной транзакцией.
 *
 * Нужен переносу с узла: касса после обновления приложения должна
 * продолжить ту же смену с теми же номерами и счётчиками. Записи
 * проверяются до записи; ошибка в любой — и в базе не остаётся ничего.
 *
 * @param builder построитель базы; путь к файлу задаёт вызывающий.
 * @param snapshot записи кассы.
 * @throws IllegalArgumentException если запись нельзя положить в схему Room без потери.
 * @throws IllegalStateException если в базе уже есть кассы.
 */
fun restoreRoomStorage(builder: RoomDatabase.Builder<SuperkassaAppDatabase>, snapshot: StorageSnapshot) {
    val rows = SnapshotRows.of(snapshot)
    val database = strictDatabase(builder)
    try {
        runBlocking { SnapshotWriter(database).write(rows) }
    } finally {
        database.close()
    }
}
