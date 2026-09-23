package io.github.texport.superkassa.importnode.impl.node

import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import io.github.texport.superkassa.importnode.api.NodeImportException
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Рабочее место узла: настройки и база так, как их находит сам узел.
 *
 * Узел берёт путь к базе из `config/core-settings.json` и считает
 * относительный путь от рабочего места (`NodeHome.resolveJdbcUrl`).
 * Без файла настроек узел здесь ещё не запускался и базу ищет по умолчанию.
 */
internal class NodeWorkspace(home: File) {

    val home: File = home.absoluteFile.normalize()

    private val settingsFile = File(this.home, SETTINGS_FILE)

    /** Настройки узла; `null`, если файла нет. Неразборный файл — отказ. */
    fun settings(): CoreSettings? {
        if (!settingsFile.isFile) return null
        return try {
            json.decodeFromString(CoreSettings.serializer(), settingsFile.readText())
        } catch (e: IllegalArgumentException) {
            // Причина без исключения разбора: его текст несёт кусок файла, а там пароли доставки.
            throw NodeImportException("Node settings $settingsFile cannot be read: ${e::class.simpleName}")
        }
    }

    /** Файл базы узла. База на сервере PostgreSQL или MySQL в приложение не переносится. */
    fun database(settings: CoreSettings?): File {
        val url = settings?.storage?.jdbcUrl ?: DEFAULT_URL
        if (!url.lowercase().startsWith(PREFIX)) {
            throw NodeImportException(
                "Node storage ${settings?.storage?.engine} is not a SQLite file: only desktop nodes are imported"
            )
        }
        val path = url.substring(PREFIX.length).substringBefore('?')
        if (path.isEmpty() || path == MEMORY || path.startsWith(URI_FORM)) {
            throw NodeImportException("Node database address in $settingsFile does not name a file")
        }
        val file = File(path)
        return (if (file.isAbsolute) file else File(home, path)).normalize()
    }

    private companion object {
        const val SETTINGS_FILE = "config/core-settings.json"
        const val DEFAULT_URL = "jdbc:sqlite:data/core.db"
        const val PREFIX = "jdbc:sqlite:"
        const val MEMORY = ":memory:"
        const val URI_FORM = "file:"
        val json = Json { ignoreUnknownKeys = true }
    }
}
