package io.github.texport.superkassa.embedded.api

import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.texport.superkassa.core.domain.api.port.integration.DocumentConvertPort
import io.github.texport.superkassa.coredatabase.impl.db.SuperkassaAppDatabase
import io.github.texport.superkassa.embedded.impl.document.HtmlDocuments
import io.github.texport.superkassa.embedded.impl.print.SystemPrinter
import java.io.File

/**
 * Платформа настольного приложения: Windows, Linux, macOS.
 *
 * Документы рисуются без браузера, печать идёт на принтеры системы.
 *
 * @param dataDir каталог данных кассы; каталог приложения выбирает само
 *   приложение, касса своего умолчания не навязывает.
 */
actual class SuperkassaPlatform(dataDir: String) {
    internal actual val dataDir: String = File(dataDir).absolutePath

    internal actual fun databaseBuilder(path: String): RoomDatabase.Builder<SuperkassaAppDatabase> =
        Room.databaseBuilder<SuperkassaAppDatabase>(name = path)

    internal actual fun documents(): DocumentConvertPort = HtmlDocuments()

    internal actual fun printer(): DocumentPrinter = SystemPrinter()
}
