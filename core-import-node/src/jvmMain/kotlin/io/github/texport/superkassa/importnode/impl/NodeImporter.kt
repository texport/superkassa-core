package io.github.texport.superkassa.importnode.impl

import androidx.room.Room
import androidx.sqlite.SQLiteException
import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import io.github.texport.superkassa.coredatabase.api.restoreRoomStorage
import io.github.texport.superkassa.coredatabase.impl.db.SuperkassaAppDatabase
import io.github.texport.superkassa.importnode.api.NodeImportException
import io.github.texport.superkassa.importnode.api.NodeImportReport
import io.github.texport.superkassa.importnode.api.NodeImportResult
import io.github.texport.superkassa.importnode.impl.node.NodeDatabase
import io.github.texport.superkassa.importnode.impl.node.NodeDatabaseCopy
import io.github.texport.superkassa.importnode.impl.node.NodeLiveness
import io.github.texport.superkassa.importnode.impl.node.NodeSnapshotReader
import io.github.texport.superkassa.importnode.impl.node.NodeWorkspace
import io.github.texport.superkassa.importnode.impl.target.DataDirLock
import io.github.texport.superkassa.importnode.impl.target.Staging
import io.github.texport.superkassa.importnode.impl.verify.Reconciliation
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException

/**
 * Перенос по шагам.
 *
 * Правила, по которым перенос не начинается: каталог кассы занят запущенной
 * кассой; перенос уже сделан; у узла нет базы; в каталоге кассы уже своя база;
 * узел работает. Затем база узла читается целиком, кладётся в черновик,
 * черновик сверяется и только после этого занимает место базы кассы.
 */
internal class NodeImporter(
    private val workspace: NodeWorkspace,
    private val dataDir: File,
    private val liveness: NodeLiveness
) {
    private val logger = LoggerFactory.getLogger(NodeImporter::class.java)

    fun run(): NodeImportResult {
        check(dataDir.isDirectory || dataDir.mkdirs()) { "Cannot create data directory $dataDir" }
        return DataDirLock.acquire(dataDir).use { runLocked(Staging(dataDir)) }
    }

    private fun runLocked(staging: Staging): NodeImportResult {
        if (staging.isDone()) return NodeImportResult.AlreadyImported.also { staging.discard() }
        staging.finishInterrupted()?.let { return NodeImportResult.Imported(it) }
        val settings = workspace.settings()
        val database = workspace.database(settings)
        if (!database.isFile) return NodeImportResult.NoNodeData
        staging.requireEmptyTarget()
        liveness.requireStopped()
        logger.info("Importing node database {} into {}", database, dataDir)
        val report = stageOrDiscard(staging, database, settings)
        staging.commit()
        logger.info("Node data imported: {} cash registers", report.kkms.size)
        return NodeImportResult.Imported(report)
    }

    private fun stageOrDiscard(staging: Staging, database: File, settings: CoreSettings?): NodeImportReport {
        staging.open()
        var sealed = false
        try {
            return translated { stage(staging, database, settings) }.also {
                staging.seal(it)
                sealed = true
            }
        } finally {
            if (!sealed) staging.discard()
        }
    }

    /** Отказ любой природы — отказ переноса: вызывающему незачем знать SQLite и Room. */
    private inline fun <T> translated(block: () -> T): T = try {
        block()
    } catch (e: NodeImportException) {
        throw e
    } catch (e: IllegalArgumentException) {
        throw NodeImportException("Node data does not fit the cash register database: ${e.message}", e)
    } catch (e: IllegalStateException) {
        throw NodeImportException("Import stopped: ${e.message}", e)
    } catch (e: SQLiteException) {
        throw NodeImportException("Node or draft database cannot be used: ${e.message}", e)
    } catch (e: IOException) {
        throw NodeImportException("Import files cannot be written: ${e.message}", e)
    }

    private fun stage(staging: Staging, database: File, settings: CoreSettings?): NodeImportReport {
        val copy = NodeDatabaseCopy.copy(database, staging.nodeCopyDir())
        val data = NodeDatabase.open(copy).use(NodeSnapshotReader::read)
        restoreRoomStorage(Room.databaseBuilder<SuperkassaAppDatabase>(name = staging.database.path), data.snapshot)
        val kkms = Reconciliation(data).verify(staging.database)
        settings?.let(staging::writeSettings)
        return NodeImportReport(kkms, data.notTransferred, settingsTransferred = settings != null)
    }
}
