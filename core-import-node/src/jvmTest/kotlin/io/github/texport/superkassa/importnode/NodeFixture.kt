package io.github.texport.superkassa.importnode

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import java.io.File
import java.security.MessageDigest

/**
 * Рабочее место узла для проверок.
 *
 * База собирается миграциями самого узла (`storage-jdbc`, SQLite, шаги 1–25,
 * снятые в ресурсы проверок) тем же порядком, что у `JdbcMigrationRunner`:
 * таблица шагов, скрипты по номерам, отметка шага 20 — пересчёта итогов в тиыны.
 */
internal class NodeFixture(val home: File) {

    val database = File(home, "data/core.db")

    private val connection: SQLiteConnection

    init {
        database.parentFile.mkdirs()
        File(home, "config").mkdirs()
        File(home, "config/core-settings.json").writeText(SETTINGS)
        connection = BundledSQLiteDriver().open(database.path)
        migrate()
    }

    /** Выполняет запрос с параметрами по порядку: строка, число, флаг, байты или `null`. */
    fun exec(sql: String, vararg args: Any?) {
        connection.prepare(sql).use { statement ->
            args.forEachIndexed { index, value ->
                val at = index + 1
                when (value) {
                    null -> statement.bindNull(at)
                    is String -> statement.bindText(at, value)
                    is Long -> statement.bindLong(at, value)
                    is Int -> statement.bindLong(at, value.toLong())
                    is Boolean -> statement.bindLong(at, if (value) 1L else 0L)
                    is ByteArray -> statement.bindBlob(at, value)
                    else -> error("Unsupported value ${value::class}")
                }
            }
            statement.step()
        }
    }

    fun close() = connection.close()

    /** Отпечаток всех файлов рабочего места: имя → SHA-256. */
    fun fingerprint(): Map<String, String> = home.walkTopDown().filter(File::isFile)
        .associate { it.relativeTo(home).path to sha256(it.readBytes()) }

    private fun migrate() {
        connection.execSQL(
            "CREATE TABLE schema_migrations (version TEXT PRIMARY KEY, checksum TEXT NOT NULL, applied_at BIGINT NOT NULL)"
        )
        scripts().forEach { (version, sql) ->
            statements(sql).forEach(connection::execSQL)
            exec("INSERT INTO schema_migrations VALUES (?, ?, ?)", version, "v$version", 1L)
        }
        exec("INSERT INTO schema_migrations VALUES (?, ?, ?)", "20", "v20", 1L)
    }

    private fun scripts(): List<Pair<String, String>> = (1..25).filter { it != 20 }.map { version ->
        val name = MIGRATIONS.single { it.startsWith("V${version}__") }
        version.toString() to checkNotNull(javaClass.getResource("/node-migrations/$name")).readText()
    }

    private fun statements(sql: String): List<String> = sql.lines()
        .filterNot { it.trimStart().startsWith("--") }
        .joinToString("\n")
        .split(';')
        .map(String::trim)
        .filter(String::isNotEmpty)

    companion object {
        private val MIGRATIONS = checkNotNull(NodeFixture::class.java.getResource("/node-migrations")).let {
            File(it.toURI()).list().orEmpty().toList()
        }

        /** Настройки узла на рабочей машине: SQLite в каталоге узла. */
        private const val SETTINGS = """{"mode":"DESKTOP","storage":{"engine":"SQLITE","jdbcUrl":"jdbc:sqlite:data/core.db?busy_timeout=30000"},""" +
            """"allowChanges":true,"ofdProtocolVersion":"204"}"""

        /** Хеш пина так, как его считает узел (`ServerPinHasherAdapter`). */
        fun nodePinHash(pin: String): String = sha256(pin.toByteArray(Charsets.UTF_8))

        private fun sha256(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
