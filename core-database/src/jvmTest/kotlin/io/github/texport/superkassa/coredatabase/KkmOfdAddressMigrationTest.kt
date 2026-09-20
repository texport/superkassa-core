package io.github.texport.superkassa.coredatabase

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import io.github.texport.superkassa.coredatabase.impl.db.MIGRATION_DROP_KKM_OFD_ADDRESS
import io.github.texport.superkassa.coredatabase.impl.db.MIGRATION_KKM_OFD_ADDRESS
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Миграции 5 → 6 → 7 сначала добавляют кассе колонки адреса ОФД, затем убирают их, не трогая прочие. */
class KkmOfdAddressMigrationTest {

    @Test
    fun migrationAddsNullableAddressColumns() {
        withKkmsTable { connection ->
            MIGRATION_KKM_OFD_ADDRESS.migrate(connection)

            val columns = columnTypes(connection)
            assertEquals("TEXT", columns["ofdHost"])
            assertEquals("INTEGER", columns["ofdPort"])

            val row = connection.prepare("SELECT ofdHost, ofdPort FROM kkms WHERE id = 'kkm-1'")
            assertEquals(true, row.step())
            assertEquals(true, row.isNull(0))
            assertEquals(true, row.isNull(1))
            row.close()
        }
    }

    @Test
    fun migrationDropsAddressColumnsAndKeepsRows() {
        withKkmsTable { connection ->
            MIGRATION_KKM_OFD_ADDRESS.migrate(connection)
            MIGRATION_DROP_KKM_OFD_ADDRESS.migrate(connection)

            val columns = columnTypes(connection)
            assertNull(columns["ofdHost"])
            assertNull(columns["ofdPort"])
            assertEquals("TEXT", columns["ofdProvider"])

            val row = connection.prepare("SELECT ofdProvider FROM kkms WHERE id = 'kkm-1'")
            assertEquals(true, row.step())
            assertEquals("BFD:DEV", row.getText(0))
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
