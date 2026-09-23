@file:kotlin.jvm.JvmName("DatabaseBuilderJvm")

package io.github.texport.superkassa.coredatabase.api

import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.texport.superkassa.coredatabase.impl.db.SuperkassaAppDatabase
import java.io.File

actual fun getDatabaseBuilder(dbPath: String): RoomDatabase.Builder<SuperkassaAppDatabase> {
    val dbFile = File(dbPath)
    dbFile.parentFile?.mkdirs()
    return Room.databaseBuilder<SuperkassaAppDatabase>(name = dbFile.absolutePath)
}

/**
 * Открывает базу кассы в памяти — для тестов.
 *
 * Только явным вызовом: прежде базу в памяти давал путь со словом `test`
 * или `inmemory`, и касса владельца с каталогом вроде `/Users/tester`
 * теряла смены и чеки при остановке.
 */
fun openInMemoryRoomStorage(): RoomStorage = openRoomStorage(Room.inMemoryDatabaseBuilder<SuperkassaAppDatabase>())
