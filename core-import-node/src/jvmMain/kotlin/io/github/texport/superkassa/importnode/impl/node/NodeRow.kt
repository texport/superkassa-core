package io.github.texport.superkassa.importnode.impl.node

import androidx.sqlite.SQLiteStatement

/**
 * Строка таблицы узла с доступом к колонкам по имени.
 *
 * Колонки читаются по именам схемы узла, а не по порядку: `ALTER TABLE`
 * узла дописывал колонки в конец, и порядок у старых и новых установок разный.
 */
internal class NodeRow(private val statement: SQLiteStatement, private val columns: Map<String, Int>) {

    fun text(name: String): String = checkNotNull(textOrNull(name)) { "Column $name is empty" }

    fun textOrNull(name: String): String? = index(name).takeUnless(statement::isNull)?.let(statement::getText)

    fun long(name: String): Long = checkNotNull(longOrNull(name)) { "Column $name is empty" }

    fun longOrNull(name: String): Long? = index(name).takeUnless(statement::isNull)?.let(statement::getLong)

    fun intOrNull(name: String): Int? = longOrNull(name)?.let(Math::toIntExact)

    fun bool(name: String): Boolean = long(name) != 0L

    fun blobOrNull(name: String): ByteArray? = index(name).takeUnless(statement::isNull)?.let(statement::getBlob)

    private fun index(name: String): Int = checkNotNull(columns[name]) { "Node table has no column $name" }

    companion object {
        /** Имена колонок выборки: одни на все её строки. */
        fun columnsOf(statement: SQLiteStatement): Map<String, Int> =
            (0 until statement.getColumnCount()).associateBy(statement::getColumnName)
    }
}
