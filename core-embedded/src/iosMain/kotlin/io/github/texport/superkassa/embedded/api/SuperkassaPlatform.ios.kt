package io.github.texport.superkassa.embedded.api

import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.texport.superkassa.core.domain.api.port.integration.DocumentConvertPort
import io.github.texport.superkassa.coredatabase.impl.db.SuperkassaAppDatabase
import io.github.texport.superkassa.embedded.impl.print.UnsupportedDocuments
import io.github.texport.superkassa.embedded.impl.print.UnsupportedPrinter

/**
 * Платформа iOS: база и очередь работают, документы и печать ещё нет.
 *
 * Рисование документов и печать отказывают с причиной, а не отдают
 * пустышку: на iOS для них нужен свой движок (WKWebView или PDFKit).
 *
 * @param dataDir абсолютный путь к каталогу данных кассы.
 */
actual class SuperkassaPlatform(dataDir: String) {
    internal actual val dataDir: String = dataDir

    internal actual fun databaseBuilder(path: String): RoomDatabase.Builder<SuperkassaAppDatabase> =
        Room.databaseBuilder<SuperkassaAppDatabase>(name = path)

    internal actual fun documents(): DocumentConvertPort = UnsupportedDocuments("iOS")

    internal actual fun printer(): DocumentPrinter = UnsupportedPrinter("iOS")
}
