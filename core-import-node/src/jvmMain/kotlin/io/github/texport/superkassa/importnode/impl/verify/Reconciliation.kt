package io.github.texport.superkassa.importnode.impl.verify

import androidx.room.Room
import io.github.texport.superkassa.coredatabase.api.openRoomStorage
import io.github.texport.superkassa.coredatabase.impl.db.SuperkassaAppDatabase
import io.github.texport.superkassa.importnode.api.KkmImportReport
import io.github.texport.superkassa.importnode.api.NodeImportException
import io.github.texport.superkassa.importnode.impl.node.NodeData
import java.io.File

/**
 * Сверка базы кассы после переноса с прочитанным у узла.
 *
 * База кассы читается её же портом хранилища — тем путём, которым её
 * будет читать касса, а не строками, которые перенос только что записал.
 * Любое расхождение фискального — отказ переноса.
 */
internal class Reconciliation(private val data: NodeData) {

    fun verify(database: File): List<KkmImportReport> {
        val storage = openRoomStorage(Room.databaseBuilder<SuperkassaAppDatabase>(name = database.path))
        try {
            val port = storage.storagePort
            same("cash registers", port.countKkms(null, null), data.snapshot.kkms.size)
            return data.snapshot.kkms.map { KkmCheck(it, data, port).verify() }
        } finally {
            storage.close()
        }
    }
}

/** Одинаковы ли записи; нет — отказ с именем записи, без её содержимого. */
internal fun <T> same(what: String, actual: T, expected: T) {
    if (actual != expected) throw NodeImportException("Reconciliation failed: $what differ after the import")
}
