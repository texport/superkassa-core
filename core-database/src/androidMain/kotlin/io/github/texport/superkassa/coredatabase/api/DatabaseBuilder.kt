@file:kotlin.jvm.JvmName("DatabaseBuilderAndroid")

package io.github.texport.superkassa.coredatabase.api

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.texport.superkassa.coredatabase.impl.db.SuperkassaAppDatabase

actual fun getDatabaseBuilder(dbPath: String): RoomDatabase.Builder<SuperkassaAppDatabase> {
    val appContext = try {
        val activityThreadClass = Class.forName("android.app.ActivityThread")
        val currentApplicationMethod = activityThreadClass.getMethod("currentApplication")
        currentApplicationMethod.invoke(null) as Context
    } catch (_: Exception) {
        error("Android Context is not available.")
    }

    val dbFile = appContext.getDatabasePath(dbPath)
    dbFile.parentFile?.mkdirs()
    return Room.databaseBuilder<SuperkassaAppDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}

actual fun deleteDatabaseFile(dbPath: String) {
    try {
        val activityThreadClass = Class.forName("android.app.ActivityThread")
        val currentApplicationMethod = activityThreadClass.getMethod("currentApplication")
        val appContext = currentApplicationMethod.invoke(null) as Context
        appContext.deleteDatabase(dbPath)
    } catch (_: Exception) {
        val file = java.io.File(dbPath)
        file.delete()
    }
}
