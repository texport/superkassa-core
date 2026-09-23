package io.github.texport.superkassa.importnode.impl.node

import io.github.texport.superkassa.importnode.api.NodeImportException
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI
import kotlin.jvm.optionals.getOrNull

/**
 * Не работает ли узел над своей базой.
 *
 * Узел не оставляет о себе ни файла, ни замка: SQLite держит замок базы
 * только на время записи, и простаивающий узел неотличим от остановленного.
 * Поэтому узел ищется так, как его запускает приложение, — процесс
 * с `-Dsuperkassa.home=<рабочее место>`, — и по адресу, если приложение
 * его назвало: ответивший узел ещё пишет.
 */
internal class NodeLiveness(private val home: File, private val address: String?) {

    fun requireStopped() {
        runningNode()?.let { throw NodeImportException("Node process $it works on $home: stop it before the import") }
        if (address != null && answers(address)) {
            throw NodeImportException("Node answers at $address: stop it before the import")
        }
    }

    private fun runningNode(): Long? = ProcessHandle.allProcesses()
        .filter { it.pid() != ProcessHandle.current().pid() }
        .filter { process -> arguments(process).any(::namesHome) }
        .findFirst().getOrNull()?.pid()

    /** Аргументы процесса; где система их порознь не отдаёт (Windows) — из командной строки. */
    private fun arguments(process: ProcessHandle): List<String> {
        val info = process.info()
        return info.arguments().getOrNull()?.toList() ?: info.commandLine().getOrNull()?.split(' ').orEmpty()
    }

    private fun namesHome(argument: String): Boolean =
        argument.startsWith(HOME_ARGUMENT) &&
            File(argument.removePrefix(HOME_ARGUMENT)).absoluteFile.normalize() == home.absoluteFile.normalize()

    private fun answers(address: String): Boolean {
        val uri = URI(if ("://" in address) address else "tcp://$address")
        val port = uri.port.takeIf { it > 0 } ?: DEFAULT_PORT
        return try {
            Socket().use { it.connect(InetSocketAddress(uri.host, port), PROBE_TIMEOUT_MS) }
            true
        } catch (_: IOException) {
            false
        }
    }

    private companion object {
        const val HOME_ARGUMENT = "-Dsuperkassa.home="
        const val DEFAULT_PORT = 8080
        const val PROBE_TIMEOUT_MS = 300
    }
}
