package io.github.texport.superkassa.importnode.impl.target

import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import io.github.texport.superkassa.core.domain.api.model.settings.StorageSettings
import io.github.texport.superkassa.importnode.api.NodeImportException
import io.github.texport.superkassa.importnode.api.NodeImportReport
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE

/**
 * Черновик переноса в каталоге кассы и его фиксация.
 *
 * Перенос собирается в `node-import/` рядом с базой кассы и становится
 * настоящим только перестановкой готовых файлов. Готовность черновика
 * отмечает файл `ready` с отчётом; его перестановка в `node-import.done`
 * и есть отметка о переносе. Сорвалось до `ready` — черновик выбрасывается,
 * и следующий запуск переносит заново; после `ready` — доводится до конца.
 */
internal class Staging(private val dataDir: File) {

    private val dir = File(dataDir, "node-import")
    private val settingsDraft = File(dir, SETTINGS)
    private val ready = File(dir, "ready")
    private val marker = File(dataDir, "node-import.done")

    /** Файл базы кассы в черновике. */
    val database = File(dir, DATABASE)

    fun isDone(): Boolean = marker.isFile

    /** Куда снимается копия базы узла; уходит вместе с черновиком. */
    fun nodeCopyDir(): File = File(dir, "node").also { check(it.isDirectory || it.mkdirs()) { "Cannot create $it" } }

    /** Доводит фиксацию, прерванную после готовности черновика; `null` — такой не было. */
    fun finishInterrupted(): NodeImportReport? {
        if (!ready.isFile) return null
        val report = json.decodeFromString(NodeImportReport.serializer(), ready.readText())
        commit()
        return report
    }

    /** Касса в каталоге уже есть: перенос поверх смешал бы две истории одних номеров. */
    fun requireEmptyTarget() {
        val taken = listOf(DATABASE, SETTINGS).filter { File(dataDir, it).exists() }
        if (taken.isNotEmpty()) {
            throw NodeImportException(
                "Data directory $dataDir already holds $taken: the import goes only into an empty one"
            )
        }
    }

    fun open() {
        discard()
        check(dir.mkdirs()) { "Cannot create $dir" }
    }

    fun discard() {
        dir.deleteRecursively()
    }

    /** Настройки узла для кассы: база своя, в каталоге кассы, а не узла. */
    fun writeSettings(settings: CoreSettings) {
        val own = settings.copy(storage = StorageSettings(engine = "SQLITE", jdbcUrl = "jdbc:sqlite:$DATABASE"))
        settingsDraft.writeText(json.encodeToString(CoreSettings.serializer(), own))
    }

    /** Отмечает черновик готовым: с этого места перенос уже не отменяется, а доводится. */
    fun seal(report: NodeImportReport) {
        check(!File(dir, "$DATABASE-wal").exists()) { "Draft database was not checkpointed" }
        val draft = File(dir, "ready.tmp")
        draft.writeText(json.encodeToString(NodeImportReport.serializer(), report))
        Files.move(draft.toPath(), ready.toPath(), ATOMIC_MOVE)
    }

    fun commit() {
        move(database, File(dataDir, DATABASE))
        move(settingsDraft, File(dataDir, SETTINGS))
        Files.move(ready.toPath(), marker.toPath(), ATOMIC_MOVE)
        discard()
    }

    private fun move(from: File, to: File) {
        if (from.exists()) Files.move(from.toPath(), to.toPath(), ATOMIC_MOVE)
    }

    private companion object {
        const val DATABASE = "superkassa.db"
        const val SETTINGS = "core-settings.json"
        val json = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            encodeDefaults = true
        }
    }
}
