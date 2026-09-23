package io.github.texport.superkassa.importnode.impl.node

import io.github.texport.superkassa.importnode.api.NodeImportException
import java.io.File

/**
 * Копия базы узла, с которой идёт чтение.
 *
 * База узла открывается не на месте: SQLite в режиме WAL даже при открытии
 * только на чтение заводит рядом с базой служебные файлы, а рабочее место
 * узла перенос не трогает. Копия снимается вместе с журналами (`-wal`,
 * `-journal`): в них могут лежать записанные, но ещё не перенесённые
 * в базу транзакции. Файлы, изменившиеся за время копирования, — отказ:
 * значит, узел всё-таки пишет.
 */
internal object NodeDatabaseCopy {

    private val PARTS = listOf("", "-wal", "-journal")

    fun copy(database: File, into: File): File {
        val parts = PARTS.map { File(database.path + it) }
        val before = parts.map(::stamp)
        parts.filter(File::isFile).forEach { it.copyTo(File(into, it.name)) }
        if (parts.map(::stamp) != before) {
            throw NodeImportException("Node database $database changed while it was copied: stop the node")
        }
        return File(into, database.name)
    }

    private fun stamp(file: File): Pair<Long, Long>? = if (file.isFile) file.length() to file.lastModified() else null
}
