package io.github.texport.superkassa.coredatabase.impl.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Даёт кассе колонку оформления чека.
 *
 * Без неё оформление жило только в памяти: первая же правка настроек
 * кассы записывала её заново с оформлением по умолчанию. Колонка пустая —
 * у заведённых раньше касс оформление по умолчанию, как и было.
 */
internal val MIGRATION_KKM_BRANDING = object : Migration(10, 11) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE kkms ADD COLUMN brandingJson TEXT")
    }
}
