package io.github.texport.superkassa.coredatabase.impl.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Убирает колонку `pin` из таблицы пользователей ККМ.
 *
 * До этой версии пин лежал рядом со своим хешем открытым текстом, и любой,
 * кто дотянулся до файла базы, читал учётные данные администратора. Вход
 * проверяется по хешу, так что сама колонка ничему не служила.
 *
 * Миграция объявлена явно, чтобы обновление не уронило базу в разрушающий
 * откат и не унесло с собой смены и чеки.
 */
internal val MIGRATION_DROP_USER_PIN = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE kkm_users DROP COLUMN pin")
    }
}
