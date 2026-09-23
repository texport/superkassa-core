package io.github.texport.superkassa.importnode

import io.github.texport.superkassa.importnode.api.NodeImportException
import io.github.texport.superkassa.importnode.api.NodeImportResult
import io.github.texport.superkassa.importnode.api.importNodeData
import java.io.File
import java.io.RandomAccessFile
import java.net.ServerSocket
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Перенос, который нельзя сделать без потерь, не делается вовсе: каталог
 * кассы остаётся без базы и без отметки, и следующий запуск пробует заново.
 */
class NodeImportRefusalTest {
    private val root: File = createTempDirectory("node-import-").toFile()
    private val home = File(root, "node")
    private val target = File(root, "kassa")

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    @Test
    fun `без базы узла переносить нечего`() {
        home.mkdirs()
        assertEquals(NodeImportResult.NoNodeData, importNodeData(home.path, target.path))
        assertNothingImported()
    }

    @Test
    fun `отвечающий узел — отказ`() = ServerSocket(0).use { listening ->
        node()
        refused("answers") { importNodeData(home.path, target.path, "127.0.0.1:${listening.localPort}") }
    }

    @Test
    fun `процесс узла над этим рабочим местом — отказ`() {
        node()
        // Так приложение запускает узел: рабочее место — свойством в командной строке.
        // Ждёт встроенный `read`: внешнюю команду оболочка запускает exec'ом
        // вместо себя, и свойство пропадает из аргументов процесса.
        val process = ProcessBuilder("/bin/sh", "-c", "read line", "-Dsuperkassa.home=${home.path}").start()
        try {
            refused("Node process") { importNodeData(home.path, target.path) }
        } finally {
            process.destroyForcibly().waitFor()
        }
    }

    @Test
    fun `в каталоге уже своя касса — отказ`() {
        node()
        target.mkdirs()
        File(target, "superkassa.db").writeText("")
        refused("already holds") { importNodeData(home.path, target.path) }
    }

    @Test
    fun `каталог занят запущенной кассой — отказ`() {
        node()
        target.mkdirs()
        RandomAccessFile(File(target, "superkassa.lock"), "rw").use { file ->
            file.channel.lock().use { refused("used by a running") { importNodeData(home.path, target.path) } }
        }
    }

    @Test
    fun `база узла другой версии — отказ`() {
        node { it.exec("INSERT INTO schema_migrations VALUES ('26', 'v26', 1)") }
        refused("schema differs") { importNodeData(home.path, target.path) }
    }

    @Test
    fun `неразборный чек — отказ целиком`() {
        node { it.exec("UPDATE fiscal_document SET payload_bin = ? WHERE id = 'd-sale'", "{broken".toByteArray()) }
        refused("cannot be read") { importNodeData(home.path, target.path) }
    }

    @Test
    fun `неразборное оформление чека — отказ, а не сброс к умолчанию`() {
        node { it.exec("UPDATE cashbox SET branding_json = '{broken'") }
        refused("branding of cash register kkm-1") { importNodeData(home.path, target.path) }
    }

    @Test
    fun `оформление с незнакомым языком чека — отказ`() {
        node { it.exec("UPDATE cashbox SET branding_json = '{\"language\":\"EN\"}'") }
        refused("branding of cash register kkm-1") { importNodeData(home.path, target.path) }
    }

    @Test
    fun `записи в старой очереди узла — отказ`() {
        node { it.exec("INSERT INTO offline_queue VALUES ('o-1', 'kkm-1', 1, 'TICKET', 'd-sale', 1, 'PENDING', 0, NULL, NULL)") }
        refused("offline_queue") { importNodeData(home.path, target.path) }
    }

    @Test
    fun `задача очереди, которой нет в модели очереди кассы, — отказ`() {
        node { it.exec("UPDATE queue_task SET lane = 'ONLINE' WHERE id = 'q-out'") }
        refused("q-out") { importNodeData(home.path, target.path) }
    }

    @Test
    fun `после отказа перенос проходит, как только причина снята`() {
        node()
        ServerSocket(0).use { listening ->
            refused("answers") { importNodeData(home.path, target.path, "127.0.0.1:${listening.localPort}") }
        }
        assertTrue(importNodeData(home.path, target.path) is NodeImportResult.Imported)
    }

    private fun node(change: (NodeFixture) -> Unit = {}) {
        val node = NodeFixture(home)
        NodeScenario.populate(node)
        change(node)
        node.close()
    }

    private fun refused(reason: String, block: () -> Unit) {
        val error = assertFailsWith<NodeImportException> { block() }
        assertTrue(reason in error.message.orEmpty(), "unexpected refusal: ${error.message}")
        assertNothingImported()
    }

    private fun assertNothingImported() {
        val left = target.list().orEmpty().toSet() - setOf("superkassa.lock", "superkassa.db")
        assertEquals(emptySet(), left)
        assertTrue(!File(target, "node-import.done").exists())
    }
}
