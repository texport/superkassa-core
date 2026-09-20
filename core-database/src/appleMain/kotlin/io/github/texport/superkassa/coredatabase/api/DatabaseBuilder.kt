package io.github.texport.superkassa.coredatabase.api

import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.texport.superkassa.coredatabase.impl.db.SuperkassaAppDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual fun getDatabaseBuilder(dbPath: String): RoomDatabase.Builder<SuperkassaAppDatabase> {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null
    )
    val fullPath = if (dbPath.startsWith("/")) {
        dbPath
    } else {
        requireNotNull(documentDirectory?.path) + "/" + dbPath
    }

    println("[RoomStorageFactory] iOS SQLite DB Full Path: $fullPath")

    return Room.databaseBuilder<SuperkassaAppDatabase>(
        name = fullPath
    )
}

@OptIn(ExperimentalForeignApi::class)
actual fun deleteDatabaseFile(dbPath: String) {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null
    )
    val fullPath = if (dbPath.startsWith("/")) {
        dbPath
    } else {
        requireNotNull(documentDirectory?.path) + "/" + dbPath
    }
    println("[RoomStorageFactory] Deleting iOS SQLite DB at path: $fullPath")
    NSFileManager.defaultManager.removeItemAtPath(fullPath, error = null)
    NSFileManager.defaultManager.removeItemAtPath("$fullPath-wal", error = null)
    NSFileManager.defaultManager.removeItemAtPath("$fullPath-shm", error = null)
}
