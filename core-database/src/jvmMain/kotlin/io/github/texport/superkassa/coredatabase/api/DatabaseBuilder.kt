@file:kotlin.jvm.JvmName("DatabaseBuilderJvm")

package io.github.texport.superkassa.coredatabase.api

import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.texport.superkassa.coredatabase.impl.db.SuperkassaAppDatabase
import java.io.File

actual fun getDatabaseBuilder(dbPath: String): RoomDatabase.Builder<SuperkassaAppDatabase> {
    return if (dbPath.contains("inmemory") || dbPath.contains("test")) {
        Room.inMemoryDatabaseBuilder<SuperkassaAppDatabase>()
    } else {
        val dbFile = File(dbPath)
        dbFile.parentFile?.mkdirs()
        Room.databaseBuilder<SuperkassaAppDatabase>(
            name = dbFile.absolutePath
        )
    }
}

actual fun deleteDatabaseFile(dbPath: String) {
    File(dbPath).delete()
    File("$dbPath-wal").delete()
    File("$dbPath-shm").delete()
}
