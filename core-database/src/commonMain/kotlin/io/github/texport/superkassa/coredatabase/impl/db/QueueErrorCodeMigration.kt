package io.github.texport.superkassa.coredatabase.impl.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Задача очереди хранит код отказа БФД рядом с текстом причины.
 *
 * Прежде код жил только в журнале: задача несла одну трёхъязычную
 * строку, и ни журнал кассира, ни решение о повторе опереться на код
 * не могли. У задач, записанных до этого шага, кода нет.
 */
internal val MIGRATION_QUEUE_ERROR_CODE = object : Migration(14, 15) {
    override fun migrate(connection: SQLiteConnection) {
        if (!connection.hasQueueErrorCode()) {
            connection.execSQL("ALTER TABLE queue_commands ADD COLUMN lastErrorCode INTEGER")
        }
    }
}

/**
 * Есть ли колонка кода уже: базу, которую подняли до версии 15 и снова
 * пометили прежней версией, шаг проходит повторно — повторное добавление
 * колонки SQLite отвергает.
 */
private fun SQLiteConnection.hasQueueErrorCode(): Boolean =
    prepare("SELECT COUNT(*) FROM pragma_table_info('queue_commands') WHERE name = 'lastErrorCode'").use {
        it.step() && it.getLong(0) > 0
    }
