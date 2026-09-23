package io.github.texport.superkassa.embedded.impl.storage

/**
 * Файлы каталога данных кассы.
 *
 * Запись идёт через временный файл и переименование: оборванная запись
 * не оставляет настройки наполовину пустыми.
 */
internal expect object LocalFiles {
    fun exists(path: String): Boolean

    fun readText(path: String): String?

    fun writeText(path: String, text: String)

    fun ensureDirectory(path: String)

    /**
     * Берёт замок владельца каталога.
     *
     * Замок держится, пока открыт возвращённый объект, и снимается и при
     * аварийном завершении процесса: его держит система, а не файл-метка.
     *
     * @throws IllegalStateException если каталог уже занят другой кассой.
     */
    fun lock(path: String): AutoCloseable
}

/** Имена файлов в каталоге данных кассы. */
internal object DataFiles {
    const val DATABASE = "superkassa.db"
    const val SETTINGS = "core-settings.json"
    const val LOCK = "superkassa.lock"
}
