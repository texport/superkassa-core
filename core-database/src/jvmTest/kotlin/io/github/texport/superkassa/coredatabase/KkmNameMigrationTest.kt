package io.github.texport.superkassa.coredatabase

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import io.github.texport.superkassa.coredatabase.impl.db.MIGRATION_KKM_NAME
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Миграция 7 → 8 добавляет кассе название и не трогает уже записанное:
 * разрушающий откат унёс бы вместе со схемой смены и чеки.
 */
class KkmNameMigrationTest {

    @Test
    fun migrationAddsNullableNameColumn() {
        withKkmsTable { connection ->
            MIGRATION_KKM_NAME.migrate(connection)

            assertEquals("TEXT", columnTypes(connection)["name"])

            val row = connection.prepare("SELECT name FROM kkms WHERE id = 'kkm-1'")
            assertEquals(true, row.step())
            assertEquals(true, row.isNull(0))
            row.close()
        }
    }

    @Test
    fun migrationKeepsExistingRows() {
        withKkmsTable { connection ->
            MIGRATION_KKM_NAME.migrate(connection)

            val row = connection.prepare("SELECT ofdProvider FROM kkms WHERE id = 'kkm-1'")
            assertEquals(true, row.step())
            assertEquals("BFD:DEV", row.getText(0))
            row.close()
        }
    }

    @Test
    fun nameSurvivesWriteAndRead() {
        withKkmsTable { connection ->
            MIGRATION_KKM_NAME.migrate(connection)
            connection.execSQL("UPDATE kkms SET name = 'Касса 2 на Достык' WHERE id = 'kkm-1'")

            val row = connection.prepare("SELECT name FROM kkms WHERE id = 'kkm-1'")
            assertEquals(true, row.step())
            assertEquals("Касса 2 на Достык", row.getText(0))
            row.close()
        }
    }

    private fun withKkmsTable(block: (SQLiteConnection) -> Unit) {
        val connection = BundledSQLiteDriver().open(":memory:")
        try {
            connection.execSQL("CREATE TABLE kkms (id TEXT NOT NULL PRIMARY KEY, ofdProvider TEXT)")
            connection.execSQL("INSERT INTO kkms (id, ofdProvider) VALUES ('kkm-1', 'BFD:DEV')")
            block(connection)
        } finally {
            connection.close()
        }
    }

    private fun columnTypes(connection: SQLiteConnection): Map<String, String> {
        val columns = mutableMapOf<String, String>()
        val info = connection.prepare("PRAGMA table_info(kkms)")
        while (info.step()) columns[info.getText(1)] = info.getText(2)
        info.close()
        return columns
    }
}
