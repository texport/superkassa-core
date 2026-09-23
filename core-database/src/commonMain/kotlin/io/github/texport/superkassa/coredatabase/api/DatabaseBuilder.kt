@file:kotlin.jvm.JvmName("DatabaseBuilderCommon")

package io.github.texport.superkassa.coredatabase.api

import androidx.room.RoomDatabase
import io.github.texport.superkassa.coredatabase.impl.db.SuperkassaAppDatabase

/**
 * Возвращает платформа-зависимый билдер базы данных Room KMP.
 *
 * База всегда в файле: путь не выбирает между диском и памятью.
 *
 * @param dbPath Имя или абсолютный путь к файлу базы данных SQLite.
 * @return Билдер [RoomDatabase.Builder] для текущей целевой платформы.
 */
expect fun getDatabaseBuilder(dbPath: String = "superkassa.db"): RoomDatabase.Builder<SuperkassaAppDatabase>
