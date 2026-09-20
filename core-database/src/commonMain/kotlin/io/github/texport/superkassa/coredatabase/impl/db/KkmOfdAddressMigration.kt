package io.github.texport.superkassa.coredatabase.impl.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Добавляет кассе колонки адреса ОФД, заданного вручную.
 *
 * Шаг 5 → 6 хранится ради баз, застрявших на пятой версии: без него цепочка
 * миграций рвётся и Room уходит в разрушающий откат. Сами колонки следующим
 * шагом удаляются — провайдера со своим адресом у кассы больше нет.
 */
internal val MIGRATION_KKM_OFD_ADDRESS = object : Migration(5, 6) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE kkms ADD COLUMN ofdHost TEXT")
        connection.execSQL("ALTER TABLE kkms ADD COLUMN ofdPort INTEGER")
    }
}

/**
 * Убирает у кассы колонки адреса ОФД.
 *
 * Касса работает только со справочными ОФД: адрес задаёт узел по контуру,
 * и у всех записей эти колонки пустые. Хранить их дальше нечем оправдать,
 * а расхождение сущности со схемой Room не прощает.
 *
 * Миграция объявлена явно, чтобы обновление не уронило базу в разрушающий
 * откат и не унесло с собой смены и чеки.
 */
internal val MIGRATION_DROP_KKM_OFD_ADDRESS = object : Migration(6, 7) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE kkms DROP COLUMN ofdHost")
        connection.execSQL("ALTER TABLE kkms DROP COLUMN ofdPort")
    }
}
