package io.github.texport.superkassa.coredatabase.impl.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Называет колонки налогоплательщика кассы по-казахстански.
 *
 * БИН/ИИН организации лежал в колонке `orgInn`, код ОКЭД — в `orgOkved`:
 * это обозначения чужой налоговой системы. Колонки переименовываются,
 * данные в них остаются как были.
 */
internal val MIGRATION_KKM_TAXPAYER_COLUMNS = object : Migration(12, 13) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE kkms RENAME COLUMN orgInn TO orgIinOrBin")
        connection.execSQL("ALTER TABLE kkms RENAME COLUMN orgOkved TO orgOked")
    }
}
