package io.github.texport.superkassa.coredatabase.impl.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Добавляет документу причину отказа словами ОФД.
 *
 * Колонка пустая: у документов, записанных до этого шага, текста отказа
 * нет, и карточка показывает по ним один код, как раньше.
 *
 * Шаг объявлен явно, а не отдан разрушающему откату: тот сносит таблицы,
 * а вместе с ними смены и чеки.
 */
internal val MIGRATION_DOCUMENT_REFUSAL_TEXT = object : Migration(8, 9) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE fiscal_documents ADD COLUMN ofdErrorText TEXT")
    }
}
