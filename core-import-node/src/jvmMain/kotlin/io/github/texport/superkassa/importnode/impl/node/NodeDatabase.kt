package io.github.texport.superkassa.importnode.impl.node

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.driver.bundled.SQLITE_OPEN_READWRITE
import androidx.sqlite.execSQL
import io.github.texport.superkassa.importnode.api.NodeImportException
import java.io.File

/**
 * Копия базы узла, открытая для чтения.
 *
 * Читается тем же движком SQLite, что и база кассы, без драйвера JDBC.
 * Открывается копия (см. [NodeDatabaseCopy]), поэтому журнал, оставленный
 * узлом, SQLite доводит в ней, а не в рабочем месте узла. Всё чтение идёт
 * одной транзакцией, копия перед чтением проверяется на целостность.
 */
internal class NodeDatabase private constructor(private val connection: SQLiteConnection) : AutoCloseable {

    fun <T> rows(sql: String, map: (NodeRow) -> T): List<T> = connection.prepare(sql).use { statement ->
        val columns = NodeRow.columnsOf(statement)
        buildList { while (statement.step()) add(map(NodeRow(statement, columns))) }
    }

    fun count(table: String): Long = rows("SELECT COUNT(*) AS n FROM $table") { it.long("n") }.single()

    override fun close() {
        try {
            connection.execSQL("ROLLBACK")
        } finally {
            connection.close()
        }
    }

    companion object {

        /**
         * Схема, которую перенос понимает: шаги узла 1–25, двадцатый —
         * пересчёт итогов чеков в тиыны. Без него итоги в тенге, с лишними
         * шагами — колонки, о которых перенос не знает и которые потерял бы.
         */
        private val KNOWN_SCHEMA = (1..25).map(Int::toString).toSet()

        fun open(file: File): NodeDatabase {
            val connection = BundledSQLiteDriver().open(file.path, SQLITE_OPEN_READWRITE)
            var opened: NodeDatabase? = null
            try {
                connection.execSQL("BEGIN")
                opened = NodeDatabase(connection).also(::requireIntact).also(::requireKnownSchema)
                return opened
            } finally {
                // Закрытие соединения откатывает и начатую транзакцию чтения.
                if (opened == null) connection.close()
            }
        }

        private fun requireIntact(database: NodeDatabase) {
            val verdict = database.rows("PRAGMA quick_check") { it.text("quick_check") }
            if (verdict != listOf("ok")) throw NodeImportException("Node database is damaged: ${verdict.take(3)}")
        }

        private fun requireKnownSchema(database: NodeDatabase) {
            val applied = database.rows("SELECT version FROM schema_migrations") { it.text("version") }.toSet()
            if (applied != KNOWN_SCHEMA) {
                throw NodeImportException(
                    "Node database schema differs from the one the import knows: " +
                        "missing ${(KNOWN_SCHEMA - applied).sorted()}, unknown ${(applied - KNOWN_SCHEMA).sorted()}. " +
                        "Start the node of the matching version once, or update the import."
                )
            }
        }
    }
}
