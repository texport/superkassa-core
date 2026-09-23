package io.github.texport.superkassa.importnode

import io.github.texport.superkassa.importnode.api.NodeImportResult
import io.github.texport.superkassa.importnode.api.importNodeData
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Процесс оборвался после того, как черновик сверен и отмечен готовым,
 * но до того, как он встал на место базы кассы. Следующий запуск доводит
 * фиксацию, а не начинает перенос заново и не отказывает.
 */
class InterruptedCommitTest {
    private val root: File = createTempDirectory("node-import-").toFile()

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    @Test
    fun `готовый черновик доводится до конца`() {
        val node = NodeFixture(File(root, "node")).also(NodeScenario::populate).also(NodeFixture::close)
        val finished = File(root, "finished")
        val report = assertIs<NodeImportResult.Imported>(importNodeData(node.home.path, finished.path)).report
        val interrupted = File(root, "interrupted")
        val draft = File(interrupted, "node-import").also(File::mkdirs)
        File(finished, "superkassa.db").copyTo(File(draft, "superkassa.db"))
        File(finished, "node-import.done").copyTo(File(draft, "ready"))

        val resumed = assertIs<NodeImportResult.Imported>(importNodeData(node.home.path, interrupted.path))

        assertEquals(report, resumed.report)
        assertTrue(File(interrupted, "superkassa.db").isFile)
        assertTrue(File(interrupted, "node-import.done").isFile)
        assertTrue(!draft.exists())
    }
}
