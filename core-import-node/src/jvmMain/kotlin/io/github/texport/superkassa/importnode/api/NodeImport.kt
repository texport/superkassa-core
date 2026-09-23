package io.github.texport.superkassa.importnode.api

import io.github.texport.superkassa.importnode.impl.NodeImporter
import io.github.texport.superkassa.importnode.impl.node.NodeLiveness
import io.github.texport.superkassa.importnode.impl.node.NodeWorkspace
import java.io.File

/**
 * Переносит данные узла в каталог данных кассы в процессе приложения.
 *
 * Приложение зовёт его при каждом запуске — до `createSuperkassa` и вместо
 * запуска узла. Первый вызов переносит кассы, смены, документы, очередь,
 * кассиров, ключи повтора и настройки; следующие отвечают [NodeImportResult.AlreadyImported]
 * и ничего не трогают. База узла только читается.
 *
 * Перенос целиком или никак: черновик собирается рядом, сверяется портом
 * хранилища кассы и только потом становится базой кассы. После переноса
 * узел больше не запускается: его база осталась прежней, и касса
 * на ней выдала бы документы с уже занятыми номерами.
 *
 * @param nodeHome рабочее место узла: там `config/core-settings.json` и база.
 * @param dataDir каталог данных кассы — тот же, что в `SuperkassaPlatform`.
 * @param nodeAddress адрес узла (`host:port` или URL), если приложение его знает:
 *   ответивший узел ещё пишет, и перенос не начинается.
 * @throws NodeImportException перенос не сделан; каталог кассы не тронут, кассу не поднимать.
 */
fun importNodeData(nodeHome: String, dataDir: String, nodeAddress: String? = null): NodeImportResult {
    val workspace = NodeWorkspace(File(nodeHome))
    return NodeImporter(workspace, File(dataDir).absoluteFile, NodeLiveness(workspace.home, nodeAddress)).run()
}
