package io.github.texport.superkassa.importnode

import io.github.texport.superkassa.importnode.api.NodeImportResult
import io.github.texport.superkassa.importnode.api.importNodeData
import java.io.File
import kotlin.test.Test
import kotlin.test.assertIs

/**
 * Перенос копии настоящего рабочего места узла.
 *
 * Запускается только по переменной `NODE_IMPORT_COPY` — каталогу с копией
 * рабочего места; `NODE_IMPORT_TARGET` — пустой каталог кассы. Оригинал
 * рабочего места сюда не подставлять: перенос его не меняет, но проверка
 * должна идти на копии. Печатает отчёт переноса — числа, без токенов и пинов.
 */
class LiveCopyImportTest {

    @Test
    fun `копия рабочего места узла переносится и сверяется`() {
        val copy = System.getenv("NODE_IMPORT_COPY") ?: return
        val target = System.getenv("NODE_IMPORT_TARGET") ?: return
        val result = importNodeData(copy, target)
        assertIs<NodeImportResult.Imported>(result)
        println("NODE_IMPORT_REPORT ${result.report}")
        println("NODE_IMPORT_REPEAT ${importNodeData(copy, target)}")
        println("NODE_IMPORT_TARGET_FILES ${File(target).list()?.sorted()}")
    }
}
