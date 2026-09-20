package io.github.texport.superkassa.coredatabase.impl.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Добавляет кассе название, данное владельцем.
 *
 * Колонка пустая: у касс, заведённых до этого шага, названия нет, и
 * показывается регистрационный номер, как раньше.
 *
 * Шаг объявлен явно, а не отдан разрушающему откату: тот сносит таблицы,
 * а вместе с ними смены и чеки.
 */
internal val MIGRATION_KKM_NAME = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE kkms ADD COLUMN name TEXT")
    }
}
