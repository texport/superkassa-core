@file:kotlin.jvm.JvmName("DatabaseBuilderCommon")

package io.github.texport.superkassa.coredatabase.api

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.texport.superkassa.coredatabase.impl.db.MIGRATION_DOCUMENT_REFUSAL_TEXT
import io.github.texport.superkassa.coredatabase.impl.db.MIGRATION_DROP_KKM_OFD_ADDRESS
import io.github.texport.superkassa.coredatabase.impl.db.MIGRATION_DROP_USER_PIN
import io.github.texport.superkassa.coredatabase.impl.db.MIGRATION_KKM_NAME
import io.github.texport.superkassa.coredatabase.impl.db.MIGRATION_KKM_OFD_ADDRESS
import io.github.texport.superkassa.coredatabase.impl.db.SuperkassaAppDatabase
import kotlinx.coroutines.Dispatchers

/**
 * Вспомогательный конструктор для сборки инстанса [SuperkassaAppDatabase].
 *
 * @param builder Платформа-зависимый билдер Room.
 * @return Инициализированный экземпляр базы данных Room KMP.
 */
fun getRoomDatabase(builder: RoomDatabase.Builder<SuperkassaAppDatabase>): SuperkassaAppDatabase {
    return builder
        .addMigrations(
            MIGRATION_DROP_USER_PIN,
            MIGRATION_KKM_OFD_ADDRESS,
            MIGRATION_DROP_KKM_OFD_ADDRESS,
            MIGRATION_KKM_NAME,
            MIGRATION_DOCUMENT_REFUSAL_TEXT
        )
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
        .build()
}

/**
 * Возвращает платформа-зависимый билдер базы данных Room KMP.
 *
 * @param dbPath Имя или абсолютный путь к файлу базы данных SQLite.
 * @return Билдер [RoomDatabase.Builder] для текущей целевой платформы.
 */
expect fun getDatabaseBuilder(dbPath: String = "superkassa.db"): RoomDatabase.Builder<SuperkassaAppDatabase>

/**
 * Удаляет файл базы данных SQLite при повреждении или сбое миграции.
 *
 * @param dbPath Имя или абсолютный путь к файлу базы данных SQLite.
 */
expect fun deleteDatabaseFile(dbPath: String = "superkassa.db")
